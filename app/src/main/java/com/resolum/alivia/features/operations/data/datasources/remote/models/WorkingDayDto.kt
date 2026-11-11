package com.resolum.alivia.features.operations.data.datasources.remote.models

/** A day of `/api/v1/technical-orders/mine/schedule`: ISO date and "HH:mm" blocks. */
data class WorkingDayDto(val date: String?, val shifts: List<ShiftDto>?) {
    data class ShiftDto(val start: String?, val end: String?)
}
