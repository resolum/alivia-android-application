package com.resolum.alivia.features.iam.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resolum.alivia.features.iam.domain.usecases.ResetPasswordUseCase
import com.resolum.alivia.features.iam.domain.usecases.SendPasswordResetCodeUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RecoveryStep { EMAIL, CODE, PASSWORD, DONE }

data class PasswordRecoveryUiState(
    val step: RecoveryStep = RecoveryStep.EMAIL,
    val email: String = "",
    val code: String = "",
    val password: String = "",
    val confirmation: String = "",
    val showPassword: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val resendSeconds: Int = 0,
) {
    /** "m***a@gmail.com", as the design shows where the code went. */
    val maskedEmail: String
        get() {
            val at = email.indexOf('@')
            if (at <= 1) return email
            return "${email.first()}***${email[at - 1]}${email.substring(at)}"
        }
}

/** Forgot-password flow: email, 6-digit code, new password and confirmation. */
class PasswordRecoveryViewModel(
    private val sendCode: SendPasswordResetCodeUseCase,
    private val resetPassword: ResetPasswordUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(PasswordRecoveryUiState())
    val state: StateFlow<PasswordRecoveryUiState> = _state.asStateFlow()

    private var countdown: Job? = null

    fun onEmail(value: String) = _state.update { it.copy(email = value.trim(), error = null) }
    fun onCode(value: String) = _state.update { it.copy(code = value.filter(Char::isDigit).take(6), error = null) }
    fun onPassword(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onConfirmation(value: String) = _state.update { it.copy(confirmation = value, error = null) }
    fun togglePassword() = _state.update { it.copy(showPassword = !it.showPassword) }
    fun dismissNotice() = _state.update { it.copy(notice = null) }

    fun submitEmail() = send(nextStep = RecoveryStep.CODE)

    fun resend() {
        if (_state.value.resendSeconds == 0) send(nextStep = RecoveryStep.CODE)
    }

    private fun send(nextStep: RecoveryStep) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val result = sendCode(_state.value.email)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(busy = false, step = nextStep, code = "", notice = "Código enviado a tu correo") }
                    startCountdown()
                }
                is NetworkResult.Error -> _state.update { it.copy(busy = false, error = result.message) }
            }
        }
    }

    private fun startCountdown() {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            for (seconds in RESEND_SECONDS downTo 0) {
                _state.update { it.copy(resendSeconds = seconds) }
                if (seconds > 0) delay(1_000)
            }
        }
    }

    /** The backend checks the code together with the new password, so this step only collects it. */
    fun submitCode() {
        if (_state.value.code.length == 6) _state.update { it.copy(step = RecoveryStep.PASSWORD, error = null) }
        else _state.update { it.copy(error = "Ingresa los 6 dígitos del código.") }
    }

    fun submitPassword() {
        val current = _state.value
        if (current.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val result = resetPassword(current.email, current.code, current.password, current.confirmation)) {
                is NetworkResult.Success -> _state.update { it.copy(busy = false, step = RecoveryStep.DONE, password = "", confirmation = "") }
                // A wrong code sends the user back to type it again.
                is NetworkResult.Error -> _state.update {
                    if (result.code == 400) it.copy(busy = false, step = RecoveryStep.CODE, code = "", error = result.message)
                    else it.copy(busy = false, error = result.message)
                }
            }
        }
    }

    /** Back arrow: one step back, or false when the flow should close. */
    fun back(): Boolean {
        val previous = when (_state.value.step) {
            RecoveryStep.EMAIL, RecoveryStep.DONE -> return false
            RecoveryStep.CODE -> RecoveryStep.EMAIL
            RecoveryStep.PASSWORD -> RecoveryStep.CODE
        }
        _state.update { it.copy(step = previous, error = null) }
        return true
    }

    private companion object {
        const val RESEND_SECONDS = 60
    }
}
