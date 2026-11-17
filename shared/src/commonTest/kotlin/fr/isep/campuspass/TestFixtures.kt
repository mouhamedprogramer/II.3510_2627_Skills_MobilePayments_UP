package fr.isep.campuspass

import fr.isep.campuspass.domain.Event
import fr.isep.campuspass.domain.EventCategory
import fr.isep.campuspass.domain.PaymentMethod
import fr.isep.campuspass.domain.PaymentReceipt
import fr.isep.campuspass.domain.TimeSource
import fr.isep.campuspass.domain.toCampusInstant
import fr.isep.campuspass.repository.AuthRepository
import fr.isep.campuspass.repository.InMemoryEventRepository
import fr.isep.campuspass.repository.InMemoryTicketingRepository
import fr.isep.campuspass.security.TicketSigner
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime

/** Mutable clock for tests. */
class FakeClock(var now: Instant) : TimeSource {
    override fun now(): Instant = now
}

val alice = AuthRepository.defaultAccounts[0]
val bob = AuthRepository.defaultAccounts[1]
val organizer = AuthRepository.defaultAccounts[2]
val receipt = PaymentReceipt(PaymentMethod.TEST_CARD, "tok_test_123")

/** "Now" for every test: 17 Nov 2026, 10:00 (Paris). */
val testNow: Instant = LocalDateTime(2026, 11, 17, 10, 0).toCampusInstant()

fun event(id: Int, startsAt: LocalDateTime, capacity: Int = 100, price: Long = 1500) = Event(
    id = id, title = "Event $id", description = "", category = EventCategory.PARTY, venue = "ISEP",
    startsAt = startsAt, priceCents = price, capacity = capacity, posterUrl = null, organizer = "BDE",
)

/** Event 1: in 10 days. Event 2: in 24 h (refund closed). Event 3: 2 seats only. Event 4: already started. */
fun newBoxOffice(clock: FakeClock = FakeClock(testNow), signer: TicketSigner = TicketSigner("test-secret")) =
    InMemoryTicketingRepository(
        events = InMemoryEventRepository(
            listOf(
                event(1, LocalDateTime(2026, 11, 27, 20, 0)),
                event(2, LocalDateTime(2026, 11, 18, 10, 0)),
                event(3, LocalDateTime(2026, 12, 1, 20, 0), capacity = 2),
                event(4, LocalDateTime(2026, 11, 16, 20, 0)),
            ),
        ),
        signer = signer,
        time = clock,
    )
