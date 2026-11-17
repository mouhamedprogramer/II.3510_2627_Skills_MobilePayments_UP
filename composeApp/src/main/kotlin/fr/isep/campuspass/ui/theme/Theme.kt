package fr.isep.campuspass.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.DownhillSkiing
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SportsBasketball
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.isep.campuspass.domain.EventCategory

/** CampusPass visual identity: night navy, coral for actions, yellow for highlights. */
object Brand {
    val Navy = Color(0xFF16133A)
    val NavySoft = Color(0xFF2A2560)
    val Coral = Color(0xFFFF5A5F)
    val Yellow = Color(0xFFFFC857)
    val Mint = Color(0xFF22B07D)
    val Red = Color(0xFFE5484D)
    val Background = Color(0xFFF6F5FB)
    val Card = Color.White
    val Muted = Color(0xFF6B6880)
    val Line = Color(0xFFE6E4F0)
    val Night = Brush.linearGradient(listOf(Color(0xFF2A2560), Color(0xFF16133A)))
    val Sunset = Brush.linearGradient(listOf(Color(0xFFFF8A5B), Color(0xFFFF5A5F), Color(0xFFE23E7B)))
}

private val Colors = lightColorScheme(
    primary = Brand.Navy, onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E6FF), onPrimaryContainer = Brand.Navy,
    secondary = Brand.Coral, onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE3E4), onSecondaryContainer = Color(0xFF5C0A0E),
    tertiary = Brand.Mint,
    background = Brand.Background, onBackground = Brand.Navy,
    surface = Brand.Card, onSurface = Brand.Navy,
    surfaceVariant = Color(0xFFEFEDF7), onSurfaceVariant = Brand.Muted,
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White, surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White, surfaceContainerHighest = Color(0xFFEFEDF7),
    outline = Brand.Line, outlineVariant = Brand.Line,
    error = Brand.Red,
)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
)

@Composable
fun CampusPassTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        typography = AppTypography,
        shapes = Shapes(small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(22.dp)),
        content = content,
    )
}

fun EventCategory.icon(): ImageVector = when (this) {
    EventCategory.PARTY -> Icons.Rounded.Celebration
    EventCategory.CONCERT -> Icons.Rounded.MusicNote
    EventCategory.SPORT -> Icons.Rounded.SportsBasketball
    EventCategory.TALK -> Icons.Rounded.Mic
    EventCategory.CULTURE -> Icons.Rounded.TheaterComedy
    EventCategory.TRIP -> Icons.Rounded.DownhillSkiing
}
