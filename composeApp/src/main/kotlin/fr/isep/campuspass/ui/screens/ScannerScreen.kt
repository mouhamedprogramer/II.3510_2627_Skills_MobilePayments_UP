package fr.isep.campuspass.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import fr.isep.campuspass.domain.ScanResult
import fr.isep.campuspass.qr.QrCodes
import fr.isep.campuspass.ui.components.LocalSnackbar
import fr.isep.campuspass.ui.components.PrimaryButton
import fr.isep.campuspass.ui.components.SecondaryButton
import fr.isep.campuspass.ui.formatInstant
import fr.isep.campuspass.ui.formatTime
import fr.isep.campuspass.ui.theme.Brand
import fr.isep.campuspass.viewmodel.Factories
import fr.isep.campuspass.viewmodel.ScannerViewModel

/** Entrance control for organizers: scan the QR code of a ticket, accept it once, refuse it afterwards. */
@Composable
fun ScannerScreen(vm: ScannerViewModel = viewModel(factory = Factories.scanner)) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    var manualEntry by remember { mutableStateOf(false) }

    val cameraScanner = rememberLauncherForActivityResult(ScanContract()) { r -> r.contents?.let(vm::validate) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) QrCodes.decode(context, uri)?.let(vm::validate) ?: snackbar("No QR code found in this picture.")
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Entrance scanner", style = MaterialTheme.typography.displaySmall, modifier = Modifier.statusBarsPadding().padding(top = 8.dp))
        Text("Choose the event, then scan each ticket at the door.", color = Brand.Muted)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ui.events, key = { it.id }) { e ->
                val selected = e.id == ui.selected?.id
                Surface(onClick = { vm.select(e.id) }, shape = CircleShape, color = if (selected) Brand.Navy else Color.White, shadowElevation = 1.dp) {
                    Text(e.title, style = MaterialTheme.typography.labelMedium, color = if (selected) Color.White else Brand.Navy, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
                }
            }
        }
        ui.selected?.let { e ->
            Surface(shape = RoundedCornerShape(22.dp), color = Brand.Navy) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(e.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("Doors open at ${formatTime(e.startsAt)} · ${e.venue}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${ui.checkedIn}", style = MaterialTheme.typography.displaySmall, color = Brand.Yellow)
                        Text("  checked in  /  ${ui.sold} tickets sold", color = Color.White, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { if (ui.sold == 0) 0f else ui.checkedIn.toFloat() / ui.sold },
                        color = Brand.Yellow, trackColor = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            Surface(
                onClick = {
                    cameraScanner.launch(
                        ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setPrompt("Scan a CampusPass ticket")
                            .setBeepEnabled(true).setOrientationLocked(false),
                    )
                },
                shape = CircleShape, color = Brand.Coral, shadowElevation = 10.dp, modifier = Modifier.size(180.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Rounded.QrCodeScanner, null, tint = Color.White, modifier = Modifier.size(64.dp))
                    Text("Scan ticket", style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("From a picture", { imagePicker.launch("image/*") }, Modifier.weight(1f), icon = Icons.Rounded.Image)
            SecondaryButton("Enter code", { manualEntry = true }, Modifier.weight(1f), icon = Icons.Rounded.Keyboard)
        }
    }

    result?.let { ScanResultOverlay(it, onNext = vm::dismiss) }

    if (manualEntry) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { manualEntry = false },
            title = { Text("Enter the ticket code") },
            text = { OutlinedTextField(code, { code = it }, placeholder = { Text("CPASS1:TK-…") }, singleLine = true) },
            confirmButton = { Button(onClick = { manualEntry = false; vm.validate(code) }) { Text("Validate") } },
            dismissButton = { TextButton(onClick = { manualEntry = false }) { Text("Cancel") } },
            containerColor = Color.White,
        )
    }
}

/** Full-screen green / red answer, readable from a distance by the door staff. */
@Composable
private fun ScanResultOverlay(result: ScanResult, onNext: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(result) { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
    val (color, icon, title, subtitle) = when (result) {
        is ScanResult.Valid -> Quad(Brand.Mint, Icons.Rounded.CheckCircle, "Welcome!", "${result.ticket.holderName}\n${result.event.title}")
        is ScanResult.AlreadyUsed -> Quad(Brand.Red, Icons.Rounded.Block, "Already used", "This ticket was scanned ${formatInstant(result.scannedAt)}")
        is ScanResult.Refunded -> Quad(Brand.Red, Icons.Rounded.MoneyOff, "Refunded ticket", "This ticket was refunded and is no longer valid.")
        is ScanResult.WrongEvent -> Quad(Color(0xFFF59F00), Icons.Rounded.EventBusy, "Wrong event", "This ticket is for ${result.ticketEvent?.title ?: "another event"}.")
        is ScanResult.Invalid -> Quad(Brand.Red, Icons.Rounded.GppBad, "Invalid ticket", result.reason)
    }
    AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn(initialScale = 0.9f), exit = fadeOut()) {
        Box(Modifier.fillMaxSize().background(color), contentAlignment = Alignment.Center) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(140.dp))
                Spacer(Modifier.height(16.dp))
                Text(title, style = MaterialTheme.typography.displaySmall, color = Color.White, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(subtitle, style = MaterialTheme.typography.titleLarge, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
                Spacer(Modifier.height(40.dp))
                PrimaryButton("Scan next ticket", onNext, Modifier.fillMaxWidth(), icon = Icons.Rounded.QrCodeScanner)
            }
        }
    }
}

private data class Quad(val color: Color, val icon: androidx.compose.ui.graphics.vector.ImageVector, val title: String, val subtitle: String)
