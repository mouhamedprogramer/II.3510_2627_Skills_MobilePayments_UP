package fr.isep.campuspass.ui.screens

import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.isep.campuspass.domain.TicketStatus
import fr.isep.campuspass.pdf.TicketPdfGenerator
import fr.isep.campuspass.ui.components.*
import fr.isep.campuspass.ui.findActivity
import fr.isep.campuspass.ui.formatDateTime
import fr.isep.campuspass.ui.formatInstant
import fr.isep.campuspass.ui.theme.Brand
import fr.isep.campuspass.viewmodel.Factories
import fr.isep.campuspass.viewmodel.MyTicketsViewModel
import fr.isep.campuspass.viewmodel.TicketDetailViewModel
import fr.isep.campuspass.viewmodel.TicketItem

@Composable
fun MyTicketsScreen(onOpen: (String) -> Unit, onBrowse: () -> Unit, vm: MyTicketsViewModel = viewModel(factory = Factories.myTickets)) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val list = if (tab == 0) ui.upcoming else ui.past

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("My tickets", style = MaterialTheme.typography.displaySmall, modifier = Modifier.statusBarsPadding().padding(top = 8.dp))
            Text("Show the QR code at the entrance.", style = MaterialTheme.typography.bodyMedium, color = Brand.Muted)
        }
        item {
            Row(Modifier.clip(CircleShape).background(Color(0xFFEFEDF7)).padding(4.dp)) {
                listOf("Upcoming (${ui.upcoming.size})", "Used & refunded (${ui.past.size})").forEachIndexed { i, label ->
                    Surface(onClick = { tab = i }, shape = CircleShape, color = if (tab == i) Brand.Navy else Color.Transparent, modifier = Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = if (tab == i) Color.White else Brand.Navy, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp))
                    }
                }
            }
        }
        if (list.isEmpty()) {
            item {
                EmptyState(Icons.Rounded.ConfirmationNumber, if (tab == 0) "No upcoming tickets" else "Nothing here yet", "Your tickets appear here right after payment.") {
                    if (tab == 0) PrimaryButton("Browse events", onBrowse, coral = true)
                }
            }
        } else {
            items(list, key = { it.ticket.id }) { TicketCard(it) { onOpen(it.ticket.id) } }
        }
    }
}

@Composable
private fun TicketCard(item: TicketItem, onClick: () -> Unit) {
    val dimmed = item.ticket.status != TicketStatus.VALID
    Surface(onClick = onClick, shape = TicketShape(12.dp, 120.dp, 22.dp), color = Color.White, shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth().alpha(if (dimmed) 0.7f else 1f)) {
        Column {
            Box(Modifier.fillMaxWidth().height(120.dp)) {
                Poster(item.event.posterUrl, item.event.category, Modifier.matchParentSize())
                Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Brand.Navy.copy(alpha = 0.9f), Color.Transparent))))
                Column(Modifier.align(Alignment.CenterStart).padding(18.dp).fillMaxWidth(0.7f)) {
                    Text(item.event.title, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(formatDateTime(item.event.startsAt), style = MaterialTheme.typography.bodySmall, color = Brand.Yellow)
                }
            }
            DashedDivider()
            Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.event.venue, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${item.ticket.holderName} · ${item.ticket.id}", style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
                }
                StatusBadge(item.ticket.status)
                Icon(Icons.Rounded.QrCode2, null, tint = Brand.Navy, modifier = Modifier.padding(start = 10.dp))
            }
        }
    }
}

@Composable
private fun DashedDivider() {
    Canvas(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 18.dp)) {
        drawLine(Brand.Line, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f)))
    }
}

@Composable
fun TicketDetailScreen(
    ticketId: String,
    onBack: () -> Unit,
    vm: TicketDetailViewModel = viewModel(key = "ticket$ticketId", factory = Factories.ticketDetail(ticketId)),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val d = ui ?: return
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    var askRefund by remember { mutableStateOf(false) }

    // Full brightness while a valid QR code is displayed, so that the scanner reads it easily.
    if (d.ticket.status == TicketStatus.VALID) {
        DisposableEffect(Unit) {
            val window = context.findActivity().window
            window.attributes = window.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL }
            onDispose { window.attributes = window.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE } }
        }
    }

    GradientBox(Brand.Night, Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
            Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White) }
                Text("Your ticket", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
            Surface(shape = TicketShape(16.dp, 150.dp, 26.dp), color = Color.White, modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                Column {
                    Box(Modifier.fillMaxWidth().height(150.dp)) {
                        Poster(d.event.posterUrl, d.event.category, Modifier.matchParentSize())
                        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))))
                        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                            Text(d.event.category.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Brand.Yellow)
                            Text(d.event.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                        }
                    }
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(Modifier.fillMaxWidth()) {
                            Field("DATE", formatDateTime(d.event.startsAt), Modifier.weight(1f))
                            StatusBadge(d.ticket.status)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Field("VENUE", d.event.venue, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Field("HOLDER", d.ticket.holderName, Modifier.weight(1f))
                            Field("ADMISSION", "General", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(20.dp))
                        Box(contentAlignment = Alignment.Center) {
                            QrImage(d.ticket.code, Modifier.size(230.dp).alpha(if (d.ticket.status == TicketStatus.VALID) 1f else 0.15f))
                            when (d.ticket.status) {
                                TicketStatus.USED -> Stamp("USED", d.ticket.scannedAt?.let { "Scanned ${formatInstant(it)}" })
                                TicketStatus.REFUNDED -> Stamp("REFUNDED", "This ticket is no longer valid")
                                TicketStatus.VALID -> Unit
                            }
                        }
                        Text(d.ticket.id, style = MaterialTheme.typography.labelSmall, color = Brand.Muted)
                        if (d.orderTickets.size > 1) {
                            Text("Order of ${d.orderTickets.size} tickets — each one has its own QR code.", style = MaterialTheme.typography.bodySmall, color = Brand.Muted, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Download PDF ticket", {
                    val tx = d.transaction ?: return@PrimaryButton
                    TicketPdfGenerator.open(context, TicketPdfGenerator.create(context, d.orderTickets, d.event, tx, vm.buyerName))
                }, Modifier.fillMaxWidth(), icon = Icons.Rounded.PictureAsPdf, coral = true)
                if (d.ticket.status == TicketStatus.VALID) {
                    SecondaryButton("Request a refund", { askRefund = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Undo, danger = true)
                }
            }
        }
    }

    if (askRefund) {
        ConfirmDialog(
            title = "Refund this order?",
            text = "All ${d.orderTickets.size} ticket(s) of order ${d.transaction?.id} will be cancelled and the payment refunded. Refunds close 48 h before the event.",
            confirm = "Refund",
            icon = Icons.Rounded.Undo,
            danger = true,
            onConfirm = {
                askRefund = false
                snackbar(vm.refund() ?: "Order refunded — your seats are released.")
            },
            onDismiss = { askRefund = false },
        )
    }
}

@Composable
private fun Field(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Brand.Muted)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun Stamp(text: String, subtitle: String?) {
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White).padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.headlineSmall, color = Brand.Red)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Brand.Muted, textAlign = TextAlign.Center)
    }
}
