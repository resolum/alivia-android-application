package com.resolum.alivia.features.nursing.data.repositories

import com.resolum.alivia.features.nursing.data.datasources.remote.ActivityFacadeService
import com.resolum.alivia.features.nursing.data.datasources.remote.mappers.ActivityMapper
import com.resolum.alivia.features.nursing.domain.repositories.ActivityRepository
import com.resolum.alivia.features.shared.data.repositories.BaseRepository
import java.time.LocalDate

class ActivityRepositoryImpl(private val facade: ActivityFacadeService) : BaseRepository(), ActivityRepository {
    override suspend fun agenda(from: LocalDate, to: LocalDate) =
        safeCall { facade.agenda(from, to).mapNotNull(ActivityMapper::toDomain) }
}
