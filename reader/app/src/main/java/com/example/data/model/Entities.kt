package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val filePath: String,
    val coverPath: String? = null,
    val pageCount: Int,
    val currentPage: Int = 0,
    val percentRead: Int = 0,
    val lastReadTimestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val fileSizeFormatted: String = "",
    val isManga: Boolean = false
)

@Entity(tableName = "annotations")
data class AnnotationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val pageIndex: Int,
    val type: String, // "HIGHLIGHT", "PEN", "STICKY_NOTE"
    val colorHex: String,
    val strokeWidth: Float,
    val pointsData: String, // Comma-separated or JSON list of x1,y1;x2,y2 normalized
    val noteText: String = "",
    val xPercent: Float = 0f,
    val yPercent: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val pageIndex: Int,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ReadingDirection(val displayName: String, val shortLabel: String, val description: String) {
    LEFT_TO_RIGHT("Left to Right (Standard)", "LTR", "Standard Western book page turn"),
    RIGHT_TO_LEFT("Right to Left (Manga)", "Manga RTL", "Read right-to-left like authentic manga"),
    VERTICAL_WEBTOON("Vertical Scroll (Webtoon)", "Webtoon", "Continuous vertical webtoon strip")
}

enum class ReadingTheme(val displayName: String, val bgHex: Long, val textHex: Long) {
    ORIGINAL_COLOR("Full Color", 0xFFFFFFFF, 0xFF0F172A),
    PAPER("Paper", 0xFFFBF0D9, 0xFF2D241E),
    PARCHMENT("Sepia", 0xFFF5E6CB, 0xFF3D3226),
    CLEAN_DAY("Day", 0xFFFFFFFF, 0xFF1E293B),
    MANGA_CONTRAST("Manga B&W", 0xFFFFFFFF, 0xFF000000),
    WARM_AMBER("Amber", 0xFFFDE68A, 0xFF312E81),
    DARK_SLATE("Dark", 0xFF1E293B, 0xFFE2E8F0),
    MIDNIGHT_OLED("OLED", 0xFF000000, 0xFFE2E8F0)
}

enum class PageTurnMode(val displayName: String) {
    CURL_3D("3D Page Curl"),
    SLIDE("Smooth Slide"),
    FADE("Soft Fade")
}

enum class AnnotationTool {
    NONE,
    HIGHLIGHT,
    PEN,
    STICKY_NOTE,
    ERASER
}

data class DrawnStroke(
    val id: Long = 0,
    val pageIndex: Int,
    val isHighlight: Boolean,
    val colorHex: String,
    val strokeWidth: Float,
    val points: List<Pair<Float, Float>> // Normalized 0..1 coordinates
)

data class StickyNote(
    val id: Long = 0,
    val pageIndex: Int,
    val xPercent: Float,
    val yPercent: Float,
    val text: String,
    val colorHex: String,
    val timestamp: Long = System.currentTimeMillis()
)
