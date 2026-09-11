package com.muktar.mindsetreader.data

import android.content.Context
import com.muktar.mindsetreader.PdfBook
import com.muktar.mindsetreader.data.local.AppDatabase
import com.muktar.mindsetreader.data.local.BookDao
import com.muktar.mindsetreader.data.local.BookEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class BookRepository(
    private val bookDao: BookDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    val allBooks: Flow<List<PdfBook>> = bookDao.observeAllBooks().map { entities ->
        entities.map { it.toPdfBook() }
    }

    suspend fun getBookById(id: String): PdfBook? = withContext(ioDispatcher) {
        bookDao.getBookById(id)?.toPdfBook()
    }

    suspend fun isBookDuplicate(id: String): Boolean = withContext(ioDispatcher) {
        bookDao.getBookById(id) != null
    }

    suspend fun insertBook(book: PdfBook) = withContext(ioDispatcher) {
        bookDao.insertBook(book.toEntity())
    }

    suspend fun updateProgress(
        id: String,
        lastPage: Int,
        pageCount: Int,
        progress: Int,
        lastReadAt: Long
    ) = withContext(ioDispatcher) {
        bookDao.updateProgress(id, lastPage, pageCount, progress, lastReadAt)
    }

    suspend fun deleteBook(id: String) = withContext(ioDispatcher) {
        bookDao.deleteBook(id)
    }

    companion object {
        @Volatile
        private var INSTANCE: BookRepository? = null

        fun getRepository(context: Context): BookRepository {
            return INSTANCE ?: synchronized(this) {
                val database = AppDatabase.getDatabase(context)
                val instance = BookRepository(database.bookDao())
                INSTANCE = instance
                instance
            }
        }
    }
}

fun BookEntity.toPdfBook(): PdfBook = PdfBook(
    id = id,
    name = name,
    uri = uri,
    lastPage = lastPage,
    progress = progress,
    pageCount = pageCount,
    addedAt = addedAt,
    lastReadAt = lastReadAt
)

fun PdfBook.toEntity(): BookEntity = BookEntity(
    id = id,
    uri = uri,
    name = name,
    lastPage = lastPage,
    progress = progress,
    pageCount = pageCount,
    addedAt = addedAt,
    lastReadAt = lastReadAt
)
