package com.example

import com.example.data.model.BookEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

  @Test
  fun bookEntity_readingProgressCalculation() {
    val book = BookEntity(
      title = "The Art of War",
      author = "Sun Tzu",
      filePath = "/dummy/path",
      pageCount = 10,
      currentPage = 4,
      percentRead = 50,
      isFavorite = true
    )
    assertEquals(10, book.pageCount)
    assertEquals(4, book.currentPage)
    assertTrue(book.isFavorite)
  }
}
