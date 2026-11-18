package com.resolum.alivia.features.notifications.push

import com.google.firebase.messaging.FirebaseMessaging
import com.resolum.alivia.features.notifications.domain.PushTokenProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Reads the Firebase Cloud Messaging token of this installation. */
class FirebasePushTokenProvider : PushTokenProvider {
    override suspend fun currentToken(): String? = runCatching {
        suspendCancellableCoroutine { continuation ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                continuation.resume(if (task.isSuccessful) task.result else null)
            }
        }
    }.getOrNull()
}
