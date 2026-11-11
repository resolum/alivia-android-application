package com.resolum.alivia.features.iam.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resolum.alivia.core.ui.components.ErrorBanner
import com.resolum.alivia.core.ui.components.PrimaryButton
import com.resolum.alivia.core.ui.theme.Background
import com.resolum.alivia.core.ui.theme.Ink
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.Outline
import com.resolum.alivia.core.ui.theme.Surface
import com.resolum.alivia.core.ui.theme.TealDark
import com.resolum.alivia.core.ui.theme.TealSoft
import com.resolum.alivia.features.iam.di.AuthModule
import com.resolum.alivia.features.iam.domain.usecases.PasswordRules

/** Forgot-password flow from the sign-in screen: email, emailed code, new password and confirmation. */
@Composable
fun PasswordRecoveryScreen(
    onClose: () -> Unit,
    viewModel: PasswordRecoveryViewModel = viewModel {
        PasswordRecoveryViewModel(AuthModule.sendPasswordResetCode(), AuthModule.resetPassword())
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val goBack = { if (!viewModel.back()) onClose() }
    BackHandler(onBack = goBack)

    LaunchedEffect(state.notice) {
        val notice = state.notice ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(notice, actionLabel = "CERRAR", duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) viewModel.dismissNotice()
    }

    Box(Modifier.fillMaxSize().background(Background)) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.step != RecoveryStep.DONE) {
                IconButton(onClick = goBack, modifier = Modifier.padding(start = 0.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = Navy)
                }
            }
            when (state.step) {
                RecoveryStep.EMAIL -> EmailStep(state, viewModel, onClose)
                RecoveryStep.CODE -> CodeStep(state, viewModel)
                RecoveryStep.PASSWORD -> PasswordStep(state, viewModel)
                RecoveryStep.DONE -> DoneStep(onClose)
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(16.dp))
    }
}

@Composable
private fun Title(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, color = Navy)
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = Surface, focusedContainerColor = Surface, unfocusedBorderColor = Outline, focusedBorderColor = Navy,
)

@Composable
private fun EmailStep(state: PasswordRecoveryUiState, viewModel: PasswordRecoveryViewModel, onClose: () -> Unit) {
    Title("Recuperar contraseña", "Ingresa el correo asociado a tu cuenta para recibir un código de verificación de 6 dígitos.")
    Text("Correo electrónico", style = MaterialTheme.typography.labelMedium, color = Ink)
    OutlinedTextField(
        value = state.email,
        onValueChange = viewModel::onEmail,
        placeholder = { Text("correo@ejemplo.com") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        shape = RoundedCornerShape(12.dp),
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
    state.error?.let { ErrorBanner(it, Modifier.padding(top = 8.dp)) }
    Spacer(Modifier.height(16.dp))
    PrimaryButton("Enviar código", onClick = viewModel::submitEmail, enabled = state.email.isNotBlank(), busy = state.busy, container = Navy)
    TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
        Text("Volver al inicio de sesión", style = MaterialTheme.typography.labelLarge, color = Navy)
    }
}

@Composable
private fun CodeStep(state: PasswordRecoveryUiState, viewModel: PasswordRecoveryViewModel) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Title("Verifica tu correo", "Hemos enviado un código a ${state.maskedEmail}")
    Text("Código de 6 dígitos", style = MaterialTheme.typography.labelMedium, color = Ink)
    BasicTextField(
        value = state.code,
        onValueChange = viewModel::onCode,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.focusRequester(focus),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(6) { index ->
                    val digit = state.code.getOrNull(index)?.toString().orEmpty()
                    val active = index == state.code.length
                    Box(
                        Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(10.dp)).background(Surface)
                            .border(if (active) 2.dp else 1.dp, if (active) Navy else Outline, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text(digit, style = MaterialTheme.typography.titleLarge, color = Navy) }
                }
            }
        },
    )
    Text("Revisa también la carpeta de spam.", style = MaterialTheme.typography.bodySmall, color = InkMuted)
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (state.resendSeconds > 0) "Reenviar código en: 0:%02d".format(state.resendSeconds) else "¿No te llegó?",
            style = MaterialTheme.typography.bodySmall, color = Ink, modifier = Modifier.weight(1f),
        )
        TextButton(onClick = viewModel::resend, enabled = state.resendSeconds == 0 && !state.busy) { Text("Reenviar código") }
    }
    state.error?.let { ErrorBanner(it) }
    Spacer(Modifier.height(8.dp))
    PrimaryButton("Verificar", onClick = viewModel::submitCode, enabled = state.code.length == 6, container = Navy)
}

@Composable
private fun PasswordStep(state: PasswordRecoveryUiState, viewModel: PasswordRecoveryViewModel) {
    Title("Nueva contraseña", "Crea una contraseña segura para tu cuenta")
    PasswordField("Nueva contraseña", state.password, viewModel::onPassword, state.showPassword, viewModel::togglePassword)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(TealSoft.copy(alpha = 0.45f)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("REQUISITOS DE SEGURIDAD", style = MaterialTheme.typography.labelSmall, color = Navy)
        PasswordRules.checks(state.password).forEach { check ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(if (check.passed) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, contentDescription = null,
                    tint = if (check.passed) TealDark else InkMuted, modifier = Modifier.size(16.dp))
                Text(check.label, style = MaterialTheme.typography.bodySmall, color = if (check.passed) TealDark else InkMuted)
            }
        }
    }
    PasswordField("Confirmar contraseña", state.confirmation, viewModel::onConfirmation, state.showPassword, viewModel::togglePassword)
    if (state.confirmation.isNotEmpty() && state.confirmation != state.password) {
        Text("Las contraseñas no coinciden.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    state.error?.let { ErrorBanner(it) }
    Spacer(Modifier.height(8.dp))
    PrimaryButton(
        "Cambiar contraseña",
        onClick = viewModel::submitPassword,
        enabled = PasswordRules.isValid(state.password) && state.password == state.confirmation,
        busy = state.busy,
        container = Navy,
    )
}

@Composable
private fun PasswordField(label: String, value: String, onChange: (String) -> Unit, visible: Boolean, onToggle: () -> Unit) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = Ink)
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggle) {
                Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña", tint = InkMuted)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = RoundedCornerShape(12.dp),
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DoneStep(onClose: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 160.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(TealSoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = TealDark, modifier = Modifier.size(30.dp))
        }
        Text("Contraseña actualizada", style = MaterialTheme.typography.headlineSmall, color = Navy)
        Text("Tu contraseña ha sido restablecida exitosamente. Ya puedes ingresar con tus nuevas credenciales.",
            style = MaterialTheme.typography.bodyMedium, color = InkMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Volver a iniciar sesión", onClick = onClose, container = Navy)
    }
}
