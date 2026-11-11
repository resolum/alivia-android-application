package com.resolum.alivia.features.profiles.presentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resolum.alivia.features.profiles.domain.models.Profile
import com.resolum.alivia.features.profiles.domain.usecases.GetProfileUseCase
import com.resolum.alivia.features.profiles.domain.usecases.UpdateProfileUseCase
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class ProfileUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val profile: Profile? = null,
    val error: String? = null,
    val editing: Boolean = false,
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val saving: Boolean = false,
    val saveError: String? = null,
    val uploadingPhoto: Boolean = false,
    val photoError: String? = null,
) {
    val canSave: Boolean get() = !saving && firstName.isNotBlank() && lastName.isNotBlank()
}

class ProfileViewModel(
    private val getProfile: GetProfileUseCase,
    private val updateProfile: UpdateProfileUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun load(refresh: Boolean = false) {
        _state.update { it.copy(loading = it.profile == null, refreshing = refresh, error = null) }
        viewModelScope.launch {
            when (val result = getProfile()) {
                is NetworkResult.Success -> _state.update { it.copy(loading = false, refreshing = false, profile = result.data) }
                // A failed reload must not hide the profile already on screen.
                is NetworkResult.Error -> _state.update {
                    it.copy(loading = false, refreshing = false, error = if (it.profile == null) result.message else null)
                }
            }
        }
    }

    fun startEditing() {
        val profile = _state.value.profile ?: return
        _state.update {
            it.copy(editing = true, firstName = profile.firstName, lastName = profile.lastName,
                phone = profile.phoneNumber.orEmpty(), saveError = null)
        }
    }

    fun stopEditing() {
        if (!_state.value.saving) _state.update { it.copy(editing = false) }
    }

    fun onFirstName(value: String) = _state.update { it.copy(firstName = value, saveError = null) }
    fun onLastName(value: String) = _state.update { it.copy(lastName = value, saveError = null) }
    fun onPhone(value: String) = _state.update { it.copy(phone = value, saveError = null) }

    fun save() {
        val current = _state.value
        val profile = current.profile ?: return
        if (!current.canSave) return
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val result = updateProfile(profile, current.firstName, current.lastName, current.phone)) {
                is NetworkResult.Success -> _state.update { it.copy(saving = false, editing = false, profile = result.data) }
                is NetworkResult.Error -> _state.update { it.copy(saving = false, saveError = result.message) }
            }
        }
    }

    /** Compresses the picked image to JPEG and sends it; on failure the profile stays as it was. */
    fun uploadPhoto(imageData: ByteArray) {
        if (_state.value.uploadingPhoto) return
        _state.update { it.copy(uploadingPhoto = true, photoError = null) }
        viewModelScope.launch {
            val jpeg = withContext(Dispatchers.Default) { compressed(imageData) }
            if (jpeg == null) {
                _state.update { it.copy(uploadingPhoto = false, photoError = "No pudimos leer esa imagen. Elige otra.") }
                return@launch
            }
            when (val result = updateProfile(jpeg)) {
                is NetworkResult.Success -> _state.update { it.copy(uploadingPhoto = false, profile = result.data) }
                is NetworkResult.Error -> _state.update { it.copy(uploadingPhoto = false, photoError = result.message) }
            }
        }
    }

    fun photoPickFailed() = _state.update { it.copy(photoError = "No pudimos abrir esa imagen. Elige otra.") }

    private companion object {
        const val MAX_SIDE = 1024

        /** JPEG of at most 1024 px on the long side, enough for an avatar. */
        fun compressed(data: ByteArray): ByteArray? {
            val image = BitmapFactory.decodeByteArray(data, 0, data.size) ?: return null
            val longSide = maxOf(image.width, image.height)
            val scaled = if (longSide > MAX_SIDE) {
                val scale = MAX_SIDE.toFloat() / longSide
                Bitmap.createScaledBitmap(image, (image.width * scale).toInt(), (image.height * scale).toInt(), true)
            } else image
            return ByteArrayOutputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)
                out.toByteArray()
            }
        }
    }
}
