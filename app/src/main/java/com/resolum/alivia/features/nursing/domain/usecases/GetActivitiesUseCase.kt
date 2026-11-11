package com.resolum.alivia.features.nursing.domain.usecases

import com.resolum.alivia.features.nursing.domain.repositories.ActivityRepository
import java.time.LocalDate

/** Activities of the care agenda from yesterday to the day after tomorrow; later repetitions stay hidden. */
class GetActivitiesUseCase(private val repository: ActivityRepository) {
    suspend operator fun invoke(today: LocalDate) = repository.agenda(today.minusDays(DAYS_BACK), today.plusDays(DAYS_AHEAD))

    companion object {
        const val DAYS_BACK = 1L
        const val DAYS_AHEAD = 2L
    }
}
