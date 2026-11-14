package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.example.data.model.ReadingTheme

object ReadingThemeController {

    fun getColorFilter(theme: ReadingTheme): ColorFilter? {
        return when (theme) {
            ReadingTheme.ORIGINAL_COLOR, ReadingTheme.CLEAN_DAY -> null // 100% full vibrant color for colored books, charts, graphs, and manga
            ReadingTheme.MANGA_CONTRAST -> {
                // High-contrast ink booster for sharp black-and-white Manga panels
                val contrast = 1.35f
                val translate = (-0.5f * contrast + 0.5f) * 255f
                val matrix = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, translate,
                        0f, contrast, 0f, 0f, translate,
                        0f, 0f, contrast, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(matrix)
            }
            ReadingTheme.PAPER -> {
                // Gentle warm paper tint
                val matrix = ColorMatrix(
                    floatArrayOf(
                        0.98f, 0f, 0f, 0f, 0f,
                        0f, 0.94f, 0f, 0f, 0f,
                        0f, 0f, 0.85f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(matrix)
            }
            ReadingTheme.PARCHMENT -> {
                // Sepia vintage warmth
                val matrix = ColorMatrix(
                    floatArrayOf(
                        0.90f, 0f, 0f, 0f, 15f,
                        0f, 0.85f, 0f, 0f, 10f,
                        0f, 0f, 0.72f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(matrix)
            }
            ReadingTheme.WARM_AMBER -> {
                // High warm tone for night reading
                val matrix = ColorMatrix(
                    floatArrayOf(
                        1.0f, 0f, 0f, 0f, 10f,
                        0f, 0.90f, 0f, 0f, 0f,
                        0f, 0f, 0.60f, 0f, -20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(matrix)
            }
            ReadingTheme.DARK_SLATE -> {
                // Invert colors with dark slate tone
                val matrix = ColorMatrix(
                    floatArrayOf(
                        -0.85f, 0f, 0f, 0f, 220f,
                        0f, -0.85f, 0f, 0f, 225f,
                        0f, 0f, -0.85f, 0f, 235f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(matrix)
            }
            ReadingTheme.MIDNIGHT_OLED -> {
                // True high contrast inversion for OLED
                val matrix = ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(matrix)
            }
        }
    }

    fun getPageBackgroundColor(theme: ReadingTheme): Color {
        return when (theme) {
            ReadingTheme.ORIGINAL_COLOR, ReadingTheme.CLEAN_DAY -> Color.White
            ReadingTheme.MANGA_CONTRAST -> Color.White
            ReadingTheme.PAPER -> Color(0xFFFBF0D9)
            ReadingTheme.PARCHMENT -> Color(0xFFF5E6CB)
            ReadingTheme.WARM_AMBER -> Color(0xFFFDE68A)
            ReadingTheme.DARK_SLATE -> Color(0xFF1E293B)
            ReadingTheme.MIDNIGHT_OLED -> Color.Black
        }
    }

    fun getSurfaceBackgroundColor(theme: ReadingTheme): Color {
        return when (theme) {
            ReadingTheme.ORIGINAL_COLOR, ReadingTheme.CLEAN_DAY -> Color(0xFFF1F5F9)
            ReadingTheme.MANGA_CONTRAST -> Color(0xFF18181B)
            ReadingTheme.PAPER -> Color(0xFFF3E7CD)
            ReadingTheme.PARCHMENT -> Color(0xFFE8D7B8)
            ReadingTheme.WARM_AMBER -> Color(0xFFF5D875)
            ReadingTheme.DARK_SLATE -> Color(0xFF0F172A)
            ReadingTheme.MIDNIGHT_OLED -> Color(0xFF0A0A0A)
        }
    }

    fun getPrimaryTextColor(theme: ReadingTheme): Color {
        return when (theme) {
            ReadingTheme.ORIGINAL_COLOR, ReadingTheme.CLEAN_DAY -> Color(0xFF0F172A)
            ReadingTheme.MANGA_CONTRAST -> Color(0xFFFFFFFF)
            ReadingTheme.PAPER -> Color(0xFF2D241E)
            ReadingTheme.PARCHMENT -> Color(0xFF382E25)
            ReadingTheme.WARM_AMBER -> Color(0xFF292524)
            ReadingTheme.DARK_SLATE -> Color(0xFFF8FAFC)
            ReadingTheme.MIDNIGHT_OLED -> Color(0xFFFFFFFF)
        }
    }
}
