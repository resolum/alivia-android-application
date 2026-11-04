package com.resolum.alivia.features.shared.di

import android.content.Context
import com.resolum.alivia.features.shared.data.datasources.datastore.TokenDataStore
import com.resolum.alivia.features.shared.data.datasources.remote.ApiClientFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import retrofit2.Retrofit

/** Objects shared by every feature: the stored session and the backend client. */
object SharedModule {
    private lateinit var appContext: Context

    private val expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when the session could not be renewed, so the app goes back to the sign-in screen. */
    val sessionExpired: SharedFlow<Unit> = expired.asSharedFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val tokenDataStore: TokenDataStore by lazy { TokenDataStore(appContext) }

    val retrofit: Retrofit by lazy {
        ApiClientFactory.create(tokenDataStore, onSessionExpired = { expired.tryEmit(Unit) })
    }
}
