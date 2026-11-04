package com.resolum.alivia.features.iam.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resolum.alivia.core.ui.components.AliviaLogo
import com.resolum.alivia.core.ui.components.ErrorBanner
import com.resolum.alivia.core.ui.components.PrimaryButton
import com.resolum.alivia.core.ui.theme.Background
import com.resolum.alivia.core.ui.theme.Ink
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.Outline
import com.resolum.alivia.core.ui.theme.Surface
import com.resolum.alivia.features.iam.di.AuthModule
import com.resolum.alivia.features.iam.domain.models.Session

/** Sign-in for technicians and caregivers; other roles are told to use the web. */
@Composable
fun LoginScreen(
    onSignedIn: (Session) -> Unit,
    viewModel: LoginViewModel = viewModel { LoginViewModel(AuthModule.signInUseCase()) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = Surface,
        focusedContainerColor = Surface,
        unfocusedBorderColor = Outline,
        focusedBorderColor = Navy,
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AliviaLogo(markSize = 26.dp, textSize = MaterialTheme.typography.titleLarge.fontSize)
        Spacer(Modifier.height(28.dp))
        Text("Bienvenido a Alivia", style = MaterialTheme.typography.headlineSmall, color = Navy)
        Text("Ingresa para continuar", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
        Spacer(Modifier.height(20.dp))

        Text("Correo electrónico", style = MaterialTheme.typography.labelMedium, color = Ink)
        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::updateEmail,
            placeholder = { Text("tecnico@alivia.pe") },
            trailingIcon = { Icon(Icons.Outlined.MailOutline, contentDescription = null, tint = InkMuted) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Ingresa el correo institucional o de cuidador", style = MaterialTheme.typography.bodySmall, color = InkMuted)
        Spacer(Modifier.height(12.dp))

        Text("Contraseña", style = MaterialTheme.typography.labelMedium, color = Ink)
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::updatePassword,
            singleLine = true,
            visualTransformation = if (state.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = viewModel::togglePassword) {
                    Icon(
                        if (state.showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (state.showPassword) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = InkMuted,
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.submit(onSignedIn) }),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        state.error?.let { ErrorBanner(it) }
        Spacer(Modifier.height(8.dp))
        PrimaryButton(
            "Ingresar",
            onClick = { viewModel.submit(onSignedIn) },
            enabled = state.email.isNotBlank() && state.password.isNotEmpty(),
            busy = state.loading,
            container = Navy,
            modifier = Modifier,
        )
    }
}
