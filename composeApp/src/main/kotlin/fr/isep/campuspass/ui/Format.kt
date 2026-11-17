package fr.isep.campuspass.ui

import fr.isep.campuspass.domain.CampusTimeZone
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime

private val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun Int.two() = toString().padStart(2, '0')

/** "Sat 12 Dec 2026 · 20:00" */
fun formatDateTime(d: LocalDateTime): String =
    "${days[d.dayOfWeek.ordinal]} ${d.dayOfMonth} ${months[d.monthNumber - 1]} ${d.year} · ${d.hour.two()}:${d.minute.two()}"

fun formatTime(d: LocalDateTime): String = "${d.hour.two()}:${d.minute.two()}"

fun monthShort(d: LocalDateTime): String = months[d.monthNumber - 1].uppercase()

fun formatInstant(i: Instant): String = formatDateTime(i.toLocalDateTime(CampusTimeZone))
