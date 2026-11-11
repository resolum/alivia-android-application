package com.resolum.alivia.features.profiles.data.repositories

import com.resolum.alivia.features.profiles.data.datasources.remote.ProfileFacadeService
import com.resolum.alivia.features.profiles.data.datasources.remote.mappers.ProfileMapper
import com.resolum.alivia.features.profiles.domain.models.ProfileChanges
import com.resolum.alivia.features.profiles.domain.repositories.ProfileRepository
import com.resolum.alivia.features.shared.data.repositories.BaseRepository

class ProfileRepositoryImpl(private val facade: ProfileFacadeService) : BaseRepository(), ProfileRepository {
    override suspend fun me() = safeCall { ProfileMapper.toDomain(facade.me()) }

    override suspend fun update(changes: ProfileChanges) = safeCall { ProfileMapper.toDomain(facade.update(changes)) }
}
