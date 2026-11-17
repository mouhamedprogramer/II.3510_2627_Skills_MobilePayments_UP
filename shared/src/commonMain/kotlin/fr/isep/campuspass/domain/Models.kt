package fr.isep.campuspass.domain

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime

/** Category of a student event, used for filtering and visuals. */
enum class EventCategory(val label: String) {
    PARTY("Party"),
    CONCERT("Concert"),
    SPORT("Sport"),
    TALK("Talks & Tech"),
    CULTURE("Culture"),
    TRIP("Trips"),
}

/** A student event that sells tickets. Prices are stored in euro cents to avoid floating-point errors. */
data class Event(
    val id: Int,
    val title: String,
    val description: String,
    val category: EventCategory,
    val venue: String,
    val startsAt: LocalDateTime,
    val priceCents: Long,
    val capacity: Int,
    val posterUrl: String?,
    val organizer: String,
)

enum class Role { STUDENT, ORGANIZER }

/** In-memory demo account (no real hashing on purpose, like the course labs). */
data class UserAccount(
    val id: Int,
    val name: String,
    val email: String,
    val password: String,
    val role: Role,
)

enum class TicketStatus { VALID, USED, REFUNDED }

/**
 * A single-use ticket. [code] is the content of the QR code: it is signed (HMAC) so it cannot be forged.
 */
data class Ticket(
    val id: String,
    val eventId: Int,
    val ownerId: Int,
    val holderName: String,
    val transactionId: String,
    val purchasedAt: Instant,
    val status: TicketStatus = TicketStatus.VALID,
    val scannedAt: Instant? = null,
    val code: String,
)

enum class PaymentMethod(val label: String) { GOOGLE_PAY("Google Pay"), TEST_CARD("Test card") }

enum class TransactionStatus { PAID, REFUNDED }

/** Result of a successful payment, provided by the UI layer (Google Pay token or test card token). */
data class PaymentReceipt(val method: PaymentMethod, val token: String)

data class Transaction(
    val id: String,
    val userId: Int,
    val eventId: Int,
    val ticketIds: List<String>,
    val quantity: Int,
    val amountCents: Long,
    val method: PaymentMethod,
    val paymentToken: String,
    val status: TransactionStatus,
    val createdAt: Instant,
    val refundedAt: Instant? = null,
)

/** Outcome of scanning a ticket at the entrance. */
sealed interface ScanResult {
    data class Valid(val ticket: Ticket, val event: Event) : ScanResult
    data class AlreadyUsed(val ticket: Ticket, val scannedAt: Instant) : ScanResult
    data class Refunded(val ticket: Ticket) : ScanResult
    data class WrongEvent(val ticket: Ticket, val ticketEvent: Event?) : ScanResult
    data class Invalid(val reason: String) : ScanResult
}

/** "€35.00" style formatting, shared by the UI and the PDF receipts. */
fun Long.formatEuros(): String {
    val euros = this / 100
    val cents = (this % 100).toString().padStart(2, '0')
    return "€$euros.$cents"
}
