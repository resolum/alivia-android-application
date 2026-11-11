package com.resolum.alivia.features.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resolum.alivia.core.ui.format.Formats
import com.resolum.alivia.features.notifications.domain.AppNotification
import com.resolum.alivia.features.notifications.domain.GetNotificationsUseCase
import com.resolum.alivia.features.notifications.domain.MarkNotificationsReadUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class NotificationFilter(val label: String) { ALL("Todas"), UNREAD("No leídas"), TODAY("Hoy") }

data class NotificationsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val notifications: List<AppNotification> = emptyList(),
    val filter: NotificationFilter = NotificationFilter.ALL,
    val today: LocalDate = Formats.today(),
) {
    fun count(filter: NotificationFilter) = notifications.count { matches(it, filter) }

    val visible: List<AppNotification> get() = notifications.filter { matches(it, filter) }

    private fun matches(item: AppNotification, filter: NotificationFilter) = when (filter) {
        NotificationFilter.ALL -> true
        NotificationFilter.UNREAD -> !item.read
        NotificationFilter.TODAY -> item.createdAt?.toLocalDate() == today
    }
}

class NotificationsViewModel(
    private val getNotifications: GetNotificationsUseCase,
    private val markRead: MarkNotificationsReadUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    fun load(refresh: Boolean = false) {
        _state.update { it.copy(loading = it.notifications.isEmpty() && !refresh, refreshing = refresh, error = null, today = Formats.today()) }
        viewModelScope.launch {
            when (val result = getNotifications()) {
                is NetworkResult.Success -> _state.update { it.copy(loading = false, refreshing = false, notifications = result.data) }
                is NetworkResult.Error -> _state.update { it.copy(loading = false, refreshing = false, error = result.message) }
            }
        }
    }

    fun select(filter: NotificationFilter) = _state.update { it.copy(filter = filter) }

    /** Marks as read right away and tells the backend; a failure keeps them read on screen until the next load. */
    fun markAllRead() = mark(_state.value.notifications.filter { !it.read }.map { it.id })

    fun open(notification: AppNotification) {
        if (!notification.read) mark(listOf(notification.id))
    }

    private fun mark(ids: List<Long>) {
        if (ids.isEmpty()) return
        _state.update { state -> state.copy(notifications = state.notifications.map { if (it.id in ids) it.copy(read = true) else it }) }
        viewModelScope.launch { markRead(ids) }
    }
}
