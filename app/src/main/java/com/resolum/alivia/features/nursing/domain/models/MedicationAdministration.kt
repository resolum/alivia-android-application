package com.resolum.alivia.features.nursing.domain.models

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** What happened with a scheduled dose. [MISSED] is set by the backend when the window closed without a record. */
enum class MedicationResult { ADMINISTERED, OMITTED, MISSED }

/** The definitive result of a dose: once it exists the dose cannot be confirmed again. */
data class MedicationOutcome(
    val result: MedicationResult,
    /** Real moment of the record, which can differ from the scheduled time. */
    val occurredAt: LocalDateTime?,
    val reason: String?,
    /** Who recorded it, when known. */
    val recordedBy: String? = null,
)

/** One scheduled dose of a medication (an occurrence of a MEDICATION activity). */
data class MedicationAdministration(
    val id: String,
    val medication: String,
    val assistedPerson: String?,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime?,
    val responsible: String?,
    val status: CareActivityStatus,
    val outcome: MedicationOutcome?,
) {
    val windowStart: LocalDateTime get() = LocalDateTime.of(date, startTime)

    /** End of the attention window; a dose without an end time gets [DEFAULT_WINDOW_MINUTES]. */
    val windowEnd: LocalDateTime
        get() = endTime?.let { LocalDateTime.of(if (it.isBefore(startTime)) date.plusDays(1) else date, it) }
            ?: windowStart.plusMinutes(DEFAULT_WINDOW_MINUTES)

    /** A record made after the window closed counts as late; the real time is kept. */
    fun isLate(at: LocalDateTime): Boolean = at.isAfter(windowEnd)

    val recordedLate: Boolean get() = outcome?.occurredAt?.let(::isLate) == true

    /** Pending while it has no definitive result. */
    val pending: Boolean get() = outcome == null && status != CareActivityStatus.MISSED

    companion object {
        const val DEFAULT_WINDOW_MINUTES = 60L

        /** The backend identifies a dose as `{assignmentId}-{yyyyMMdd}`. */
        fun idOf(assignmentId: Long, date: LocalDate): String = "$assignmentId-${date.format(DateTimeFormatter.BASIC_ISO_DATE)}"
    }
}
