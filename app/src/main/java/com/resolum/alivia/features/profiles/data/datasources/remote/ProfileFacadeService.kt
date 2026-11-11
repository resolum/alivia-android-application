package com.resolum.alivia.features.profiles.data.datasources.remote

import com.resolum.alivia.features.profiles.data.datasources.remote.services.ProfileService
import com.resolum.alivia.features.profiles.domain.models.ProfileChanges
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/** Entry point of the signed-in account's own profile. */
class ProfileFacadeService(private val service: ProfileService) {
    suspend fun me() = service.me()

    suspend fun update(changes: ProfileChanges) = service.update(
        firstName = changes.firstName?.let(::text),
        lastName = changes.lastName?.let(::text),
        phoneNumber = changes.phoneNumber?.let(::text),
        photo = changes.photo?.let {
            MultipartBody.Part.createFormData("photo", "photo.jpg", it.toRequestBody(JPEG))
        },
    )

    private fun text(value: String) = value.toRequestBody(TEXT)

    private companion object {
        val TEXT = "text/plain".toMediaType()
        val JPEG = "image/jpeg".toMediaType()
    }
}
