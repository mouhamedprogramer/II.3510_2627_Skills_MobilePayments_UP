package fr.isep.campuspass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.isep.campuspass.AppContainer
import fr.isep.campuspass.domain.Role
import fr.isep.campuspass.domain.TransactionStatus
import fr.isep.campuspass.domain.formatEuros
import fr.isep.campuspass.pdf.TicketPdfGenerator
import fr.isep.campuspass.ui.components.*
import fr.isep.campuspass.ui.formatInstant
import fr.isep.campuspass.ui.theme.Brand
import fr.isep.campuspass.viewmodel.Factories
import fr.isep.campuspass.viewmodel.LoginViewModel
import fr.isep.campuspass.viewmodel.OrderItem
import fr.isep.campuspass.viewmodel.ProfileViewModel

@Composable
fun LoginScreen(onBack: (() -> Unit)?, vm: LoginViewModel = viewModel(factory = Factories.login)) {
    val error by vm.error.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbar.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()) {
        GradientBox(Brand.Night, Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))) {
            Column(Modifier.statusBarsPadding().padding(20.dp)) {
                if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White) }
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Brand.Sunset), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ConfirmationNumber, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Welcome to\nCampusPass", style = MaterialTheme.typography.displaySmall, color = Color.White)
                Text("Sign in to buy tickets and get into every campus event.", color = Color.White.copy(alpha = 0.75f))
                Spacer(Modifier.height(8.dp))
            }
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(email, { email = it }, label = { Text("E-mail") }, leadingIcon = { Icon(Icons.Rounded.Email, null) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(password, { password = it }, label = { Text("Password") }, leadingIcon = { Icon(Icons.Rounded.Lock, null) }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = Brand.Red) }
            PrimaryButton("Sign in", { vm.login(email, password)?.let { snackbar("Welcome, ${it.name.substringBefore(' ')}!") } }, Modifier.fillMaxWidth(), coral = true)
            Text("DEMO ACCOUNTS", style = MaterialTheme.typography.labelSmall, color = Brand.Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Triple("Alice · student", "alice@isep.fr", "alice123"), Triple("Bob · student", "bob@isep.fr", "bob123"), Triple("BDE · organizer", "bde@isep.fr", "bde123"))
                    .forEach { (label, e, p) ->
                        Surface(onClick = { email = e; password = p }, shape = CircleShape, color = Color(0xFFE8E6FF)) {
                            Text(label, style = MaterialTheme.typography.labelMedium, color = Brand.Navy, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                        }
                    }
            }
        }
    }
}

@Composable
fun ProfileScreen(vm: ProfileViewModel = viewModel(factory = Factories.profile)) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val p = ui ?: return
    val snackbar = LocalSnackbar.current
    val context = LocalContext.current
    var askLogout by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        GradientBox(Brand.Night, Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))) {
            Column(Modifier.statusBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().wrapContentWidth().size(84.dp).clip(CircleShape).background(Brand.Yellow), contentAlignment = Alignment.Center) {
                    Text(p.user.name.split(" ").take(2).joinToString("") { it.take(1) }, style = MaterialTheme.typography.headlineMedium, color = Brand.Navy)
                }
                Spacer(Modifier.height(12.dp))
                Text(p.user.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Text(p.user.email, color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.height(8.dp))
                Box(Modifier.clip(CircleShape).background(Brand.Coral).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(if (p.user.role == Role.ORGANIZER) "ORGANIZER" else "STUDENT", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (p.user.role == Role.STUDENT) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Stat("${p.ticketCount}", "Tickets", Modifier.weight(1f))
                    Stat(p.spentCents.formatEuros(), "Spent", Modifier.weight(1f))
                    Stat("${p.orders.size}", "Orders", Modifier.weight(1f))
                }
                Text("Purchase history", style = MaterialTheme.typography.titleLarge)
                if (p.orders.isEmpty()) {
                    Text("No purchase yet.", color = Brand.Muted)
                } else {
                    p.orders.forEach { o ->
                        OrderRow(o) {
                            val tickets = AppContainer.ticketing.tickets.value.filter { it.transactionId == o.transaction.id }
                            TicketPdfGenerator.open(context, TicketPdfGenerator.create(context, tickets, o.event, o.transaction, p.user.name))
                        }
                    }
                }
            } else {
                Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFFE8E6FF)) {
                    Row(Modifier.padding(16.dp)) {
                        Icon(Icons.Rounded.QrCodeScanner, null, tint = Brand.Navy)
                        Text("  Organizer account: use the Scanner tab to check tickets at the entrance.", color = Brand.Navy)
                    }
                }
            }
            SecondaryButton("Sign out", { askLogout = true }, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.Logout, danger = true)
        }
    }

    if (askLogout) {
        ConfirmDialog("Sign out?", "You will need to sign in again to buy or show your tickets.", "Sign out", Icons.AutoMirrored.Rounded.Logout, danger = true,
            onConfirm = { askLogout = false; vm.logout(); snackbar("Signed out") }, onDismiss = { askLogout = false })
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 1.dp) {
        Column(Modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = Brand.Coral)
            Text(label, style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
        }
    }
}

@Composable
private fun OrderRow(o: OrderItem, onReceipt: () -> Unit) {
    val refunded = o.transaction.status == TransactionStatus.REFUNDED
    Surface(onClick = onReceipt, shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Poster(o.event.posterUrl, o.event.category, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(o.event.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text("${o.transaction.quantity} ticket(s) · ${o.transaction.method.label}", style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
                Text(formatInstant(o.transaction.createdAt), style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(o.transaction.amountCents.formatEuros(), style = MaterialTheme.typography.titleMedium, color = if (refunded) Brand.Muted else Brand.Navy)
                Text(if (refunded) "REFUNDED" else "PAID", style = MaterialTheme.typography.labelSmall, color = if (refunded) Brand.Red else Brand.Mint)
                Icon(Icons.Rounded.PictureAsPdf, "Receipt", tint = Brand.Coral, modifier = Modifier.size(18.dp))
            }
        }
    }
}
