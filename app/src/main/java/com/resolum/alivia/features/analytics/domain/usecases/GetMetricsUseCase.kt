package com.resolum.alivia.features.analytics.domain.usecases

import com.resolum.alivia.features.analytics.domain.models.AutonomyAction
import com.resolum.alivia.features.analytics.domain.models.AutonomyCalculator
import com.resolum.alivia.features.analytics.domain.models.AutonomyMetrics
import com.resolum.alivia.features.analytics.domain.models.AutonomySummary
import com.resolum.alivia.features.analytics.domain.models.MetricPeriod
import com.resolum.alivia.features.analytics.domain.repositories.MetricRepository
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.shared.data.datasources.remote.map
import java.time.LocalDate

/** The actions of one period and device, each counted once, newest first, with their summary. */
data class AutonomyReport(val metrics: AutonomyMetrics, val actions: List<AutonomyAction>, val summary: AutonomySummary)

class GetMetricsUseCase(private val repository: MetricRepository) {
    suspend operator fun invoke(period: MetricPeriod, deviceId: Long?, today: LocalDate): NetworkResult<AutonomyReport> {
        val from = period.from(today)
        return repository.autonomy(from, today, deviceId).map { metrics ->
            // Filtered here too, so the summary matches the selection even if the backend ignores a filter.
            val actions = AutonomyCalculator.filter(AutonomyCalculator.distinct(metrics.actions), deviceId, from, today)
                .sortedByDescending { it.occurredAt }
            AutonomyReport(metrics, actions, AutonomyCalculator.summarize(actions))
        }
    }
}
