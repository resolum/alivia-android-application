package com.resolum.alivia.features.iam.di

import com.resolum.alivia.features.iam.data.datasources.remote.AuthFacadeService
import com.resolum.alivia.features.iam.data.datasources.remote.services.AuthService
import com.resolum.alivia.features.iam.data.datasources.remote.services.PasswordResetService
import com.resolum.alivia.features.iam.data.repositories.AuthRepositoryImpl
import com.resolum.alivia.features.iam.data.repositories.PasswordResetRepositoryImpl
import com.resolum.alivia.features.iam.domain.repositories.AuthRepository
import com.resolum.alivia.features.iam.domain.repositories.PasswordResetRepository
import com.resolum.alivia.features.iam.domain.usecases.ResetPasswordUseCase
import com.resolum.alivia.features.iam.domain.usecases.RestoreSessionUseCase
import com.resolum.alivia.features.iam.domain.usecases.SendPasswordResetCodeUseCase
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

    private val passwordResetRepository: PasswordResetRepository by lazy {
        PasswordResetRepositoryImpl(SharedModule.retrofit.create(PasswordResetService::class.java))
    }

    fun sendPasswordResetCode() = SendPasswordResetCodeUseCase(passwordResetRepository)
    fun resetPassword() = ResetPasswordUseCase(passwordResetRepository)
    fun signInUseCase() = SignInUseCase(repository)
    fun restoreSessionUseCase() = RestoreSessionUseCase(repository)
    fun signOutUseCase() = SignOutUseCase(repository)
}
