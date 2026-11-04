package com.resolum.alivia.features.iam.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resolum.alivia.features.iam.domain.models.Session
import com.resolum.alivia.features.iam.domain.usecases.SignInUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
)

class LoginViewModel(private val signIn: SignInUseCase) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun updateEmail(value: String) = _state.update { it.copy(email = value, error = null) }
    fun updatePassword(value: String) = _state.update { it.copy(password = value, error = null) }
    fun togglePassword() = _state.update { it.copy(showPassword = !it.showPassword) }

    fun submit(onSignedIn: (Session) -> Unit) {
        val current = _state.value
        if (current.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = signIn(current.email, current.password)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(loading = false, password = "") }
                    onSignedIn(result.data)
                }
                is NetworkResult.Error -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
