package com.resolum.alivia.features.tracking.data.datasources.remote

import com.google.gson.Gson
import com.resolum.alivia.features.shared.data.datasources.datastore.SessionTokenStore
import com.resolum.alivia.features.tracking.data.datasources.remote.services.LiveMessageDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.IOException
import java.util.concurrent.TimeUnit

/** The WebSocket `/ws/v1/assisted-persons/{personId}/events`, authenticated with the session token like any call. */
class HomeEventsSocket(
    private val baseUrl: String,
    private val tokens: SessionTokenStore,
    private val client: OkHttpClient = OkHttpClient.Builder().pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS).build(),
    private val gson: Gson = Gson(),
) {
    fun messages(personId: Long): Flow<LiveMessageDto> = callbackFlow {
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/ws/v1/assisted-persons/$personId/events")
            .apply { tokens.accessToken?.let { header("Authorization", "Bearer $it") } }
            .build()
        val socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text == "pong") return
                runCatching { gson.fromJson(text, LiveMessageDto::class.java) }.getOrNull()?.let { trySend(it) }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, null)
                close()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                close()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                close(IOException("live history closed: ${response?.code ?: t.javaClass.simpleName}", t))
            }
        })
        awaitClose { socket.close(1000, null) }
    }
}
