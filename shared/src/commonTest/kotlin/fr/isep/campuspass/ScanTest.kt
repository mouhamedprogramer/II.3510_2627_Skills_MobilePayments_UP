package fr.isep.campuspass

import fr.isep.campuspass.domain.ScanResult
import fr.isep.campuspass.security.TicketSigner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ScanTest {
    @Test fun aValidTicketCanOnlyBeUsedOnce() {
        val box = newBoxOffice()
        box.purchase(alice, 1, 1, receipt).getOrThrow()
        val code = box.tickets.value.first().code
        assertIs<ScanResult.Valid>(box.validate(code, 1))
        assertIs<ScanResult.AlreadyUsed>(box.validate(code, 1))
        assertEquals(1, box.checkedInCount(1))
    }

    @Test fun aTamperedCodeIsRejected() {
        val box = newBoxOffice()
        box.purchase(alice, 1, 1, receipt).getOrThrow()
        val code = box.tickets.value.first().code
        val tampered = code.dropLast(1) + if (code.last() == '0') "1" else "0"
        assertIs<ScanResult.Invalid>(box.validate(tampered, 1))
        assertIs<ScanResult.Invalid>(box.validate("hello world", 1))
    }

    @Test fun aCodeSignedWithAnotherSecretIsRejected() {
        val box = newBoxOffice()
        val forged = TicketSigner("attacker-secret").sign("TK-FAKE", 1)
        assertIs<ScanResult.Invalid>(box.validate(forged, 1))
    }

    @Test fun aTicketForAnotherEventIsRejected() {
        val box = newBoxOffice()
        box.purchase(alice, 1, 1, receipt).getOrThrow()
        assertIs<ScanResult.WrongEvent>(box.validate(box.tickets.value.first().code, 3))
    }

    @Test fun aRefundedTicketIsRejected() {
        val box = newBoxOffice()
        val tx = box.purchase(alice, 1, 1, receipt).getOrThrow()
        box.refund(alice, tx.id).getOrThrow()
        assertIs<ScanResult.Refunded>(box.validate(box.tickets.value.first().code, 1))
    }

    @Test fun signerRoundTrip() {
        val signer = TicketSigner("s3cret")
        val code = signer.sign("TK-ABC", 7)
        assertEquals(TicketSigner.SignedTicket("TK-ABC", 7), signer.verify(code))
        assertNotNull(signer.verify(" $code "))
        assertNull(TicketSigner("other").verify(code))
    }
}
