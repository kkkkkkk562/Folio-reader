package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class StudyColors(
    val isDark: Boolean,
    val woodTop: Color,
    val woodFace: Color,
    val woodGrain: Color,
    val woodBottom: Color,
    val waxSeal: Color,
    val sageBadge: Color,
    val brassAccent: Color,
    val ribbonPaper: Color,
    val ribbonBorder: Color,
    val linenGrainDot: Color
)

private val LightStudyColors = StudyColors(
    isDark = false,
    woodTop = HoneyWalnutTop,
    woodFace = HoneyWalnutFace,
    woodGrain = HoneyWalnutGrain,
    woodBottom = HoneyWalnutBottom,
    waxSeal = WaxSealCrimson,
    sageBadge = ForestSage,
    brassAccent = WarmOchre,
    ribbonPaper = CottonPaper,
    ribbonBorder = LinenBorder,
    linenGrainDot = Color(0x126B5D52)
)

private val DarkStudyColors = StudyColors(
    isDark = true,
    woodTop = SmokedOakTop,
    woodFace = SmokedOakFace,
    woodGrain = SmokedOakGrain,
    woodBottom = SmokedOakBottom,
    waxSeal = TerracottaSeal,
    sageBadge = SageLeafDark,
    brassAccent = CandleBrass,
    ribbonPaper = DarkLeather,
    ribbonBorder = EspressoBorder,
    linenGrainDot = Color(0x16F2EAE0)
)

val LocalStudyColors = staticCompositionLocalOf { LightStudyColors }

private val DarkColorScheme = darkColorScheme(
    primary = CandleBrass,
    onPrimary = EspressoWood,
    primaryContainer = SmokedWalnut,
    onPrimaryContainer = CreamVellum,
    secondary = TerracottaSeal,
    onSecondary = CreamVellum,
    tertiary = SageLeafDark,
    onTertiary = EspressoWood,
    background = EspressoWood,
    onBackground = CreamVellum,
    surface = DarkLeather,
    onSurface = CreamVellum,
    surfaceVariant = SmokedWalnut,
    onSurfaceVariant = MutedUmber,
    outline = EspressoBorder,
    outlineVariant = EspressoBorderSubtle,
    error = TerracottaSeal,
    onError = CreamVellum
)

private val LightColorScheme = lightColorScheme(
    primary = LeatherBrown,
    onPrimary = CottonPaper,
    primaryContainer = AgedParchment,
    onPrimaryContainer = FountainInk,
    secondary = WaxSealCrimson,
    onSecondary = CottonPaper,
    tertiary = ForestSage,
    onTertiary = CottonPaper,
    background = LinenCanvas,
    onBackground = FountainInk,
    surface = CottonPaper,
    onSurface = FountainInk,
    surfaceVariant = AgedParchment,
    onSurfaceVariant = SepiaGraphite,
    outline = LinenBorder,
    outlineVariant = LinenBorderSubtle,
    error = WaxSealCrimson,
    onError = CottonPaper
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val studyColors = if (darkTheme) DarkStudyColors else LightStudyColors

    CompositionLocalProvider(LocalStudyColors provides studyColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
