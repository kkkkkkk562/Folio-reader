package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.example.data.local.AnnotationDao
import com.example.data.local.BookDao
import com.example.data.local.BookmarkDao
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookEntity
import com.example.data.model.BookmarkEntity
import com.example.data.sample.SampleBooksGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class BookRepository(
    private val context: Context,
    private val bookDao: BookDao,
    private val annotationDao: AnnotationDao,
    private val bookmarkDao: BookmarkDao
) {

    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()

    suspend fun initializeLibraryIfEmpty() = withContext(Dispatchers.IO) {
        val currentBooks = bookDao.getAllBooks().first()
        if (currentBooks.isEmpty()) {
            val sampleBooks = SampleBooksGenerator.generateSampleBooksIfEmpty(context)
            sampleBooks.forEach { book ->
                bookDao.insertBook(book)
            }
        }
    }

    suspend fun inspectPdfDraft(uri: Uri): Result<PdfImportDraft> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            var fileName = "Document_${System.currentTimeMillis()}.pdf"
            var fileSize = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) {
                        cursor.getString(nameIndex)?.let { if (it.isNotBlank()) fileName = it }
                    }
                    if (sizeIndex >= 0) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            val sanitizedTitle = fileName.removeSuffix(".pdf").replace('_', ' ').replace('-', ' ').trim()
            val fileSizeKb = fileSize / 1024
            val fileSizeFormatted = if (fileSizeKb >= 1024) {
                String.format(Locale.US, "%.1f MB", fileSizeKb / 1024.0)
            } else {
                "$fileSizeKb KB"
            }

            // Detect if title contains manga keywords to provide an intelligent pre-selection
            val lower = sanitizedTitle.lowercase(Locale.ROOT)
            val looksLikeManga = lower.contains("manga") || lower.contains("ch.") || lower.contains("chapter") ||
                    lower.contains("vol.") || lower.contains("volume") || lower.contains("comic") ||
                    lower.contains("webtoon") || lower.contains("manhwa")

            var pageCount = 0
            try {
                contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    val renderer = PdfRenderer(pfd)
                    pageCount = renderer.pageCount
                    renderer.close()
                }
            } catch (ignored: Exception) {}

            Result.success(
                PdfImportDraft(
                    uri = uri,
                    suggestedTitle = sanitizedTitle,
                    suggestedAuthor = if (looksLikeManga) "Manga Artist" else "Author",
                    pageCount = pageCount,
                    fileSizeFormatted = fileSizeFormatted,
                    isManga = looksLikeManga
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun importPdfFromUri(
        uri: Uri,
        isManga: Boolean = false,
        customTitle: String? = null,
        customAuthor: String? = null
    ): Result<BookEntity> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            // Determine file name
            var fileName = "Document_${System.currentTimeMillis()}.pdf"
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) {
                        fileName = name
                    }
                }
            }

            val sanitizedTitle = fileName.removeSuffix(".pdf").replace('_', ' ')
            val booksDir = File(context.filesDir, "books").apply { mkdirs() }
            val coversDir = File(context.filesDir, "covers").apply { mkdirs() }

            val targetPdfFile = File(booksDir, "imported_${System.currentTimeMillis()}_${fileName.filter { it.isLetterOrDigit() || it == '.' }}")

            // Copy file to internal private storage
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetPdfFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Unable to read selected PDF file"))

            val fileSizeKb = targetPdfFile.length() / 1024
            val fileSizeFormatted = if (fileSizeKb >= 1024) {
                String.format(Locale.US, "%.1f MB", fileSizeKb / 1024.0)
            } else {
                "$fileSizeKb KB"
            }

            // Extract page count and render first page cover with clean white background for vibrant colors
            val pfd = ParcelFileDescriptor.open(targetPdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            var coverPath: String? = null

            if (pageCount > 0) {
                val firstPage = renderer.openPage(0)
                val coverBitmap = Bitmap.createBitmap(firstPage.width, firstPage.height, Bitmap.Config.ARGB_8888)
                coverBitmap.eraseColor(android.graphics.Color.WHITE)
                firstPage.render(coverBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                firstPage.close()

                val coverFile = File(coversDir, "cover_${System.currentTimeMillis()}.png")
                FileOutputStream(coverFile).use { out ->
                    coverBitmap.compress(Bitmap.CompressFormat.PNG, 85, out)
                }
                coverPath = coverFile.absolutePath
            }
            renderer.close()
            pfd.close()

            val titleToUse = customTitle?.takeIf { it.isNotBlank() } ?: sanitizedTitle
            val authorToUse = customAuthor?.takeIf { it.isNotBlank() } ?: if (isManga) "Manga Artist" else "Author"

            val book = BookEntity(
                title = titleToUse,
                author = authorToUse,
                filePath = targetPdfFile.absolutePath,
                coverPath = coverPath,
                pageCount = pageCount,
                currentPage = 0,
                percentRead = 0,
                fileSizeFormatted = fileSizeFormatted,
                isManga = isManga,
                lastReadTimestamp = System.currentTimeMillis()
            )

            val id = bookDao.insertBook(book)
            Result.success(book.copy(id = id))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getBookById(id: Long): BookEntity? = withContext(Dispatchers.IO) {
        bookDao.getBookById(id)
    }

    suspend fun updateReadingProgress(bookId: Long, pageIndex: Int, totalPages: Int) = withContext(Dispatchers.IO) {
        val percent = if (totalPages > 1) {
            ((pageIndex.toFloat() / (totalPages - 1).toFloat()) * 100).toInt().coerceIn(0, 100)
        } else 100
        bookDao.updateProgress(bookId, pageIndex, percent)
    }

    suspend fun toggleFavorite(bookId: Long, currentVal: Boolean) = withContext(Dispatchers.IO) {
        bookDao.updateFavorite(bookId, !currentVal)
    }

    suspend fun deleteBook(book: BookEntity) = withContext(Dispatchers.IO) {
        try {
            File(book.filePath).delete()
            book.coverPath?.let { File(it).delete() }
        } catch (ignored: Exception) {}
        bookDao.deleteBookById(book.id)
    }

    // Bookmarks
    fun getBookmarks(bookId: Long): Flow<List<BookmarkEntity>> = bookmarkDao.getBookmarksForBook(bookId)

    suspend fun isPageBookmarked(bookId: Long, pageIndex: Int): Boolean = withContext(Dispatchers.IO) {
        bookmarkDao.getBookmarkForPage(bookId, pageIndex) != null
    }

    suspend fun toggleBookmark(bookId: Long, pageIndex: Int, defaultTitle: String) = withContext(Dispatchers.IO) {
        val existing = bookmarkDao.getBookmarkForPage(bookId, pageIndex)
        if (existing != null) {
            bookmarkDao.removeBookmarkForPage(bookId, pageIndex)
            false
        } else {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    bookId = bookId,
                    pageIndex = pageIndex,
                    title = defaultTitle
                )
            )
            true
        }
    }

    // Annotations
    fun getAnnotationsForBook(bookId: Long): Flow<List<AnnotationEntity>> =
        annotationDao.getAnnotationsForBook(bookId)

    fun getAnnotationsForPage(bookId: Long, pageIndex: Int): Flow<List<AnnotationEntity>> =
        annotationDao.getAnnotationsForPage(bookId, pageIndex)

    suspend fun saveAnnotation(annotation: AnnotationEntity): Long = withContext(Dispatchers.IO) {
        annotationDao.insertAnnotation(annotation)
    }

    suspend fun deleteAnnotation(id: Long) = withContext(Dispatchers.IO) {
        annotationDao.deleteAnnotationById(id)
    }

    suspend fun clearPageAnnotations(bookId: Long, pageIndex: Int) = withContext(Dispatchers.IO) {
        annotationDao.clearPageAnnotations(bookId, pageIndex)
    }
}

data class PdfImportDraft(
    val uri: Uri,
    val suggestedTitle: String,
    val suggestedAuthor: String = "Author",
    val pageCount: Int,
    val fileSizeFormatted: String,
    val isManga: Boolean = false
)
