package fr.isep.campuspass.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.contract.TaskResultContracts
import com.google.pay.button.ButtonTheme
import com.google.pay.button.ButtonType
import com.google.pay.button.PayButton
import fr.isep.campuspass.domain.PaymentMethod
import fr.isep.campuspass.domain.PaymentReceipt
import fr.isep.campuspass.domain.formatEuros
import fr.isep.campuspass.payment.GooglePayConfig
import fr.isep.campuspass.pdf.TicketPdfGenerator
import fr.isep.campuspass.security.BiometricAuthenticator
import fr.isep.campuspass.ui.components.*
import fr.isep.campuspass.ui.findActivity
import fr.isep.campuspass.ui.formatDateTime
import fr.isep.campuspass.ui.theme.Brand
import fr.isep.campuspass.viewmodel.CheckoutStep
import fr.isep.campuspass.viewmodel.CheckoutViewModel
import fr.isep.campuspass.viewmodel.Factories
import fr.isep.campuspass.viewmodel.OrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    eventId: Int,
    quantity: Int,
    onBack: () -> Unit,
    onPaid: (String) -> Unit,
    vm: CheckoutViewModel = viewModel(key = "checkout$eventId-$quantity", factory = Factories.checkout(eventId, quantity)),
) {
    val event = vm.event ?: return
    val gpayReady by vm.googlePayReady.collectAsStateWithLifecycle()
    val step by vm.step.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity() as FragmentActivity
    val snackbar = LocalSnackbar.current
    var showTestCard by remember { mutableStateOf(false) }
    var pendingWithoutBiometrics by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(step) { (step as? CheckoutStep.Done)?.let { onPaid(it.transactionId) } }

    // Google Pay sheet result
    val googlePayLauncher = rememberLauncherForActivityResult(TaskResultContracts.GetPaymentDataResult()) { result ->
        when (result.status.statusCode) {
            CommonStatusCodes.SUCCESS -> result.result?.let {
                vm.completePurchase(PaymentReceipt(PaymentMethod.GOOGLE_PAY, GooglePayConfig.extractToken(it)))
            }
            CommonStatusCodes.CANCELED -> snackbar("Payment cancelled")
            else -> vm.fail(result.status.statusMessage ?: "Google Pay error")
        }
    }

    /** Step 1 of every payment: biometric confirmation. */
    fun confirmThen(action: () -> Unit) {
        val biometrics = BiometricAuthenticator(activity)
        if (biometrics.availability() == BiometricAuthenticator.Availability.AVAILABLE) {
            biometrics.authenticate(
                title = "Confirm your payment",
                subtitle = "${vm.totalCents.formatEuros()} · ${event.title}",
                onSuccess = action,
                onError = { snackbar(it) },
            )
        } else {
            pendingWithoutBiometrics = action
        }
    }

    Column(Modifier.fillMaxSize()) {
        BackBar("Checkout", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Order summary
            Surface(shape = RoundedCornerShape(22.dp), color = Color.White, shadowElevation = 2.dp) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Poster(event.posterUrl, event.category, Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)))
                        Column(Modifier.padding(start = 14.dp)) {
                            Text(event.title, style = MaterialTheme.typography.titleMedium)
                            Text(formatDateTime(event.startsAt), style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
                            Text(event.venue, style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 14.dp), color = Brand.Line)
                    SummaryRow("Tickets", "${vm.quantity} × ${event.priceCents.formatEuros()}")
                    SummaryRow("Service fee", "€0.00")
                    Spacer(Modifier.height(6.dp))
                    Row {
                        Text("Total", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        Text(vm.totalCents.formatEuros(), style = MaterialTheme.typography.headlineSmall, color = Brand.Coral)
                    }
                }
            }

            Text("Pay with", style = MaterialTheme.typography.titleLarge)
            when (gpayReady) {
                null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("  Checking Google Pay…", color = Brand.Muted)
                }
                true -> PayButton(
                    onClick = {
                        confirmThen {
                            // The Task must be completed before it is handed to the launcher.
                            vm.googlePay.loadPaymentData(vm.totalCents).addOnCompleteListener(googlePayLauncher::launch)
                        }
                    },
                    allowedPaymentMethods = GooglePayConfig.allowedPaymentMethods,
                    type = ButtonType.Pay,
                    theme = ButtonTheme.Dark,
                    radius = 16.dp,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                )
                false -> Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFFFF4DB)) {
                    Row(Modifier.padding(14.dp)) {
                        Icon(Icons.Rounded.Info, null, tint = Color(0xFF8A5A00))
                        Text(
                            "  Google Pay is not available on this device (Google Play services or Google account missing). Use a test card below.",
                            style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A5A00),
                        )
                    }
                }
            }
            SecondaryButton("Pay with a test card", { showTestCard = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.CreditCard)

            (step as? CheckoutStep.Failed)?.let { f ->
                Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFFFE3E4)) {
                    Text(f.message, color = Brand.Red, modifier = Modifier.fillMaxWidth().padding(14.dp))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, null, tint = Brand.Muted, modifier = Modifier.size(16.dp))
                Text(
                    "  Test environment: no real money is charged. Your card number never reaches CampusPass — Google Pay only returns an encrypted token.",
                    style = MaterialTheme.typography.bodySmall, color = Brand.Muted,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (step is CheckoutStep.Processing) ProcessingOverlay()

    if (showTestCard) {
        TestCardSheet(
            amount = vm.totalCents.formatEuros(),
            onDismiss = { showTestCard = false },
            onPay = { showTestCard = false; confirmThen { vm.payWithTestCard() } },
        )
    }

    pendingWithoutBiometrics?.let { action ->
        ConfirmDialog(
            title = "No biometrics on this device",
            text = "No fingerprint, face or screen lock is configured, so the payment cannot be confirmed biometrically. Continue in demo mode?",
            confirm = "Continue",
            icon = Icons.Rounded.Fingerprint,
            onConfirm = { pendingWithoutBiometrics = null; action() },
            onDismiss = { pendingWithoutBiometrics = null },
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text(label, color = Brand.Muted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun ProcessingOverlay() {
    Box(Modifier.fillMaxSize().background(Brand.Navy.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Brand.Yellow, strokeWidth = 4.dp, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(20.dp))
            Text("Confirming your payment…", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Generating your signed tickets", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

/** Fallback payment form pre-filled with a public test card number (works without Google Pay). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestCardSheet(amount: String, onDismiss: () -> Unit, onPay: () -> Unit) {
    var number by remember { mutableStateOf("4242 4242 4242 4242") }
    var expiry by remember { mutableStateOf("12/28") }
    var cvc by remember { mutableStateOf("123") }
    val valid = number.filter { it.isDigit() }.length == 16 && expiry.length == 5 && cvc.length == 3
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Test card", style = MaterialTheme.typography.headlineSmall)
            GradientBox(Brand.Night, Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(22.dp))) {
                Column(Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row {
                        Text("CampusPass", color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.Contactless, null, tint = Brand.Yellow)
                    }
                    Text(number.ifBlank { "•••• •••• •••• ••••" }, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    Row {
                        Text("TEST CARD", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        Text(expiry, color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            OutlinedTextField(number, { number = it.take(19) }, label = { Text("Card number") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(expiry, { expiry = it.take(5) }, label = { Text("Expiry") }, singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f))
                OutlinedTextField(cvc, { cvc = it.take(3) }, label = { Text("CVC") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f))
            }
            PrimaryButton("Pay $amount", onPay, Modifier.fillMaxWidth(), enabled = valid, icon = Icons.Rounded.Fingerprint, coral = true)
            Text("4242 4242 4242 4242 is a public test number: no real payment is made.", style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
        }
    }
}

@Composable
fun PaymentSuccessScreen(
    transactionId: String,
    onTickets: () -> Unit,
    onHome: () -> Unit,
    vm: OrderViewModel = viewModel(key = "order$transactionId", factory = Factories.order(transactionId)),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val o = ui ?: return
    val context = LocalContext.current
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(if (appeared) 1f else 0.2f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "check")

    GradientBox(Brand.Night, Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Box(Modifier.size(110.dp).scale(scale).clip(CircleShape).background(Brand.Mint), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(64.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("You're in!", style = MaterialTheme.typography.displaySmall, color = Color.White)
            Text(
                "${o.tickets.size} ticket${if (o.tickets.size > 1) "s" else ""} for ${o.event.title}",
                style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Surface(shape = TicketShape(14.dp, 96.dp, 22.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(o.event.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Text(formatDateTime(o.event.startsAt), style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
                    Spacer(Modifier.height(32.dp))
                    QrImage(o.tickets.first().code, Modifier.size(170.dp))
                    Text(o.tickets.first().id, style = MaterialTheme.typography.labelSmall, color = Brand.Muted)
                    Spacer(Modifier.height(12.dp))
                    Text("Paid ${o.transaction.amountCents.formatEuros()} with ${o.transaction.method.label}", style = MaterialTheme.typography.titleSmall)
                    Text("Order ${o.transaction.id}", style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
                }
            }
            Spacer(Modifier.height(24.dp))
            PrimaryButton("View my tickets", onTickets, Modifier.fillMaxWidth(), icon = Icons.Rounded.ConfirmationNumber, coral = true)
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Download PDF ticket", {
                TicketPdfGenerator.open(context, TicketPdfGenerator.create(context, o.tickets, o.event, o.transaction, vm.buyerName))
            }, Modifier.fillMaxWidth(), icon = Icons.Rounded.PictureAsPdf)
            TextButton(onClick = onHome) { Text("Back to events", color = Color.White) }
        }
    }
}
