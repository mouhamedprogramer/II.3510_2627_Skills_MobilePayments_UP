package fr.isep.campuspass.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.isep.campuspass.domain.Event
import fr.isep.campuspass.domain.EventCategory
import fr.isep.campuspass.domain.PaymentMethod
import fr.isep.campuspass.domain.PaymentReceipt
import fr.isep.campuspass.domain.Role
import fr.isep.campuspass.domain.ScanResult
import fr.isep.campuspass.domain.Ticket
import fr.isep.campuspass.domain.TicketStatus
import fr.isep.campuspass.domain.Transaction
import fr.isep.campuspass.domain.TransactionStatus
import fr.isep.campuspass.domain.UserAccount
import fr.isep.campuspass.domain.startInstant
import fr.isep.campuspass.payment.GooglePayClient
import fr.isep.campuspass.repository.AuthRepository
import fr.isep.campuspass.repository.EventRepository
import fr.isep.campuspass.repository.TicketingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.random.Random

/** Loading / Success / Error pattern for the event list. */
sealed interface EventsUiState {
    data object Loading : EventsUiState
    data class Success(val featured: List<Event>, val events: List<Event>, val seatsLeft: Map<Int, Int>) : EventsUiState
    data class Error(val message: String) : EventsUiState
}

class EventsViewModel(private val events: EventRepository, private val ticketing: TicketingRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _category = MutableStateFlow<EventCategory?>(null)
    val category: StateFlow<EventCategory?> = _category.asStateFlow()

    val uiState: StateFlow<EventsUiState> =
        combine(events.events, ticketing.tickets, _query, _category) { all, _, q, c ->
            val list = events.search(q, c)
            EventsUiState.Success(
                featured = all.take(3),
                events = list,
                seatsLeft = all.associate { it.id to ticketing.seatsLeft(it.id) },
            ) as EventsUiState
        }
            .catch { emit(EventsUiState.Error(it.message ?: "Unknown error")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventsUiState.Loading)

    fun onQuery(value: String) { _query.value = value }
    fun onCategory(value: EventCategory?) { _category.value = value }
}

data class EventDetailUi(val event: Event, val seatsLeft: Int, val sold: Int, val user: UserAccount?)

class EventDetailViewModel(
    private val eventId: Int,
    private val events: EventRepository,
    private val ticketing: TicketingRepository,
    auth: AuthRepository,
) : ViewModel() {
    val ui: StateFlow<EventDetailUi?> = combine(events.events, ticketing.tickets, auth.currentUser) { _, _, user ->
        events.event(eventId)?.let { EventDetailUi(it, ticketing.seatsLeft(eventId), ticketing.soldCount(eventId), user) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _quantity = MutableStateFlow(1)
    val quantity: StateFlow<Int> = _quantity.asStateFlow()

    fun increment() = _quantity.update { (it + 1).coerceAtMost(minOf(4, ui.value?.seatsLeft ?: 4).coerceAtLeast(1)) }
    fun decrement() = _quantity.update { (it - 1).coerceAtLeast(1) }
}

/** Steps of the checkout. Biometric confirmation and the Google Pay sheet are handled by the UI layer. */
sealed interface CheckoutStep {
    data object Ready : CheckoutStep
    data object Processing : CheckoutStep
    data class Done(val transactionId: String) : CheckoutStep
    data class Failed(val message: String) : CheckoutStep
}

class CheckoutViewModel(
    private val eventId: Int,
    val quantity: Int,
    events: EventRepository,
    private val ticketing: TicketingRepository,
    private val auth: AuthRepository,
    val googlePay: GooglePayClient,
) : ViewModel() {
    val event: Event? = events.event(eventId)
    val totalCents: Long = (event?.priceCents ?: 0) * quantity

    /** null while checking, then true if Google Pay can be used on this device. */
    private val _googlePayReady = MutableStateFlow<Boolean?>(null)
    val googlePayReady: StateFlow<Boolean?> = _googlePayReady.asStateFlow()

    private val _step = MutableStateFlow<CheckoutStep>(CheckoutStep.Ready)
    val step: StateFlow<CheckoutStep> = _step.asStateFlow()

    init {
        viewModelScope.launch { _googlePayReady.value = googlePay.isReadyToPay() }
    }

    /** Called once the payment sheet returned a token: creates the tickets (simulated server call). */
    fun completePurchase(receipt: PaymentReceipt) {
        val user = auth.currentUser.value ?: return fail("Please sign in first.")
        _step.value = CheckoutStep.Processing
        viewModelScope.launch {
            delay(1200) // the payment provider confirms the charge
            ticketing.purchase(user, eventId, quantity, receipt).fold(
                onSuccess = { _step.value = CheckoutStep.Done(it.id) },
                onFailure = { fail(it.message ?: "Payment failed") },
            )
        }
    }

    /** Token returned by the "test card" form when Google Pay is not available (emulator without account). */
    fun payWithTestCard() = completePurchase(PaymentReceipt(PaymentMethod.TEST_CARD, "tok_test_" + Random.nextLong(1_000_000, 9_999_999)))

    fun fail(message: String) { _step.value = CheckoutStep.Failed(message) }
    fun reset() { _step.value = CheckoutStep.Ready }
}

data class TicketItem(val ticket: Ticket, val event: Event)

data class MyTicketsUi(val upcoming: List<TicketItem>, val past: List<TicketItem>)

class MyTicketsViewModel(events: EventRepository, ticketing: TicketingRepository, auth: AuthRepository) : ViewModel() {
    val ui: StateFlow<MyTicketsUi> = combine(ticketing.tickets, auth.currentUser) { tickets, user ->
        val now = Clock.System.now()
        val mine = tickets.filter { it.ownerId == user?.id }
            .mapNotNull { t -> events.event(t.eventId)?.let { TicketItem(t, it) } }
            .sortedBy { it.event.startsAt }
        val (upcoming, past) = mine.partition { it.ticket.status == TicketStatus.VALID && it.event.startInstant() > now }
        MyTicketsUi(upcoming, past)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyTicketsUi(emptyList(), emptyList()))
}

data class TicketDetailUi(val ticket: Ticket, val event: Event, val transaction: Transaction?, val orderTickets: List<Ticket>)

class TicketDetailViewModel(
    private val ticketId: String,
    private val events: EventRepository,
    private val ticketing: TicketingRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    val ui: StateFlow<TicketDetailUi?> = combine(ticketing.tickets, ticketing.transactions) { tickets, txs ->
        val t = tickets.firstOrNull { it.id == ticketId } ?: return@combine null
        val e = events.event(t.eventId) ?: return@combine null
        TicketDetailUi(t, e, txs.firstOrNull { it.id == t.transactionId }, tickets.filter { it.transactionId == t.transactionId })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val buyerName: String get() = auth.currentUser.value?.name ?: ""

    /** Returns null on success, or the reason why the refund is refused. */
    fun refund(): String? {
        val user = auth.currentUser.value ?: return "Please sign in first."
        val tx = ui.value?.transaction ?: return "Order not found."
        return ticketing.refund(user, tx.id).exceptionOrNull()?.message
    }
}

data class ScannerUi(val events: List<Event>, val selected: Event?, val sold: Int, val checkedIn: Int)

class ScannerViewModel(private val events: EventRepository, private val ticketing: TicketingRepository) : ViewModel() {
    private val _selectedId = MutableStateFlow(events.events.value.firstOrNull()?.id)
    private val _result = MutableStateFlow<ScanResult?>(null)
    val result: StateFlow<ScanResult?> = _result.asStateFlow()

    val ui: StateFlow<ScannerUi> = combine(events.events, ticketing.tickets, _selectedId) { all, _, id ->
        val selected = all.firstOrNull { it.id == id }
        ScannerUi(all, selected, selected?.let { ticketing.soldCount(it.id) } ?: 0, selected?.let { ticketing.checkedInCount(it.id) } ?: 0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScannerUi(events.events.value, null, 0, 0))

    fun select(eventId: Int) { _selectedId.value = eventId }

    fun validate(code: String) {
        val eventId = _selectedId.value ?: return
        _result.value = ticketing.validate(code, eventId)
    }

    fun dismiss() { _result.value = null }
}

data class OrderItem(val transaction: Transaction, val event: Event)

data class ProfileUi(val user: UserAccount, val orders: List<OrderItem>, val spentCents: Long, val ticketCount: Int)

class ProfileViewModel(events: EventRepository, ticketing: TicketingRepository, private val auth: AuthRepository) : ViewModel() {
    val ui: StateFlow<ProfileUi?> = combine(auth.currentUser, ticketing.transactions) { user, txs ->
        user?.let { u ->
            val mine = txs.filter { it.userId == u.id }.sortedByDescending { it.createdAt }
                .mapNotNull { tx -> events.event(tx.eventId)?.let { OrderItem(tx, it) } }
            val paid = mine.filter { it.transaction.status == TransactionStatus.PAID }
            ProfileUi(u, mine, paid.sumOf { it.transaction.amountCents }, paid.sumOf { it.transaction.quantity })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun logout() = auth.logout()
}

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun login(email: String, password: String): UserAccount? {
        val account = auth.authenticate(email, password)
        _error.value = if (account == null) "Wrong e-mail or password." else null
        return account
    }
}

/** Current user, exposed to the navigation (bottom bar tabs, protected routes). */
class SessionViewModel(auth: AuthRepository) : ViewModel() {
    val user: StateFlow<UserAccount?> = auth.currentUser
    val isOrganizer: StateFlow<Boolean> = auth.currentUser.map { it?.role == Role.ORGANIZER }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
}

data class OrderUi(val transaction: Transaction, val event: Event, val tickets: List<Ticket>)

/** A single order (confirmation screen, PDF receipt). */
class OrderViewModel(private val transactionId: String, events: EventRepository, ticketing: TicketingRepository, private val auth: AuthRepository) : ViewModel() {
    val ui: StateFlow<OrderUi?> = combine(ticketing.transactions, ticketing.tickets) { txs, tickets ->
        val tx = txs.firstOrNull { it.id == transactionId } ?: return@combine null
        val e = events.event(tx.eventId) ?: return@combine null
        OrderUi(tx, e, tickets.filter { it.transactionId == tx.id })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val buyerName: String get() = auth.currentUser.value?.name ?: ""
}
