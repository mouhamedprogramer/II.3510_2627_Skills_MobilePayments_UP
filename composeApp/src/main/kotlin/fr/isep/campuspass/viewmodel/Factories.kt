package fr.isep.campuspass.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.isep.campuspass.AppContainer

object Factories {
    val events: ViewModelProvider.Factory = viewModelFactory { initializer { EventsViewModel(AppContainer.events, AppContainer.ticketing) } }
    val myTickets: ViewModelProvider.Factory = viewModelFactory { initializer { MyTicketsViewModel(AppContainer.events, AppContainer.ticketing, AppContainer.auth) } }
    val scanner: ViewModelProvider.Factory = viewModelFactory { initializer { ScannerViewModel(AppContainer.events, AppContainer.ticketing) } }
    val profile: ViewModelProvider.Factory = viewModelFactory { initializer { ProfileViewModel(AppContainer.events, AppContainer.ticketing, AppContainer.auth) } }
    val login: ViewModelProvider.Factory = viewModelFactory { initializer { LoginViewModel(AppContainer.auth) } }
    val session: ViewModelProvider.Factory = viewModelFactory { initializer { SessionViewModel(AppContainer.auth) } }

    fun eventDetail(id: Int): ViewModelProvider.Factory =
        viewModelFactory { initializer { EventDetailViewModel(id, AppContainer.events, AppContainer.ticketing, AppContainer.auth) } }

    fun checkout(eventId: Int, quantity: Int): ViewModelProvider.Factory =
        viewModelFactory { initializer { CheckoutViewModel(eventId, quantity, AppContainer.events, AppContainer.ticketing, AppContainer.auth, AppContainer.googlePay) } }

    fun ticketDetail(id: String): ViewModelProvider.Factory =
        viewModelFactory { initializer { TicketDetailViewModel(id, AppContainer.events, AppContainer.ticketing, AppContainer.auth) } }

    fun order(transactionId: String): ViewModelProvider.Factory =
        viewModelFactory { initializer { OrderViewModel(transactionId, AppContainer.events, AppContainer.ticketing, AppContainer.auth) } }
}
