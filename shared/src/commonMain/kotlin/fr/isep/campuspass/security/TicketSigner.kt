package fr.isep.campuspass.security

/** HMAC-SHA256, implemented per platform with javax.crypto (expect/actual). */
expect fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray

/**
 * Signs and verifies the content of ticket QR codes.
 *
 * QR content format: `CPASS1:<ticketId>:<eventId>:<signature>` where the signature is the first
 * 16 hex characters of HMAC-SHA256(secret, "<ticketId>:<eventId>").
 * Anyone can read a QR code, but nobody can create a valid one without the secret.
 * In production the secret stays on the server; here it lives in memory for the demo.
 */
class TicketSigner(private val secret: String) {

    fun sign(ticketId: String, eventId: Int): String =
        "$PREFIX:$ticketId:$eventId:${signature(ticketId, eventId)}"

    /** Returns the decoded content if the code is well-formed AND correctly signed, null otherwise. */
    fun verify(code: String): SignedTicket? {
        // TODO(Exercise 3a): decode and verify a QR code.
        //  - split the trimmed code on ':' → it must have 4 parts and start with PREFIX
        //  - parts[2] must be an Int (the event id)
        //  - recompute signature(ticketId, eventId) and compare it with parts[3] using constantTimeEquals
        //  - return SignedTicket(ticketId, eventId) if everything matches, null otherwise
        return null
    }

    private fun signature(ticketId: String, eventId: Int): String =
        hmacSha256(secret.encodeToByteArray(), "$ticketId:$eventId".encodeToByteArray())
            .joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
            .take(16)

    /** Avoids leaking information through comparison timing. */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    data class SignedTicket(val ticketId: String, val eventId: Int)

    companion object {
        const val PREFIX = "CPASS1"
    }
}
