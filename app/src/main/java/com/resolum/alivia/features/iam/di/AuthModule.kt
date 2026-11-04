package com.resolum.alivia.features.iam.di

import com.resolum.alivia.features.iam.data.datasources.remote.AuthFacadeService
import com.resolum.alivia.features.iam.data.datasources.remote.services.AuthService
import com.resolum.alivia.features.iam.data.repositories.AuthRepositoryImpl
import com.resolum.alivia.features.iam.domain.repositories.AuthRepository
import com.resolum.alivia.features.iam.domain.usecases.RestoreSessionUseCase
import com.resolum.alivia.features.iam.domain.usecases.SignInUseCase
import com.resolum.alivia.features.iam.domain.usecases.SignOutUseCase
import com.resolum.alivia.features.shared.di.SharedModule

object AuthModule {
    private val repository: AuthRepository by lazy {
        AuthRepositoryImpl(
            AuthFacadeService(SharedModule.retrofit.create(AuthService::class.java)),
            SharedModule.tokenDataStore,
        )
    }

    fun signInUseCase() = SignInUseCase(repository)
    fun restoreSessionUseCase() = RestoreSessionUseCase(repository)
    fun signOutUseCase() = SignOutUseCase(repository)
}
