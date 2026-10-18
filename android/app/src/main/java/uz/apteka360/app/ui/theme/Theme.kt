package uz.apteka360.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Dizayn tokenlari (packages/tokens/tokens.css)
val Accent = Color(0xFF1B8A55)
val AccentHover = Color(0xFF167346)
val AccentDark = Color(0xFF22A05F)
val Danger = Color(0xFFC42F37)
val Warning = Color(0xFF8A5A00)
val WarningFill = Color(0x2EE2A336)
val Info = Color(0xFF2A5BC4)
val PageLight = Color(0xFFF3F4F1)
val CardLight = Color(0xFFFFFFFF)
val InsetLight = Color(0xFFF8F9F6)
val BorderLight = Color(0xFFE7E9E4)
val TextPrimary = Color(0xFF0F1512)
val TextSecondary = Color(0xFF56605A)
val TextMuted = Color(0xFF6B746E)

private val LightColors = lightColorScheme(
    primary = Accent, onPrimary = Color.White,
    primaryContainer = Color(0x1A1B8A55), onPrimaryContainer = AccentHover,
    secondary = AccentHover, onSecondary = Color.White,
    secondaryContainer = Color(0x1F1B8A55), onSecondaryContainer = AccentHover,
    surfaceContainer = Color(0xFFFFFFFF), surfaceContainerHigh = Color(0xFFF8F9F6), surfaceContainerHighest = Color(0xFFEEF0EC),
    background = PageLight, onBackground = TextPrimary,
    surface = CardLight, onSurface = TextPrimary,
    surfaceVariant = InsetLight, onSurfaceVariant = TextSecondary,
    outline = Color(0xFFD9DDD6), outlineVariant = BorderLight,
    error = Danger, onError = Color.White, errorContainer = Color(0x1AE5484D), onErrorContainer = Danger,
    tertiary = Info,
)

private val DarkColors = darkColorScheme(
    primary = AccentDark, onPrimary = Color.White,
    primaryContainer = Color(0x2922A05F), onPrimaryContainer = Color(0xFF60D090),
    secondary = Color(0xFF60D090), onSecondary = Color.Black,
    secondaryContainer = Color(0x2E22A05F), onSecondaryContainer = Color(0xFF60D090),
    surfaceContainer = Color(0xFF141615), surfaceContainerHigh = Color(0xFF1F2120), surfaceContainerHighest = Color(0xFF262827),
    background = Color(0xFF0B0C0C), onBackground = Color.White,
    surface = Color(0xFF141615), onSurface = Color.White,
    surfaceVariant = Color(0xFF181A19), onSurfaceVariant = Color(0xFF9DA3A0),
    outline = Color(0xFF2C2F2E), outlineVariant = Color(0xFF232625),
    error = Color(0xFFFF8787), onError = Color.Black, errorContainer = Color(0x33FF8787), onErrorContainer = Color(0xFFFF8787),
    tertiary = Color(0xFF7FA6FF),
)

val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp), large = RoundedCornerShape(18.dp), extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun Apteka360Theme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, shapes = AppShapes, content = content)
}
