package com.example

import com.example.data.model.PageTurnMode
import com.example.data.model.ReadingDirection
import com.example.data.model.ReadingTheme
import com.example.ui.components.ReadingThemeController
import com.example.ui.components.parseHexColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun readingTheme_palettesAreDefined() {
        for (theme in ReadingTheme.values()) {
            val bgColor = ReadingThemeController.getPageBackgroundColor(theme)
            val textColor = ReadingThemeController.getPrimaryTextColor(theme)
            assertNotNull(bgColor)
            assertNotNull(textColor)
        }
    }

    @Test
    fun parseHexColor_handlesVariousFormats() {
        val yellow = parseHexColor("#FFF200")
        assertNotNull(yellow)

        val withAlpha = parseHexColor("#80FF0000")
        assertNotNull(withAlpha)

        val invalidFallback = parseHexColor("invalid")
        assertNotNull(invalidFallback)
    }

    @Test
    fun pageTurnModes_areAvailable() {
        assertEquals(3, PageTurnMode.values().size)
    }

    @Test
    fun readingDirection_includesMangaAndWebtoon() {
        assertEquals(3, ReadingDirection.values().size)
        assertNotNull(ReadingDirection.RIGHT_TO_LEFT)
        assertNotNull(ReadingDirection.VERTICAL_WEBTOON)
    }

    @Test
    fun readingTheme_originalColorHasNoFilter() {
        val filter = ReadingThemeController.getColorFilter(ReadingTheme.ORIGINAL_COLOR)
        assertEquals(null, filter)
    }

    @Test
    fun readingDirection_mangaIsRightToLeft() {
        assertEquals("Manga RTL", ReadingDirection.RIGHT_TO_LEFT.shortLabel)
        assertEquals("LTR", ReadingDirection.LEFT_TO_RIGHT.shortLabel)
        assertEquals(true, ReadingDirection.RIGHT_TO_LEFT.description.contains("manga", ignoreCase = true))
    }

    @Test
    fun readingTheme_originalColorDisplayNameIsFullColor() {
        assertEquals("Full Color", ReadingTheme.ORIGINAL_COLOR.displayName)
    }
}

