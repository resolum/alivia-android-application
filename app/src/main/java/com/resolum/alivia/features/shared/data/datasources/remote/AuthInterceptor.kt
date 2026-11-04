package com.resolum.alivia.features.shared.data.datasources.remote

import com.resolum.alivia.features.shared.data.datasources.datastore.TokenDataStore
import okhttp3.Interceptor
import okhttp3.Response

/** Adds the access token to every call except the sign-in and refresh ones. */
class AuthInterceptor(private val tokens: TokenDataStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokens.accessToken
        if (token == null || request.url.encodedPath.contains("/api/v1/auth/")) return chain.proceed(request)
        return chain.proceed(request.newBuilder().header("Authorization", "Bearer $token").build())
    }
}
