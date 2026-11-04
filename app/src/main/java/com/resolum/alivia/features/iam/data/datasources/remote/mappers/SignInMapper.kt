package com.resolum.alivia.features.iam.data.datasources.remote.mappers

import com.resolum.alivia.features.iam.data.datasources.remote.models.SignInResponseDto
import com.resolum.alivia.features.iam.domain.models.Session
import com.resolum.alivia.features.iam.domain.models.UserRole
import com.resolum.alivia.features.shared.data.datasources.datastore.StoredSession

object SignInMapper {
    fun toStoredSession(dto: SignInResponseDto): StoredSession? {
        val access = dto.accessToken ?: return null
        val refresh = dto.refreshToken ?: return null
        val user = dto.user ?: return null
        return StoredSession(access, refresh, user.id.orEmpty(), user.email.orEmpty(), user.role.orEmpty())
    }

    fun toSession(stored: StoredSession): Session =
        Session(stored.accountId, stored.email, UserRole.from(stored.role))
}
