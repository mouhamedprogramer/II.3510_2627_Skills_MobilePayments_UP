package fr.isep.campuspass.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/** Injectable clock so that time-based business rules (48 h refund window…) are testable. */
fun interface TimeSource {
    fun now(): Instant

    companion object {
        val System = TimeSource { Clock.System.now() }
    }
}

/** Events are expressed in Paris local time. */
val CampusTimeZone: TimeZone = TimeZone.of("Europe/Paris")

fun Event.startInstant(): Instant = startsAt.toInstant(CampusTimeZone)

fun LocalDateTime.toCampusInstant(): Instant = toInstant(CampusTimeZone)
