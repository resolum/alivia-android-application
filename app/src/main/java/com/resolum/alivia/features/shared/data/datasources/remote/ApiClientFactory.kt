package com.resolum.alivia.features.shared.data.datasources.remote

import com.resolum.alivia.BuildConfig
import com.resolum.alivia.features.shared.data.datasources.datastore.SessionTokenStore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Builds the Retrofit client of the Alivia backend, with the session token and its renewal. */
object ApiClientFactory {
    fun create(tokens: SessionTokenStore, onSessionExpired: () -> Unit, baseUrl: String = BuildConfig.API_BASE_URL): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokens))
            .authenticator(TokenRefreshAuthenticator(baseUrl, tokens, onSessionExpired))
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
