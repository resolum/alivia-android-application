package com.resolum.alivia.features.nursing.domain.usecases

import com.resolum.alivia.features.nursing.domain.models.CareActivity
import com.resolum.alivia.features.nursing.domain.models.MedicationAdministration
import com.resolum.alivia.features.nursing.domain.models.MedicationOutcome
import com.resolum.alivia.features.nursing.domain.models.MedicationResult
import com.resolum.alivia.features.nursing.domain.repositories.MedicationAdministrationRepository
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import java.time.LocalDateTime

/** Reads the current state of the dose behind an agenda occurrence, with its result when it already has one. */
class GetMedicationAdministrationUseCase(private val repository: MedicationAdministrationRepository) {
    suspend operator fun invoke(activity: CareActivity): NetworkResult<MedicationAdministration> {
        val assignmentId = activity.assignmentId ?: return NetworkResult.Error(NOT_RECORDABLE)
        return when (val result = repository.find(MedicationAdministration.idOf(assignmentId, activity.date))) {
            is NetworkResult.Error -> if (result.code == 404) NetworkResult.Error("No encontramos esta administración.", 404) else result
            is NetworkResult.Success -> result
        }
    }
}

sealed interface MedicationRecord {
    /** Saved now; [late] when it was recorded after the attention window. */
    data class Recorded(val administration: MedicationAdministration, val late: Boolean) : MedicationRecord

    /** The dose already had a definitive result, which is returned instead of recording a second one. */
    data class AlreadyRecorded(val administration: MedicationAdministration) : MedicationRecord

    /** Nothing was saved; [forbidden] when the account may not record this dose. */
    data class Rejected(val message: String, val forbidden: Boolean = false) : MedicationRecord
}

/**
 * Records that a dose was given or omitted, at most once: it checks the dose first and, when the backend still finds
 * a result (409), shows that result instead of a second one.
 */
class RecordMedicationOutcomeUseCase(
    private val repository: MedicationAdministrationRepository,
    private val now: () -> LocalDateTime,
) {
    suspend operator fun invoke(activity: CareActivity, result: MedicationResult, reason: String? = null): MedicationRecord {
        val assignmentId = activity.assignmentId ?: return MedicationRecord.Rejected(NOT_RECORDABLE)
        if (result == MedicationResult.MISSED) return MedicationRecord.Rejected(NOT_RECORDABLE)
        val cleanReason = reason?.trim()?.ifEmpty { null }
        if (result == MedicationResult.OMITTED && cleanReason == null) {
            return MedicationRecord.Rejected("Indica el motivo por el que no se administró.")
        }
        val id = MedicationAdministration.idOf(assignmentId, activity.date)

        when (val current = repository.find(id)) {
            is NetworkResult.Error -> return rejected(current)
            is NetworkResult.Success -> if (!current.data.pending) return MedicationRecord.AlreadyRecorded(current.data)
        }

        val at = now()
        return when (val saved = repository.record(id, result, cleanReason, at)) {
            is NetworkResult.Success -> {
                // The backend stamps the record with its own clock and drops the omission reason; show what was recorded here.
                val outcome = MedicationOutcome(result, at, cleanReason ?: saved.data.outcome?.reason, recordedBy = "Tú")
                val administration = saved.data.copy(outcome = outcome)
                MedicationRecord.Recorded(administration, late = administration.isLate(at))
            }
            is NetworkResult.Error -> if (saved.code == 409) {
                (repository.find(id) as? NetworkResult.Success)?.data?.takeIf { !it.pending }
                    ?.let { MedicationRecord.AlreadyRecorded(it) }
                    ?: MedicationRecord.Rejected("Esta administración ya tiene un resultado registrado.")
            } else rejected(saved)
        }
    }

    private fun rejected(error: NetworkResult.Error) = if (error.code == 403) {
        MedicationRecord.Rejected(FORBIDDEN, forbidden = true)
    } else {
        MedicationRecord.Rejected(if (error.code == 404) "No encontramos esta administración." else error.message)
    }

    companion object {
        const val FORBIDDEN = "No tienes permiso para registrar esta administración. El historial no se modificó."
    }
}

private const val NOT_RECORDABLE = "Esta administración no se puede registrar desde la app."
