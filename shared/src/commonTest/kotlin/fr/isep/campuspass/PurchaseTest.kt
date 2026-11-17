package fr.isep.campuspass

import fr.isep.campuspass.domain.PaymentMethod
import fr.isep.campuspass.domain.PaymentReceipt
import fr.isep.campuspass.domain.TicketStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PurchaseTest {
    @Test fun purchaseCreatesSignedTicketsAndDecreasesSeats() {
        val box = newBoxOffice()
        val tx = box.purchase(alice, 1, 2, receipt).getOrThrow()
        assertEquals(3000, tx.amountCents)
        assertEquals(2, box.tickets.value.size)
        assertTrue(box.tickets.value.all { it.status == TicketStatus.VALID && it.code.startsWith("CPASS1:") })
        assertEquals(98, box.seatsLeft(1))
    }

    @Test fun quantityMustBeBetweenOneAndFour() {
        val box = newBoxOffice()
        assertTrue(box.purchase(alice, 1, 0, receipt).isFailure)
        assertTrue(box.purchase(alice, 1, 5, receipt).isFailure)
    }

    @Test fun cannotExceedFourTicketsPerStudentAndEvent() {
        val box = newBoxOffice()
        box.purchase(alice, 1, 3, receipt).getOrThrow()
        assertTrue(box.purchase(alice, 1, 2, receipt).isFailure)
        assertTrue(box.purchase(bob, 1, 2, receipt).isSuccess)
    }

    @Test fun cannotBuyMoreThanSeatsLeft() {
        val box = newBoxOffice()
        assertTrue(box.purchase(alice, 3, 3, receipt).isFailure)
        box.purchase(alice, 3, 2, receipt).getOrThrow()
        assertEquals("Sold out!", box.purchase(bob, 3, 1, receipt).exceptionOrNull()?.message)
    }

    @Test fun cannotBuyForAStartedEventOrWithoutPaymentOrAsOrganizer() {
        val box = newBoxOffice()
        assertTrue(box.purchase(alice, 4, 1, receipt).isFailure)
        assertTrue(box.purchase(alice, 1, 1, PaymentReceipt(PaymentMethod.GOOGLE_PAY, " ")).isFailure)
        assertTrue(box.purchase(organizer, 1, 1, receipt).isFailure)
    }
}
