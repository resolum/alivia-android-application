package com.resolum.alivia.features.profiles.presentation

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resolum.alivia.core.ui.components.AliviaCard
import com.resolum.alivia.core.ui.components.ErrorBanner
import com.resolum.alivia.core.ui.components.Loading
import com.resolum.alivia.core.ui.components.Pill
import com.resolum.alivia.core.ui.components.PrimaryButton
import com.resolum.alivia.core.ui.components.SectionLabel
import com.resolum.alivia.core.ui.theme.Background
import com.resolum.alivia.core.ui.theme.Ink
import com.resolum.alivia.core.ui.theme.InkMuted
import com.resolum.alivia.core.ui.theme.Navy
import com.resolum.alivia.core.ui.theme.NavyButton
import com.resolum.alivia.core.ui.theme.NavySoft
import com.resolum.alivia.core.ui.theme.Outline
import com.resolum.alivia.core.ui.theme.Surface
import com.resolum.alivia.core.ui.theme.TealDark
import com.resolum.alivia.core.ui.theme.TealSoft
import com.resolum.alivia.features.iam.domain.models.Session
import com.resolum.alivia.features.iam.domain.models.UserRole
import com.resolum.alivia.features.operations.presentation.orders.ScreenHeader
import com.resolum.alivia.features.profiles.di.ProfileModule
import com.resolum.alivia.features.profiles.domain.models.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

/** The signed-in account's personal information, its photo and the sign-out. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    session: Session,
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = viewModel { ProfileViewModel(ProfileModule.getProfile(), ProfileModule.updateProfile()) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            }
            if (bytes != null) viewModel.uploadPhoto(bytes) else viewModel.photoPickFailed()
        }
    }

    LaunchedEffect(viewModel) { viewModel.load() }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "Perfil", subtitle = session.email, onOpenProfile = {})
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { viewModel.load(refresh = true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val profile = state.profile
                when {
                    state.loading -> Loading()
                    profile != null -> {
                        Header(profile, if (session.role == UserRole.TECHNICIAN) "Técnico" else "Cuidador", state.uploadingPhoto) {
                            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        state.photoError?.let { ErrorBanner(it) }
                        PersonalInformation(profile, onEdit = viewModel::startEditing)
                    }
                    else -> Column(Modifier.fillMaxWidth()) {
                        ErrorBanner(state.error ?: "No pudimos cargar tu perfil.")
                        TextButton(onClick = { viewModel.load() }) { Text("Reintentar", color = NavyButton) }
                    }
                }
                OutlinedButton(onClick = onSignOut, border = BorderStroke(1.dp, Outline), modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = NavyButton)
                    Text("  Cerrar sesión", style = MaterialTheme.typography.labelLarge, color = NavyButton)
                }
            }
        }
    }

    if (state.editing) EditProfileSheet(state, viewModel)
}

@Composable
private fun Header(profile: Profile, role: String, uploading: Boolean, onChangePhoto: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(112.dp)) {
            Avatar(profile, Modifier.size(104.dp).align(Alignment.Center))
            if (uploading) {
                Box(Modifier.size(104.dp).align(Alignment.Center).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }
            }
            IconButton(
                onClick = onChangePhoto,
                enabled = !uploading,
                modifier = Modifier.align(Alignment.BottomEnd).size(36.dp).clip(CircleShape).background(Navy)
                    .border(3.dp, Background, CircleShape),
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = "Cambiar foto", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        Text(profile.fullName, style = MaterialTheme.typography.headlineSmall, color = Navy, textAlign = TextAlign.Center)
        Pill(role, container = TealSoft, content = TealDark)
    }
}

@Composable
private fun Avatar(profile: Profile, modifier: Modifier = Modifier) {
    val photo by produceState<ImageBitmap?>(null, profile.photoUrl) {
        value = profile.photoUrl?.let { url ->
            withContext(Dispatchers.IO) {
                runCatching { URL(url).openStream().use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
            }
        }
    }
    Box(modifier.clip(CircleShape).background(NavySoft).border(1.dp, Outline, CircleShape), contentAlignment = Alignment.Center) {
        val image = photo
        if (image != null) {
            Image(image, contentDescription = "Foto de perfil", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(profile.initials, color = Navy, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PersonalInformation(profile: Profile, onEdit: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Información personal", Modifier.weight(1f))
            TextButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = null, tint = NavyButton, modifier = Modifier.size(16.dp))
                Text(" Editar", style = MaterialTheme.typography.labelMedium, color = NavyButton)
            }
        }
        AliviaCard {
            InfoRow(Icons.Outlined.Person, "Nombres", profile.firstName)
            InfoRow(Icons.Outlined.Person, "Apellidos", profile.lastName)
            InfoRow(Icons.Outlined.Badge, "DNI", profile.dni ?: "—")
            InfoRow(Icons.Outlined.Phone, "Teléfono", profile.phoneNumber?.let { "+$it" } ?: "—")
            InfoRow(Icons.Outlined.Email, "Correo", profile.email)
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = InkMuted, modifier = Modifier.width(22.dp).size(18.dp))
        Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = InkMuted)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = Ink)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(state: ProfileUiState, viewModel: ProfileViewModel) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !state.saving },
    )
    ModalBottomSheet(onDismissRequest = viewModel::stopEditing, sheetState = sheetState, containerColor = Surface) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
                .padding(bottom = 16.dp).imePadding().navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Editar información", style = MaterialTheme.typography.headlineSmall, color = Navy)
            Text("El correo, el DNI y la fecha de nacimiento no se pueden cambiar.",
                style = MaterialTheme.typography.bodyMedium, color = InkMuted)
            Field("Nombres", "Tus nombres", state.firstName, viewModel::onFirstName, !state.saving)
            Field("Apellidos", "Tus apellidos", state.lastName, viewModel::onLastName, !state.saving)
            Field("Teléfono", "51987654321", state.phone, viewModel::onPhone, !state.saving, KeyboardType.Phone)
            state.saveError?.let { ErrorBanner(it) }
            PrimaryButton("Guardar", onClick = viewModel::save, icon = Icons.Outlined.Check,
                enabled = state.canSave, busy = state.saving, container = Navy)
            TextButton(onClick = viewModel::stopEditing, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                Text("Cancelar", color = NavyButton)
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    placeholder: String,
    value: String,
    onChange: (String) -> Unit,
    enabled: Boolean,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            enabled = enabled,
            singleLine = true,
            placeholder = { Text(placeholder) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Outline, focusedBorderColor = Navy),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
