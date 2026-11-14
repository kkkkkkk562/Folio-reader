package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BookEntity
import com.example.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryFilter {
    ALL,
    MANGA,
    READING,
    FAVORITES,
    COMPLETED
}

data class LibraryUiState(
    val books: List<BookEntity> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: LibraryFilter = LibraryFilter.ALL,
    val isImporting: Boolean = false,
    val importError: String? = null,
    val totalPagesRead: Int = 0,
    val completedBooksCount: Int = 0,
    val isDarkMode: Boolean? = null,
    val pendingDraft: com.example.data.repository.PdfImportDraft? = null
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = BookRepository(
        context = application,
        bookDao = database.bookDao(),
        annotationDao = database.annotationDao(),
        bookmarkDao = database.bookmarkDao()
    )

    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow(LibraryFilter.ALL)
    private val _isImporting = MutableStateFlow(false)
    private val _importError = MutableStateFlow<String?>(null)
    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    private val _pendingDraft = MutableStateFlow<com.example.data.repository.PdfImportDraft?>(null)

    private val _importState = combine(_isImporting, _importError, _pendingDraft) { importing, error, draft ->
        Triple(importing, error, draft)
    }

    private val _uiParams = combine(
        _searchQuery,
        _activeFilter,
        _isDarkMode,
        _importState
    ) { query, filter, darkMode, (importing, error, draft) ->
        UiParams(query, filter, importing, error, darkMode, draft)
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.allBooks,
        _uiParams
    ) { books, params ->
        val query = params.query
        val filter = params.filter
        val filtered = books.filter { book ->
            val matchesQuery = query.isBlank() ||
                    book.title.contains(query, ignoreCase = true) ||
                    book.author.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                LibraryFilter.ALL -> true
                LibraryFilter.MANGA -> book.isManga
                LibraryFilter.READING -> book.percentRead in 1..99
                LibraryFilter.FAVORITES -> book.isFavorite
                LibraryFilter.COMPLETED -> book.percentRead >= 100
            }
            matchesQuery && matchesFilter
        }

        val totalPages = books.sumOf { (it.pageCount * (it.percentRead / 100f)).toInt() }
        val completedCount = books.count { it.percentRead >= 100 }

        LibraryUiState(
            books = filtered,
            searchQuery = query,
            activeFilter = filter,
            isImporting = params.importing,
            importError = params.error,
            totalPagesRead = totalPages,
            completedBooksCount = completedCount,
            isDarkMode = params.darkMode,
            pendingDraft = params.draft
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState(isImporting = true)
    )

private data class UiParams(
    val query: String,
    val filter: LibraryFilter,
    val importing: Boolean,
    val error: String?,
    val darkMode: Boolean?,
    val draft: com.example.data.repository.PdfImportDraft?
)

    init {
        viewModelScope.launch {
            repository.initializeLibraryIfEmpty()
        }
    }

    fun toggleDarkMode(systemIsDark: Boolean) {
        val current = _isDarkMode.value ?: systemIsDark
        _isDarkMode.value = !current
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterChanged(newFilter: LibraryFilter) {
        _activeFilter.value = newFilter
    }

    fun onPdfSelected(uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            _importError.value = null
            val result = repository.inspectPdfDraft(uri)
            _isImporting.value = false
            result.onSuccess { draft ->
                _pendingDraft.value = draft
            }.onFailure { err ->
                _importError.value = err.localizedMessage ?: "Failed to read selected PDF file"
            }
        }
    }

    fun confirmImport(
        draft: com.example.data.repository.PdfImportDraft,
        isManga: Boolean,
        title: String,
        author: String,
        onImportSuccess: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            _pendingDraft.value = null
            _isImporting.value = true
            _importError.value = null
            val result = repository.importPdfFromUri(
                uri = draft.uri,
                isManga = isManga,
                customTitle = title,
                customAuthor = author
            )
            _isImporting.value = false
            result.onSuccess { book ->
                onImportSuccess(book.id)
            }.onFailure { err ->
                _importError.value = err.localizedMessage ?: "Failed to import PDF file"
            }
        }
    }

    fun dismissImportDraft() {
        _pendingDraft.value = null
    }

    fun importPdf(uri: Uri, onImportSuccess: (Long) -> Unit = {}) {
        onPdfSelected(uri)
    }

    fun clearImportError() {
        _importError.value = null
    }

    fun toggleFavorite(book: BookEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(book.id, book.isFavorite)
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            repository.deleteBook(book)
        }
    }
}
