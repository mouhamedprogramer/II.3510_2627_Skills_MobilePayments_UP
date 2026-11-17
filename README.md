# CampusPass — Unfinished Project (UP)

**II.3510 Mobile Application Development for Android — A3 2026-2027 — Students presentations (Skills)**
**Topic:** Mobile payments & secure transactions — presented by Mouhamadou Soukhouna

Welcome! CampusPass is a ticketing app for student events. The app already runs, but the
**security and payment features are missing**: it is your job to implement them.

At the end you will have:
- a QR code generated for each ticket (ZXing),
- a fingerprint confirmation before paying (BiometricPrompt),
- signed, single-use tickets validated at the entrance (HMAC-SHA256),
- a real Google Pay payment sheet (TEST environment, no real money),
- a PDF e-ticket (bonus) and the 48 h refund rule (bonus).

📽️ The slides of the presentation are in [`slides/`](slides/).
✅ The full solution is on the [`solution`](../../tree/solution) branch — try first, peek later!

## 0. Setup (5 min)

1. Clone this repository and open it in **Android Studio**.
2. Use an emulator **with Google APIs** (Pixel, API 34+), Android 8.0 minimum.
3. Run the app (`composeApp` ▶). Sign in with a demo account:

| Role | E-mail | Password |
|---|---|---|
| Student | alice@isep.fr | alice123 |
| Student | bob@isep.fr | bob123 |
| Organizer (scanner) | bde@isep.fr | bde123 |

4. Run the unit tests — **6 of them fail on purpose** (exercises 3 and 6):

```bash
./gradlew :shared:jvmTest
```

All the places to edit are marked `// TODO(Exercise N)` (Android Studio › View › Tool Windows › TODO).

---

## Exercise 1 ⭐ — Generate the ticket QR code (ZXing)
📄 `composeApp/src/main/kotlin/fr/isep/campuspass/qr/QrCodes.kt` → `generate()`

Today every ticket shows a grey square. Draw the real QR code of `content` with `QRCodeWriter`.

<details><summary>Hint</summary>

```kotlin
val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 1)
val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
val pixels = IntArray(matrix.width * matrix.height) { i ->
    if (matrix[i % matrix.width, i / matrix.width]) Color.BLACK else Color.WHITE
}
return Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
```
</details>

✅ **Check:** buy a ticket (*Pay with a test card*) — the success screen and *My tickets* show a QR code.

## Exercise 2 ⭐⭐ — Confirm the payment with the fingerprint (BiometricPrompt)
📄 `composeApp/.../security/BiometricAuthenticator.kt` → `authenticate()`

Show the system biometric dialog and call `onSuccess()` / `onError(message)`.
`MainActivity` is already a `FragmentActivity` (required by `BiometricPrompt`).

<details><summary>Hint</summary>

```kotlin
val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
    object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onError(errString.toString())
    })
val info = BiometricPrompt.PromptInfo.Builder()
    .setTitle(title).setSubtitle(subtitle).setAllowedAuthenticators(authenticators)
    .apply { if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) setNegativeButtonText("Cancel") }
    .build()
prompt.authenticate(info)
```
</details>

✅ **Check:** on the emulator, *Settings › Security* → set a PIN, then add a fingerprint
(*Extended controls › Fingerprint › Touch sensor*). Paying now opens the fingerprint dialog.

## Exercise 3 ⭐⭐ — Secure, single-use tickets
A QR code can be photographed and copied. Two protections:

**3a.** 📄 `shared/.../security/TicketSigner.kt` → `verify()` — a code is `CPASS1:<ticketId>:<eventId>:<signature>`.
Recompute the HMAC signature and reject any code that does not match (forged tickets).

**3b.** 📄 `shared/.../repository/TicketingRepository.kt` → `validate()` — at the entrance a valid ticket
becomes `USED`; scanning it again must return `AlreadyUsed`. Also handle `WrongEvent` and `Refunded`.

✅ **Check:** `./gradlew :shared:jvmTest` → all `ScanTest` tests are green. Then in the app: buy a ticket as Alice,
take a screenshot, sign in as **bde@isep.fr**, open *Scanner*, select the event, *From a picture* → ✅ **Welcome!**
Scan it again → ⛔ **Already used**.

## Exercise 4 ⭐⭐ — Pay with Google Pay
📄 `composeApp/.../payment/GooglePay.kt` → `paymentDataRequest()`

Build the JSON request (`allowedPaymentMethods`, `transactionInfo`, `merchantInfo`).
Everything else (readiness check, button, result handling) is already wired in `CheckoutScreens.kt`.

<details><summary>Hint</summary>

```kotlin
val transactionInfo = JSONObject()
    .put("totalPrice", "${totalCents / 100}.${(totalCents % 100).toString().padStart(2, '0')}")
    .put("totalPriceStatus", "FINAL").put("currencyCode", "EUR").put("countryCode", "FR")
return JSONObject(baseRequest.toString())
    .put("allowedPaymentMethods", JSONArray().put(cardPaymentMethod()))
    .put("transactionInfo", transactionInfo)
    .put("merchantInfo", JSONObject().put("merchantName", "CampusPass (demo)"))
```
</details>

✅ **Check:** sign in to a Google account on the emulator, tap **G Pay**: the Google Pay sheet shows
the total and test cards. Pick one — your tickets are created with the Google Pay token.

## Exercise 5 ⭐⭐⭐ (bonus) — PDF e-ticket
📄 `composeApp/.../pdf/TicketPdfGenerator.kt` → `drawTicket()` — draw the title, date, venue, the QR code
and the holder on the PDF page with `Canvas`. ✅ *Download PDF ticket* opens your ticket.

## Exercise 6 ⭐ (bonus) — Refund deadline
📄 `shared/.../repository/TicketingRepository.kt` → `refund()` — refuse refunds less than 48 h before the
event. ✅ `RefundTest.refundIsClosedLessThan48hBeforeTheEvent` is green.

---

## Architecture reminder

```
shared/        domain (Event, Ticket, Transaction, ScanResult) + repositories with all business rules + tests
composeApp/    Compose UI, ViewModels (StateFlow), Google Pay, BiometricPrompt, ZXing, PDF
```
Business rules live in `shared` and are unit-tested; Android services stay in `composeApp`.

## Going further
- Send the Google Pay token to a server and charge it with Stripe / Adyen (`PAYMENT_GATEWAY` tokenization).
- Replace the shared HMAC secret by an asymmetric signature (the scanner only holds the public key).
- Add the ticket to **Google Wallet** with the Wallet API.
