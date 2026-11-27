package ir.dakhlokharj.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.dakhlokharj.R
import ir.dakhlokharj.domain.model.CurrencyUnit
import ir.dakhlokharj.domain.model.ThemeMode
import ir.dakhlokharj.domain.model.UserSettings

val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

private val OnDark = Color(0xFFF8FAFC)

private val DarkScheme = darkColorScheme(
    primary = DkColors.Emerald,
    onPrimary = DkColors.MidnightNavy,
    primaryContainer = DkColors.EmeraldDeep,
    onPrimaryContainer = DkColors.SoftMint,
    secondary = DkColors.MutedGold,
    onSecondary = DkColors.MidnightNavy,
    secondaryContainer = DkColors.DeepIndigo,
    onSecondaryContainer = DkColors.MutedGold,
    tertiary = DkColors.IndigoAccent,
    onTertiary = DkColors.MidnightNavy,
    background = DkColors.MidnightNavy,
    onBackground = OnDark,
    surface = DkColors.SurfaceDark,
    onSurface = OnDark,
    surfaceVariant = DkColors.SurfaceDarkAlt,
    onSurfaceVariant = DkColors.Slate,
    outline = androidx.compose.ui.graphics.Color(0xFF334155),
    error = DkColors.Danger,
    onError = DkColors.MidnightNavy,
)

private val LightScheme = lightColorScheme(
    primary = DkColors.EmeraldDeep,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = DkColors.SoftMint,
    onPrimaryContainer = DkColors.Charcoal,
    secondary = androidx.compose.ui.graphics.Color(0xFF8A6D1F),
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = androidx.compose.ui.graphics.Color(0xFF4F46E5),
    background = DkColors.WarmWhite,
    onBackground = DkColors.Charcoal,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = DkColors.Charcoal,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFEDEFE9),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF5B6472),
    outline = androidx.compose.ui.graphics.Color(0xFFCBD5E1),
    error = androidx.compose.ui.graphics.Color(0xFFB91C1C),
)

private val OnDark = androidx.compose.ui.graphics.Color(0xFFF8FAFC)

private val DkShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private fun typography(): Typography {
    val base = TextStyle(fontFamily = Vazirmatn, letterSpacing = 0.sp)
    return Typography(
        displayLarge = base.copy(fontSize = 44.sp, fontWeight = FontWeight.Bold, lineHeight = 56.sp),
        displayMedium = base.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold, lineHeight = 46.sp),
        displaySmall = base.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp),
        headlineLarge = base.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp),
        headlineMedium = base.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
        headlineSmall = base.copy(fontSize = 19.sp, fontWeight = FontWeight.Medium, lineHeight = 28.sp),
        titleLarge = base.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium, lineHeight = 28.sp),
        titleMedium = base.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp),
        titleSmall = base.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp),
        bodyLarge = base.copy(fontSize = 16.sp, lineHeight = 26.sp),
        bodyMedium = base.copy(fontSize = 14.sp, lineHeight = 24.sp),
        bodySmall = base.copy(fontSize = 12.sp, lineHeight = 20.sp),
        labelLarge = base.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
        labelMedium = base.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp),
        labelSmall = base.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
    )
}

@Composable
fun DkTheme(settings: UserSettings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(
        LocalCurrencyUnit provides settings.currencyUnit,
        LocalHideBalances provides settings.hideBalances,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = typography(),
            shapes = DkShapes,
            content = content,
        )
    }
}

val LocalCurrencyUnit = androidx.compose.runtime.staticCompositionLocalOf { CurrencyUnit.TOMAN }
val LocalHideBalances = androidx.compose.runtime.staticCompositionLocalOf { false }