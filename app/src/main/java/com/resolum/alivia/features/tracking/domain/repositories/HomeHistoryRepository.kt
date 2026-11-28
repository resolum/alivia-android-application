package com.resolum.alivia.features.tracking.domain.repositories

import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.tracking.domain.models.HistoryDevice
import com.resolum.alivia.features.tracking.domain.models.HistoryEvent
import com.resolum.alivia.features.tracking.domain.models.LiveHistoryMessage
import kotlinx.coroutines.flow.Flow

interface HomeHistoryRepository {
    /** The latest events of the home of the person, newest first. */
    suspend fun recent(personId: Long, limit: Int = 100): NetworkResult<List<HistoryEvent>>

    /** The devices of the home, to name the events. */
    suspend fun devices(personId: Long): NetworkResult<List<HistoryDevice>>

    /** Each new event of the home, pushed by the backend over a WebSocket; the flow ends when the channel closes. */
    fun live(personId: Long): Flow<LiveHistoryMessage>
}
