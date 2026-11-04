package com.resolum.alivia.core.ui.format

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Dates and times in Peruvian Spanish, as the designs show them ("Hoy, 24 de octubre · 9:00 a. m."). */
object Formats {
    val locale: Locale = Locale.forLanguageTag("es-PE")
    private val zone: ZoneId = ZoneId.of("America/Lima")
    private val dayMonth = DateTimeFormatter.ofPattern("d 'de' MMMM", locale)
    private val weekdayDayMonth = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", locale)
    private val time = DateTimeFormatter.ofPattern("h:mm a", locale)
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    private val shortDay = DateTimeFormatter.ofPattern("d MMM", locale)

    fun today(): LocalDate = LocalDate.now(zone)

    fun time(value: LocalTime): String = value.format(time)

    fun window(start: LocalTime, end: LocalTime): String = "${time(start)} – ${time(end)}"

    /** "Hoy, 24 de octubre", "Mañana, 25 de octubre" or "Lunes 28 de octubre". */
    fun day(date: LocalDate, today: LocalDate = today()): String = when (date) {
        today -> "Hoy, ${date.format(dayMonth)}"
        today.plusDays(1) -> "Mañana, ${date.format(dayMonth)}"
        today.minusDays(1) -> "Ayer, ${date.format(dayMonth)}"
        else -> date.format(weekdayDayMonth).replaceFirstChar { it.titlecase(locale) }
    }

    fun monthYear(date: LocalDate): String = date.format(monthYear).replaceFirstChar { it.titlecase(locale) }

    /** "10:14 a. m. · Hoy, 24 Oct" for the moment an order closed. */
    fun moment(value: Instant, today: LocalDate = today()): String {
        val local = value.atZone(zone)
        val date = local.toLocalDate()
        val dayText = when (date) {
            today -> "Hoy, ${date.format(shortDay)}"
            today.minusDays(1) -> "Ayer, ${date.format(shortDay)}"
            else -> date.format(shortDay)
        }
        return "${local.toLocalTime().format(time)} · $dayText"
    }
}
