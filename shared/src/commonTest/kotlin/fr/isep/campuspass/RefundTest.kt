package fr.isep.campuspass

import fr.isep.campuspass.domain.TicketStatus
import fr.isep.campuspass.domain.TransactionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RefundTest {
    @Test fun refundMoreThan48hBeforeFreesTheSeats() {
        val box = newBoxOffice()
        val tx = box.purchase(alice, 1, 2, receipt).getOrThrow()
        val refunded = box.refund(alice, tx.id).getOrThrow()
        assertEquals(TransactionStatus.REFUNDED, refunded.status)
        assertTrue(box.tickets.value.all { it.status == TicketStatus.REFUNDED })
        assertEquals(100, box.seatsLeft(1))
    }

    @Test fun refundIsClosedLessThan48hBeforeTheEvent() {
        val box = newBoxOffice()
        val tx = box.purchase(alice, 2, 1, receipt).getOrThrow()
        assertEquals("Refunds close 48 h before the event.", box.refund(alice, tx.id).exceptionOrNull()?.message)
    }

    @Test fun onlyTheBuyerCanRefundAndOnlyOnce() {
        val box = newBoxOffice()
        val tx = box.purchase(alice, 1, 1, receipt).getOrThrow()
        assertTrue(box.refund(bob, tx.id).isFailure)
        assertTrue(box.refund(alice, tx.id).isSuccess)
        assertTrue(box.refund(alice, tx.id).isFailure)
    }

    @Test fun cannotRefundAfterATicketWasScanned() {
        val box = newBoxOffice()
        val tx = box.purchase(alice, 1, 1, receipt).getOrThrow()
        box.validate(box.tickets.value.first().code, 1)
        assertTrue(box.refund(alice, tx.id).isFailure)
    }
}
