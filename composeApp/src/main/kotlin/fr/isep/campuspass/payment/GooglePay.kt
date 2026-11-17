package fr.isep.campuspass.payment

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds the JSON requests of the Google Pay API (version 2).
 * Docs: https://developers.google.com/pay/api/android/guides/tutorial
 */
object GooglePayConfig {
    private val baseRequest = JSONObject().put("apiVersion", 2).put("apiVersionMinor", 0)

    private val allowedCardNetworks = JSONArray(listOf("AMEX", "DISCOVER", "MASTERCARD", "VISA"))
    private val allowedAuthMethods = JSONArray(listOf("PAN_ONLY", "CRYPTOGRAM_3DS"))

    /**
     * Google Pay never gives the card number to the app: it returns an encrypted token for a
     * payment service provider (Stripe, Adyen…). "example" is Google's test gateway.
     */
    private fun tokenizationSpecification() = JSONObject()
        .put("type", "PAYMENT_GATEWAY")
        .put(
            "parameters",
            JSONObject().put("gateway", "example").put("gatewayMerchantId", "exampleGatewayMerchantId"),
        )

    private fun baseCardPaymentMethod() = JSONObject()
        .put("type", "CARD")
        .put(
            "parameters",
            JSONObject().put("allowedAuthMethods", allowedAuthMethods).put("allowedCardNetworks", allowedCardNetworks),
        )

    private fun cardPaymentMethod() = baseCardPaymentMethod().put("tokenizationSpecification", tokenizationSpecification())

    /** Used by the Google Pay button to render the user's card. */
    val allowedPaymentMethods: String get() = JSONArray().put(cardPaymentMethod()).toString()

    fun isReadyToPayRequest(): JSONObject =
        JSONObject(baseRequest.toString()).put("allowedPaymentMethods", JSONArray().put(baseCardPaymentMethod()))

    /** Full request for an amount in euro cents. */
    fun paymentDataRequest(totalCents: Long): JSONObject {
        val transactionInfo = JSONObject()
            .put("totalPrice", "${totalCents / 100}.${(totalCents % 100).toString().padStart(2, '0')}")
            .put("totalPriceStatus", "FINAL")
            .put("currencyCode", "EUR")
            .put("countryCode", "FR")
        return JSONObject(baseRequest.toString())
            .put("allowedPaymentMethods", JSONArray().put(cardPaymentMethod()))
            .put("transactionInfo", transactionInfo)
            .put("merchantInfo", JSONObject().put("merchantName", "CampusPass (demo)"))
    }

    /** Extracts the payment token that would be sent to the payment provider's server. */
    fun extractToken(paymentData: PaymentData): String =
        JSONObject(paymentData.toJson())
            .getJSONObject("paymentMethodData")
            .getJSONObject("tokenizationData")
            .getString("token")
}

/** Thin wrapper around the Google Pay [PaymentsClient], in TEST environment (no real money). */
class GooglePayClient(context: Context) {
    private val client: PaymentsClient = Wallet.getPaymentsClient(
        context,
        Wallet.WalletOptions.Builder().setEnvironment(WalletConstants.ENVIRONMENT_TEST).build(),
    )

    /** False on devices without Google Play services or without a signed-in Google account. */
    suspend fun isReadyToPay(): Boolean = try {
        client.isReadyToPay(IsReadyToPayRequest.fromJson(GooglePayConfig.isReadyToPayRequest().toString())).await() == true
    } catch (e: Exception) {
        false
    }

    fun loadPaymentData(totalCents: Long): Task<PaymentData> =
        client.loadPaymentData(PaymentDataRequest.fromJson(GooglePayConfig.paymentDataRequest(totalCents).toString()))
}
