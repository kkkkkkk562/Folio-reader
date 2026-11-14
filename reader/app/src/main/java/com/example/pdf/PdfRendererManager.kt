package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class PdfRendererManager(private val context: Context) {

    private var currentFile: File? = null
    private var pfd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private val mutex = Mutex()

    // Cache up to 12 rendered page bitmaps in memory for instant flipping
    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(16) {
        override fun entryRemoved(evicted: Boolean, key: String?, oldValue: Bitmap?, newValue: Bitmap?) {
            // Do not manually recycle here if Compose might still be referencing the bitmap in the composition
        }
    }

    var pageCount: Int = 0
        private set

    suspend fun openDocument(filePath: String): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = File(filePath)
            if (currentFile?.absolutePath == file.absolutePath && renderer != null) {
                return@withContext pageCount
            }

            closeInternal()

            currentFile = file
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val newRenderer = PdfRenderer(pfd!!)
            renderer = newRenderer
            pageCount = newRenderer.pageCount
            memoryCache.evictAll()
            pageCount
        }
    }

    suspend fun renderPage(pageIndex: Int, targetWidth: Int = 1080): Bitmap? = withContext(Dispatchers.IO) {
        if (pageIndex < 0 || pageIndex >= pageCount) return@withContext null

        val cacheKey = "${currentFile?.absolutePath}_${pageIndex}_$targetWidth"
        memoryCache.get(cacheKey)?.let { cached ->
            if (!cached.isRecycled) return@withContext cached
        }

        mutex.withLock {
            val rend = renderer ?: return@withContext null
            try {
                val page = rend.openPage(pageIndex)
                val originalWidth = page.width
                val originalHeight = page.height

                // Calculate scaled dimensions to ensure crisp rendering at high DPI
                val scale = if (targetWidth > 0) {
                    (targetWidth.toFloat() / originalWidth.toFloat()).coerceIn(1.0f, 3.0f)
                } else {
                    1.5f
                }

                val renderWidth = (originalWidth * scale).toInt().coerceAtLeast(1)
                val renderHeight = (originalHeight * scale).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                // Fill with white first so transparent vector diagrams, charts, color photos, and text render with 100% color accuracy
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                memoryCache.put(cacheKey, bitmap)
                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        mutex.withLock {
            closeInternal()
        }
    }

    private fun closeInternal() {
        try {
            renderer?.close()
        } catch (ignored: Exception) {}
        try {
            pfd?.close()
        } catch (ignored: Exception) {}
        renderer = null
        pfd = null
        currentFile = null
        pageCount = 0
    }
}
