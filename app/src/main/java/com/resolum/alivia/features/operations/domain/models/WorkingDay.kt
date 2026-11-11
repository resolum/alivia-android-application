package com.resolum.alivia.features.operations.domain.models

import java.time.LocalDate
import java.time.LocalTime

/** A day of the technician's assigned schedule, with its working blocks in order. */
data class WorkingDay(val date: LocalDate, val shifts: List<Shift>) {
    data class Shift(val start: LocalTime, val end: LocalTime)
}
