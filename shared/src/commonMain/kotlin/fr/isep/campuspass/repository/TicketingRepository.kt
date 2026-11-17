package fr.isep.campuspass.repository

import fr.isep.campuspass.domain.PaymentReceipt
import fr.isep.campuspass.domain.Role
import fr.isep.campuspass.domain.ScanResult
import fr.isep.campuspass.domain.Ticket
import fr.isep.campuspass.domain.TicketStatus
import fr.isep.campuspass.domain.TimeSource
import fr.isep.campuspass.domain.Transaction
import fr.isep.campuspass.domain.TransactionStatus
import fr.isep.campuspass.domain.UserAccount
import fr.isep.campuspass.domain.startInstant
import fr.isep.campuspass.security.TicketSigner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random
import kotlin.time.Duration.Companion.hours

/**
 * Box office: purchase, refund and entrance validation of tickets.
 * All business rules live here (domain/data layer), never in the composables.
 */
interface TicketingRepository {
    val tickets: StateFlow<List<Ticket>>
    val transactions: StateFlow<List<Transaction>>

    /** Seats still available for an event (capacity − sold tickets that are not refunded). */
    fun seatsLeft(eventId: Int): Int
    fun soldCount(eventId: Int): Int

    /** Creates the tickets after a successful payment. */
    fun purchase(user: UserAccount, eventId: Int, quantity: Int, receipt: PaymentReceipt): Result<Transaction>

    /** Refunds a whole transaction if the event starts in more than 48 h and no ticket was used. */
    fun refund(user: UserAccount, transactionId: String): Result<Transaction>

    /** Validates a scanned QR code for [eventId]: a valid ticket becomes USED (single use). */
    fun validate(code: String, eventId: Int): ScanResult

    fun checkedInCount(eventId: Int): Int
}

class InMemoryTicketingRepository(
    private val events: EventRepository,
    private val signer: TicketSigner,
    private val time: TimeSource = TimeSource.System,
    /** Tickets already sold before the app started (demo data), per event id. */
    private val alreadySold: Map<Int, Int> = emptyMap(),
    private val random: Random = Random.Default,
) : TicketingRepository {

    private val _tickets = MutableStateFlow<List<Ticket>>(emptyList())
    override val tickets: StateFlow<List<Ticket>> = _tickets
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    override val transactions: StateFlow<List<Transaction>> = _transactions

    override fun soldCount(eventId: Int): Int =
        (alreadySold[eventId] ?: 0) + _tickets.value.count { it.eventId == eventId && it.status != TicketStatus.REFUNDED }

    override fun seatsLeft(eventId: Int): Int {
        val event = events.event(eventId) ?: return 0
        return (event.capacity - soldCount(eventId)).coerceAtLeast(0)
    }

    override fun checkedInCount(eventId: Int): Int =
        _tickets.value.count { it.eventId == eventId && it.status == TicketStatus.USED }

    override fun purchase(user: UserAccount, eventId: Int, quantity: Int, receipt: PaymentReceipt): Result<Transaction> {
        val event = events.event(eventId) ?: return failure("This event does not exist.")
        val now = time.now()
        val alreadyOwned = _tickets.value.count { it.eventId == eventId && it.ownerId == user.id && it.status != TicketStatus.REFUNDED }
        val error = when {
            user.role != Role.STUDENT -> "Only student accounts can buy tickets."
            quantity !in 1..MAX_TICKETS_PER_ORDER -> "You can buy between 1 and $MAX_TICKETS_PER_ORDER tickets per order."
            alreadyOwned + quantity > MAX_TICKETS_PER_STUDENT -> "Limit reached: $MAX_TICKETS_PER_STUDENT tickets per student for this event."
            event.startInstant() <= now -> "This event has already started."
            seatsLeft(eventId) < quantity -> if (seatsLeft(eventId) == 0) "Sold out!" else "Only ${seatsLeft(eventId)} seat(s) left."
            receipt.token.isBlank() -> "Payment was not confirmed."
            else -> null
        }
        if (error != null) return failure(error)

        val transactionId = newId("TX")
        val newTickets = List(quantity) {
            val ticketId = newId("TK")
            Ticket(
                id = ticketId,
                eventId = eventId,
                ownerId = user.id,
                holderName = user.name,
                transactionId = transactionId,
                purchasedAt = now,
                code = signer.sign(ticketId, eventId),
            )
        }
        val transaction = Transaction(
            id = transactionId,
            userId = user.id,
            eventId = eventId,
            ticketIds = newTickets.map { it.id },
            quantity = quantity,
            amountCents = event.priceCents * quantity,
            method = receipt.method,
            paymentToken = receipt.token,
            status = TransactionStatus.PAID,
            createdAt = now,
        )
        _tickets.value = _tickets.value + newTickets
        _transactions.value = _transactions.value + transaction
        return Result.success(transaction)
    }

    override fun refund(user: UserAccount, transactionId: String): Result<Transaction> {
        val tx = _transactions.value.firstOrNull { it.id == transactionId } ?: return failure("Order not found.")
        val event = events.event(tx.eventId) ?: return failure("This event does not exist.")
        val txTickets = _tickets.value.filter { it.transactionId == tx.id }
        val error = when {
            tx.userId != user.id -> "Only the buyer can request a refund."
            tx.status == TransactionStatus.REFUNDED -> "This order has already been refunded."
            txTickets.any { it.status == TicketStatus.USED } -> "A ticket of this order has already been used."
            time.now() > event.startInstant() - REFUND_DEADLINE -> "Refunds close 48 h before the event."
            else -> null
        }
        if (error != null) return failure(error)

        val now = time.now()
        val refunded = tx.copy(status = TransactionStatus.REFUNDED, refundedAt = now)
        _tickets.value = _tickets.value.map { if (it.transactionId == tx.id) it.copy(status = TicketStatus.REFUNDED) else it }
        _transactions.value = _transactions.value.map { if (it.id == tx.id) refunded else it }
        return Result.success(refunded)
    }

    override fun validate(code: String, eventId: Int): ScanResult {
        val signed = signer.verify(code) ?: return ScanResult.Invalid("Forged or unreadable QR code.")
        val ticket = _tickets.value.firstOrNull { it.id == signed.ticketId }
            ?: return ScanResult.Invalid("Unknown ticket.")
        if (ticket.eventId != eventId) return ScanResult.WrongEvent(ticket, events.event(ticket.eventId))
        val event = events.event(eventId) ?: return ScanResult.Invalid("Unknown event.")
        return when (ticket.status) {
            TicketStatus.REFUNDED -> ScanResult.Refunded(ticket)
            TicketStatus.USED -> ScanResult.AlreadyUsed(ticket, ticket.scannedAt ?: time.now())
            TicketStatus.VALID -> {
                val used = ticket.copy(status = TicketStatus.USED, scannedAt = time.now())
                _tickets.value = _tickets.value.map { if (it.id == used.id) used else it }
                ScanResult.Valid(used, event)
            }
        }
    }

    private fun newId(prefix: String): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return prefix + "-" + (1..8).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
    }

    private fun <T> failure(message: String): Result<T> = Result.failure(IllegalStateException(message))

    companion object {
        const val MAX_TICKETS_PER_ORDER = 4
        const val MAX_TICKETS_PER_STUDENT = 4
        val REFUND_DEADLINE = 48.hours
    }
}
