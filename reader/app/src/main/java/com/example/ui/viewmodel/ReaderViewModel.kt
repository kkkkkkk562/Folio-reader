package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AnnotationEntity
import com.example.data.model.AnnotationTool
import com.example.data.model.BookEntity
import com.example.data.model.BookmarkEntity
import com.example.data.model.DrawnStroke
import com.example.data.model.PageTurnMode
import com.example.data.model.ReadingDirection
import com.example.data.model.ReadingTheme
import com.example.data.model.StickyNote
import com.example.data.repository.BookRepository
import com.example.pdf.PdfRendererManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReaderUiState(
    val book: BookEntity? = null,
    val currentPage: Int = 0,
    val pageCount: Int = 1,
    val currentPageBitmap: Bitmap? = null,
    val nextPageBitmap: Bitmap? = null,
    val prevPageBitmap: Bitmap? = null,
    val isLoadingPage: Boolean = false,
    val isBookmarked: Boolean = false,
    val readingTheme: ReadingTheme = ReadingTheme.ORIGINAL_COLOR,
    val pageTurnMode: PageTurnMode = PageTurnMode.CURL_3D,
    val readingDirection: ReadingDirection = ReadingDirection.LEFT_TO_RIGHT,
    val isMangaMode: Boolean = false,
    val annotationTool: AnnotationTool = AnnotationTool.NONE,
    val activeColorHex: String = "#FFF000",
    val activeStrokeWidth: Float = 16f,
    val pageStrokes: List<DrawnStroke> = emptyList(),
    val pageStickyNotes: List<StickyNote> = emptyList(),
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val allAnnotations: List<AnnotationEntity> = emptyList(),
    val isControlsVisible: Boolean = true,
    val zoomScale: Float = 1.0f,
    val editingStickyNote: StickyNote? = null,
    val isCreatingStickyNote: Boolean = false,
    val newStickyNoteCoords: Pair<Float, Float>? = null,
    val showDrawer: Boolean = false,
    val drawerTab: Int = 0 // 0 = Bookmarks, 1 = Annotations, 2 = Appearance / Manga
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = BookRepository(
        context = application,
        bookDao = database.bookDao(),
        annotationDao = database.annotationDao(),
        bookmarkDao = database.bookmarkDao()
    )
    private val pdfManager = PdfRendererManager(application)

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    fun loadBook(bookId: Long) {
        viewModelScope.launch {
            val book = repository.getBookById(bookId) ?: return@launch
            val isManga = book.isManga
            _uiState.value = _uiState.value.copy(
                book = book,
                currentPage = book.currentPage.coerceIn(0, (book.pageCount - 1).coerceAtLeast(0)),
                pageCount = book.pageCount.coerceAtLeast(1),
                isMangaMode = isManga,
                readingDirection = if (isManga) ReadingDirection.RIGHT_TO_LEFT else ReadingDirection.LEFT_TO_RIGHT,
                readingTheme = ReadingTheme.ORIGINAL_COLOR
            )

            // Open PDF document
            val totalPages = pdfManager.openDocument(book.filePath)
            val initialPage = book.currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))

            _uiState.value = _uiState.value.copy(pageCount = totalPages, currentPage = initialPage)

            // Collect bookmarks and annotations reactively
            launch {
                repository.getBookmarks(bookId).collect { bookmarks ->
                    _uiState.value = _uiState.value.copy(
                        bookmarks = bookmarks,
                        isBookmarked = bookmarks.any { it.pageIndex == _uiState.value.currentPage }
                    )
                }
            }

            launch {
                repository.getAnnotationsForBook(bookId).collect { annotations ->
                    _uiState.value = _uiState.value.copy(allAnnotations = annotations)
                    refreshPageAnnotations(_uiState.value.currentPage, annotations)
                }
            }

            renderCurrentAndAdjacentPages(initialPage)
        }
    }

    private fun refreshPageAnnotations(page: Int, annotations: List<AnnotationEntity>) {
        val pageAnnotations = annotations.filter { it.pageIndex == page }
        val strokes = pageAnnotations.filter { it.type == "HIGHLIGHT" || it.type == "PEN" }.map { entity ->
            DrawnStroke(
                id = entity.id,
                pageIndex = entity.pageIndex,
                isHighlight = (entity.type == "HIGHLIGHT"),
                colorHex = entity.colorHex,
                strokeWidth = entity.strokeWidth,
                points = deserializePoints(entity.pointsData)
            )
        }
        val stickyNotes = pageAnnotations.filter { it.type == "STICKY_NOTE" }.map { entity ->
            StickyNote(
                id = entity.id,
                pageIndex = entity.pageIndex,
                xPercent = entity.xPercent,
                yPercent = entity.yPercent,
                text = entity.noteText,
                colorHex = entity.colorHex,
                timestamp = entity.timestamp
            )
        }
        _uiState.value = _uiState.value.copy(
            pageStrokes = strokes,
            pageStickyNotes = stickyNotes
        )
    }

    private fun renderCurrentAndAdjacentPages(page: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingPage = true)

            val currentBitmap = pdfManager.renderPage(page)
            val nextBitmap = if (page + 1 < _uiState.value.pageCount) pdfManager.renderPage(page + 1) else null
            val prevBitmap = if (page - 1 >= 0) pdfManager.renderPage(page - 1) else null

            val isBookmarked = _uiState.value.bookmarks.any { it.pageIndex == page }

            _uiState.value = _uiState.value.copy(
                currentPage = page,
                currentPageBitmap = currentBitmap,
                nextPageBitmap = nextBitmap,
                prevPageBitmap = prevBitmap,
                isLoadingPage = false,
                isBookmarked = isBookmarked
            )

            // Refresh annotations for current page
            refreshPageAnnotations(page, _uiState.value.allAnnotations)

            // Save reading progress to database
            _uiState.value.book?.let { b ->
                repository.updateReadingProgress(b.id, page, _uiState.value.pageCount)
            }
        }
    }

    fun goToPage(page: Int) {
        val target = page.coerceIn(0, (_uiState.value.pageCount - 1).coerceAtLeast(0))
        if (target != _uiState.value.currentPage) {
            renderCurrentAndAdjacentPages(target)
        }
    }

    fun nextPage() {
        if (_uiState.value.currentPage + 1 < _uiState.value.pageCount) {
            goToPage(_uiState.value.currentPage + 1)
        }
    }

    fun prevPage() {
        if (_uiState.value.currentPage - 1 >= 0) {
            goToPage(_uiState.value.currentPage - 1)
        }
    }

    fun toggleControls() {
        if (_uiState.value.annotationTool == AnnotationTool.NONE) {
            _uiState.value = _uiState.value.copy(isControlsVisible = !_uiState.value.isControlsVisible)
        }
    }

    fun setReadingTheme(theme: ReadingTheme) {
        _uiState.value = _uiState.value.copy(readingTheme = theme)
    }

    fun setPageTurnMode(mode: PageTurnMode) {
        _uiState.value = _uiState.value.copy(pageTurnMode = mode)
    }

    fun setReadingDirection(direction: ReadingDirection) {
        val isManga = (direction == ReadingDirection.RIGHT_TO_LEFT || direction == ReadingDirection.VERTICAL_WEBTOON)
        _uiState.value = _uiState.value.copy(
            readingDirection = direction,
            isMangaMode = isManga,
            readingTheme = if (isManga && _uiState.value.readingTheme == ReadingTheme.PAPER) ReadingTheme.MANGA_CONTRAST else _uiState.value.readingTheme
        )
    }

    fun toggleMangaMode() {
        val nextManga = !_uiState.value.isMangaMode
        _uiState.value = _uiState.value.copy(
            isMangaMode = nextManga,
            readingDirection = if (nextManga) ReadingDirection.RIGHT_TO_LEFT else ReadingDirection.LEFT_TO_RIGHT,
            readingTheme = if (nextManga) ReadingTheme.MANGA_CONTRAST else ReadingTheme.PAPER
        )
    }

    suspend fun getPageBitmap(pageIndex: Int): Bitmap? {
        return pdfManager.renderPage(pageIndex)
    }

    fun setAnnotationTool(tool: AnnotationTool) {
        val defaultColor = when (tool) {
            AnnotationTool.HIGHLIGHT -> "#FFF200"
            AnnotationTool.PEN -> "#2563EB"
            AnnotationTool.STICKY_NOTE -> "#F59E0B"
            else -> _uiState.value.activeColorHex
        }
        val defaultWidth = when (tool) {
            AnnotationTool.HIGHLIGHT -> 20f
            AnnotationTool.PEN -> 4f
            else -> _uiState.value.activeStrokeWidth
        }

        _uiState.value = _uiState.value.copy(
            annotationTool = tool,
            activeColorHex = defaultColor,
            activeStrokeWidth = defaultWidth,
            isControlsVisible = true // keep toolbar visible while editing annotations
        )
    }

    fun setAnnotationColor(hex: String) {
        _uiState.value = _uiState.value.copy(activeColorHex = hex)
    }

    fun setStrokeWidth(width: Float) {
        _uiState.value = _uiState.value.copy(activeStrokeWidth = width)
    }

    fun setZoomScale(scale: Float) {
        _uiState.value = _uiState.value.copy(zoomScale = scale)
    }

    fun resetZoom() {
        _uiState.value = _uiState.value.copy(zoomScale = 1.0f)
    }

    fun toggleBookmark() {
        val book = _uiState.value.book ?: return
        val page = _uiState.value.currentPage
        viewModelScope.launch {
            val title = "Page ${page + 1} bookmark"
            repository.toggleBookmark(book.id, page, title)
        }
    }

    fun onStrokeDrawn(stroke: DrawnStroke) {
        val book = _uiState.value.book ?: return
        val page = _uiState.value.currentPage
        viewModelScope.launch {
            val entity = AnnotationEntity(
                bookId = book.id,
                pageIndex = page,
                type = if (stroke.isHighlight) "HIGHLIGHT" else "PEN",
                colorHex = stroke.colorHex,
                strokeWidth = stroke.strokeWidth,
                pointsData = serializePoints(stroke.points)
            )
            repository.saveAnnotation(entity)
        }
    }

    fun undoLastStroke() {
        val book = _uiState.value.book ?: return
        val strokes = _uiState.value.pageStrokes
        if (strokes.isNotEmpty()) {
            val lastStroke = strokes.last()
            if (lastStroke.id > 0) {
                viewModelScope.launch {
                    repository.deleteAnnotation(lastStroke.id)
                }
            }
        }
    }

    fun clearCurrentPageAnnotations() {
        val book = _uiState.value.book ?: return
        val page = _uiState.value.currentPage
        viewModelScope.launch {
            repository.clearPageAnnotations(book.id, page)
        }
    }

    fun onStickyNoteDropped(xPercent: Float, yPercent: Float) {
        _uiState.value = _uiState.value.copy(
            isCreatingStickyNote = true,
            newStickyNoteCoords = xPercent to yPercent,
            editingStickyNote = null
        )
    }

    fun onStickyNoteClicked(note: StickyNote) {
        _uiState.value = _uiState.value.copy(
            isCreatingStickyNote = false,
            newStickyNoteCoords = null,
            editingStickyNote = note
        )
    }

    fun saveStickyNote(text: String) {
        val book = _uiState.value.book ?: return
        val page = _uiState.value.currentPage
        val coords = _uiState.value.newStickyNoteCoords
        val existingNote = _uiState.value.editingStickyNote

        viewModelScope.launch {
            if (existingNote != null) {
                val updated = AnnotationEntity(
                    id = existingNote.id,
                    bookId = book.id,
                    pageIndex = page,
                    type = "STICKY_NOTE",
                    colorHex = existingNote.colorHex,
                    strokeWidth = 0f,
                    pointsData = "",
                    noteText = text,
                    xPercent = existingNote.xPercent,
                    yPercent = existingNote.yPercent,
                    timestamp = System.currentTimeMillis()
                )
                repository.saveAnnotation(updated)
            } else if (coords != null) {
                val newNote = AnnotationEntity(
                    bookId = book.id,
                    pageIndex = page,
                    type = "STICKY_NOTE",
                    colorHex = "#F59E0B",
                    strokeWidth = 0f,
                    pointsData = "",
                    noteText = text,
                    xPercent = coords.first,
                    yPercent = coords.second,
                    timestamp = System.currentTimeMillis()
                )
                repository.saveAnnotation(newNote)
            }
            dismissStickyNoteDialog()
        }
    }

    fun deleteStickyNote(id: Long) {
        viewModelScope.launch {
            repository.deleteAnnotation(id)
            dismissStickyNoteDialog()
        }
    }

    fun dismissStickyNoteDialog() {
        _uiState.value = _uiState.value.copy(
            isCreatingStickyNote = false,
            newStickyNoteCoords = null,
            editingStickyNote = null
        )
    }

    fun setDrawerOpen(isOpen: Boolean, tab: Int = 0) {
        _uiState.value = _uiState.value.copy(showDrawer = isOpen, drawerTab = tab)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            pdfManager.close()
        }
    }

    private fun serializePoints(points: List<Pair<Float, Float>>): String {
        return points.joinToString(";") { "${it.first},${it.second}" }
    }

    private fun deserializePoints(data: String): List<Pair<Float, Float>> {
        if (data.isBlank()) return emptyList()
        return try {
            data.split(";").mapNotNull { pairStr ->
                val parts = pairStr.split(",")
                if (parts.size == 2) {
                    val x = parts[0].toFloatOrNull()
                    val y = parts[1].toFloatOrNull()
                    if (x != null && y != null) x to y else null
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
