package fr.isep.campuspass.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import fr.isep.campuspass.domain.Event
import fr.isep.campuspass.domain.EventCategory
import fr.isep.campuspass.domain.TicketStatus
import fr.isep.campuspass.domain.formatEuros
import fr.isep.campuspass.qr.QrCodes
import fr.isep.campuspass.ui.formatTime
import fr.isep.campuspass.ui.monthShort
import fr.isep.campuspass.ui.theme.Brand
import fr.isep.campuspass.ui.theme.icon

/** `LocalSnackbar.current("Ticket refunded")` from any screen. */
val LocalSnackbar = staticCompositionLocalOf<(String) -> Unit> { {} }

/** Event poster with a gradient + category icon shown while the photo loads (or offline). */
@Composable
fun Poster(url: String?, category: EventCategory, modifier: Modifier = Modifier) {
    Box(modifier.background(Brand.Night), contentAlignment = Alignment.Center) {
        Icon(category.icon(), null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
        if (url != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
fun DateBadge(event: Event, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(Color.White).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("${event.startsAt.dayOfMonth}", style = MaterialTheme.typography.titleLarge, color = Brand.Navy)
        Text(monthShort(event.startsAt), style = MaterialTheme.typography.labelSmall, color = Brand.Coral)
    }
}

@Composable
fun PricePill(cents: Long, modifier: Modifier = Modifier) {
    Box(modifier.clip(CircleShape).background(Brand.Yellow).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(cents.formatEuros(), style = MaterialTheme.typography.labelLarge, color = Brand.Navy)
    }
}

@Composable
fun SeatsLine(seatsLeft: Int, modifier: Modifier = Modifier) {
    val (text, color) = when {
        seatsLeft == 0 -> "Sold out" to Brand.Red
        seatsLeft <= 10 -> "Only $seatsLeft seats left" to Brand.Coral
        else -> "$seatsLeft seats left" to Brand.Mint
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
fun EventCard(event: Event, seatsLeft: Int, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(22.dp), color = Brand.Card, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(Modifier.fillMaxWidth().height(170.dp)) {
                Poster(event.posterUrl, event.category, Modifier.matchParentSize())
                DateBadge(event, Modifier.align(Alignment.TopStart).padding(12.dp))
                PricePill(event.priceCents, Modifier.align(Alignment.TopEnd).padding(12.dp))
                if (seatsLeft == 0) {
                    Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                        Text("SOLD OUT", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    }
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(event.category.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Brand.Coral)
                Text(event.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Place, null, tint = Brand.Muted, modifier = Modifier.size(16.dp))
                    Text(" ${event.venue} · ${formatTime(event.startsAt)}", style = MaterialTheme.typography.bodySmall, color = Brand.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                SeatsLine(seatsLeft, Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** Ticket outline with two half-circle notches, like a real paper ticket. */
class TicketShape(private val notchRadius: Dp, private val notchY: Dp, private val corner: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { notchRadius.toPx() }
        val y = with(density) { notchY.toPx() }
        val c = with(density) { corner.toPx() }
        val body = Path().apply { addRoundRect(RoundRect(Rect(0f, 0f, size.width, size.height), c, c)) }
        val notches = Path().apply {
            addOval(Rect(-r, y - r, r, y + r))
            addOval(Rect(size.width - r, y - r, size.width + r, y + r))
        }
        return Outline.Generic(Path.combine(PathOperation.Difference, body, notches))
    }
}

@Composable
fun StatusBadge(status: TicketStatus, modifier: Modifier = Modifier) {
    val (text, bg, fg) = when (status) {
        TicketStatus.VALID -> Triple("VALID", Color(0xFFDDF5EC), Brand.Mint)
        TicketStatus.USED -> Triple("USED", Color(0xFFEFEDF7), Brand.Muted)
        TicketStatus.REFUNDED -> Triple("REFUNDED", Color(0xFFFFE3E4), Brand.Red)
    }
    Box(modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = fg)
    }
}

@Composable
fun QrImage(content: String, modifier: Modifier = Modifier) {
    val bitmap = remember(content) { QrCodes.generate(content, 720).asImageBitmap() }
    Image(bitmap, contentDescription = "Ticket QR code", modifier = modifier)
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null, coral: Boolean = false) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (coral) Brand.Coral else Brand.Navy, contentColor = Color.White,
            disabledContainerColor = Color(0xFFE6E4F0), disabledContentColor = Brand.Muted,
        ),
        modifier = modifier.height(56.dp),
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, danger: Boolean = false) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (danger) Brand.Red.copy(alpha = 0.4f) else Brand.Line),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = if (danger) Brand.Red else Brand.Navy),
        modifier = modifier.height(52.dp),
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun BackBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Brand.Navy) }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(88.dp).clip(CircleShape).background(Color(0xFFE8E6FF)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Brand.Navy, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Brand.Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        action()
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    icon: ImageVector,
    danger: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon, null, tint = if (danger) Brand.Red else Brand.Navy) },
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = { Text(text, color = Brand.Muted) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = if (danger) Brand.Red else Brand.Navy),
                shape = RoundedCornerShape(12.dp),
            ) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Brand.Navy) } },
        containerColor = Color.White,
        shape = RoundedCornerShape(26.dp),
    )
}

@Composable
fun GradientBox(brush: Brush, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.background(brush), content = content)
}
