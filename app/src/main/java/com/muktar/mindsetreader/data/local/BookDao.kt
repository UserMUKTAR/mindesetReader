package com.muktar.mindsetreader.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books")
    fun observeAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getBookById(id: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Query(
        """
        UPDATE books 
        SET last_page = :lastPage, 
            page_count = :pageCount, 
            progress = :progress, 
            last_read_at = :lastReadAt 
        WHERE id = :id
        """
    )
    suspend fun updateProgress(
        id: String,
        lastPage: Int,
        pageCount: Int,
        progress: Int,
        lastReadAt: Long
    )

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBook(id: String)
}
