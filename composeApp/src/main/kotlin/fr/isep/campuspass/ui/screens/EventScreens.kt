package fr.isep.campuspass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.isep.campuspass.domain.Event
import fr.isep.campuspass.domain.EventCategory
import fr.isep.campuspass.domain.UserAccount
import fr.isep.campuspass.domain.formatEuros
import fr.isep.campuspass.ui.components.*
import fr.isep.campuspass.ui.formatDateTime
import fr.isep.campuspass.ui.theme.Brand
import fr.isep.campuspass.ui.theme.icon
import fr.isep.campuspass.viewmodel.EventDetailViewModel
import fr.isep.campuspass.viewmodel.EventsUiState
import fr.isep.campuspass.viewmodel.EventsViewModel
import fr.isep.campuspass.viewmodel.Factories

@Composable
fun EventsScreen(
    user: UserAccount?,
    onOpen: (Int) -> Unit,
    onLogin: () -> Unit,
    vm: EventsViewModel = viewModel(factory = Factories.events),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val category by vm.category.collectAsStateWithLifecycle()

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item(key = "header") {
            GradientBox(Brand.Night, Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))) {
                Column(Modifier.statusBarsPadding().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Brand.Sunset), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.ConfirmationNumber, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Text("  CampusPass", style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.weight(1f))
                        if (user == null) {
                            TextButton(onClick = onLogin) { Text("Sign in", color = Brand.Yellow) }
                        } else {
                            Box(Modifier.size(36.dp).clip(CircleShape).background(Brand.Yellow), contentAlignment = Alignment.Center) {
                                Text(user.name.split(" ").take(2).joinToString("") { it.take(1) }, style = MaterialTheme.typography.labelLarge, color = Brand.Navy)
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(user?.let { "Hi ${it.name.substringBefore(' ')}," } ?: "Hi there,", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.75f))
                    Text("What's on campus?", style = MaterialTheme.typography.displaySmall, color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    TextField(
                        value = query, onValueChange = vm::onQuery,
                        placeholder = { Text("Search events, venues…") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { vm.onQuery("") }) { Icon(Icons.Rounded.Close, "Clear") } },
                        singleLine = true, shape = CircleShape,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item(key = "chips") {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { CategoryChip("All", null, category == null) { vm.onCategory(null) } }
                items(EventCategory.entries) { c -> CategoryChip(c.label, c.icon(), category == c) { vm.onCategory(c) } }
            }
        }
        when (val s = state) {
            is EventsUiState.Loading -> item { Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Brand.Coral) } }
            is EventsUiState.Error -> item { EmptyState(Icons.Rounded.ErrorOutline, "Something went wrong", s.message) }
            is EventsUiState.Success -> {
                if (query.isBlank() && category == null) {
                    item(key = "featuredTitle") { SectionTitle("Coming up next") }
                    item(key = "featured") {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(s.featured, key = { it.id }) { FeaturedCard(it) { onOpen(it.id) } }
                        }
                    }
                    item(key = "allTitle") { SectionTitle("All events") }
                }
                if (s.events.isEmpty()) {
                    item { EmptyState(Icons.Rounded.SearchOff, "No event found", "Try another keyword or category.") }
                } else {
                    items(s.events, key = { it.id }) { e ->
                        Box(Modifier.padding(horizontal = 16.dp).animateItem()) { EventCard(e, s.seatsLeft[e.id] ?: 0) { onOpen(e.id) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 20.dp))
}

@Composable
private fun CategoryChip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = if (selected) Brand.Navy else Color.White, shadowElevation = if (selected) 0.dp else 1.dp) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, Modifier.size(18.dp), tint = if (selected) Brand.Yellow else Brand.Coral); Spacer(Modifier.width(6.dp)) }
            Text(text, style = MaterialTheme.typography.labelMedium, color = if (selected) Color.White else Brand.Navy)
        }
    }
}

@Composable
private fun FeaturedCard(event: Event, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(24.dp), modifier = Modifier.width(290.dp).height(200.dp)) {
        Box {
            Poster(event.posterUrl, event.category, Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
            DateBadge(event, Modifier.align(Alignment.TopStart).padding(12.dp))
            PricePill(event.priceCents, Modifier.align(Alignment.TopEnd).padding(12.dp))
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(event.title, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(event.venue, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
            }
        }
    }
}

@Composable
fun EventDetailScreen(
    eventId: Int,
    onBack: () -> Unit,
    onCheckout: (Int, Int) -> Unit,
    vm: EventDetailViewModel = viewModel(key = "event$eventId", factory = Factories.eventDetail(eventId)),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val qty by vm.quantity.collectAsStateWithLifecycle()
    val d = ui ?: return
    val e = d.event

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(320.dp)) {
                Poster(e.posterUrl, e.category, Modifier.matchParentSize())
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent, Color.Transparent))))
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.statusBarsPadding().padding(12.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)),
                ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Brand.Navy) }
            }
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(e.category.icon(), null, tint = Brand.Coral, modifier = Modifier.size(18.dp))
                    Text("  ${e.category.label.uppercase()}  ·  ${e.organizer}", style = MaterialTheme.typography.labelSmall, color = Brand.Coral)
                }
                Text(e.title, style = MaterialTheme.typography.headlineMedium)
                InfoRow(Icons.Rounded.CalendarMonth, formatDateTime(e.startsAt))
                InfoRow(Icons.Rounded.Place, e.venue)
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 1.dp) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row {
                            Text("${d.sold} / ${e.capacity} tickets sold", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            SeatsLine(d.seatsLeft)
                        }
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { d.sold.toFloat() / e.capacity },
                            color = if (d.seatsLeft == 0) Brand.Red else Brand.Coral,
                            trackColor = Color(0xFFEFEDF7),
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        )
                    }
                }
                Text("About", style = MaterialTheme.typography.titleLarge)
                Text(e.description, style = MaterialTheme.typography.bodyLarge, color = Brand.Navy.copy(alpha = 0.8f))
                Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFFE8E6FF)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.VerifiedUser, null, tint = Brand.Navy)
                        Text(
                            "  Secure payment with Google Pay, confirmed by your fingerprint. Your ticket is a signed, single-use QR code.",
                            style = MaterialTheme.typography.bodySmall, color = Brand.Navy,
                        )
                    }
                }
            }
        }
        // Sticky purchase bar
        Surface(shadowElevation = 12.dp, color = Color.White) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text((e.priceCents * qty).formatEuros(), style = MaterialTheme.typography.headlineSmall)
                    Text("${e.priceCents.formatEuros()} × $qty", style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
                }
                if (d.seatsLeft > 0) {
                    Stepper(qty, vm::decrement, vm::increment)
                    Spacer(Modifier.width(12.dp))
                    PrimaryButton("Get tickets", { onCheckout(e.id, qty) }, coral = true)
                } else {
                    PrimaryButton("Sold out", {}, enabled = false)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE8E6FF)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Brand.Navy, modifier = Modifier.size(20.dp))
        }
        Text("  $text", style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun Stepper(value: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.clip(CircleShape).background(Color(0xFFEFEDF7)), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMinus, modifier = Modifier.size(40.dp)) { Icon(Icons.Rounded.Remove, "Less") }
        Text("$value", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onPlus, modifier = Modifier.size(40.dp)) { Icon(Icons.Rounded.Add, "More") }
    }
}
