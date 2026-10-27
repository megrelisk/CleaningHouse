package ge.cleaninghouse.quest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Purple = Color(0xFF6C4CF5)
val PurpleDark = Color(0xFF4B2FD1)
val Coral = Color(0xFFFF7A59)
val Mint = Color(0xFF14C8A8)
val Gold = Color(0xFFFFC233)
val Bg = Color(0xFFF6F3FF)
val Ink = Color(0xFF1E1A3C)
val Muted = Color(0xFF7B7698)

val HeroGradient = Brush.linearGradient(listOf(Purple, Color(0xFFB04CF5), Coral))
val CoinGradient = Brush.linearGradient(listOf(Gold, Color(0xFFFF9F1C)))

fun categoryColor(category: String): Color = when (category) {
    "dishes", "kitchen" -> Color(0xFFFFE3D6)
    "laundry" -> Color(0xFFDDE7FF)
    "bed" -> Color(0xFFEADFFF)
    "floor", "vacuum" -> Color(0xFFDFF7EE)
    "dust", "surfaces", "windows" -> Color(0xFFFFF4CC)
    "trash" -> Color(0xFFE8E8E8)
    "bathroom" -> Color(0xFFD6F4FF)
    "plants" -> Color(0xFFDDF5D4)
    else -> Color(0xFFF1EDFF)
}

@Composable
fun CleanQuestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Purple,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE6E0FF),
            onPrimaryContainer = PurpleDark,
            secondary = Coral,
            onSecondary = Color.White,
            tertiary = Mint,
            background = Bg,
            onBackground = Ink,
            surface = Color.White,
            onSurface = Ink,
            surfaceVariant = Color(0xFFF1EDFF),
            onSurfaceVariant = Muted,
        ),
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(26.dp),
        ),
        content = content,
    )
}
