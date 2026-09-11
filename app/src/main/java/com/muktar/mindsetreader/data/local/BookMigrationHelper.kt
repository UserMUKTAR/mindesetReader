package com.muktar.mindsetreader.data.local

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class BookMigrationHelper(
    private val context: Context,
    private val bookDao: BookDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun migrateIfNeeded(): Boolean = withContext(ioDispatcher) {
        migrationMutex.withLock {
            val preferences = context.getSharedPreferences("library", Context.MODE_PRIVATE)

            if (preferences.getBoolean(KEY_MIGRATION_COMPLETED, false)) {
                return@withLock true
            }

            val allEntries = try {
                preferences.all
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read SharedPreferences for Room migration", e)
                return@withLock false
            }

            val booksToMigrate = mutableListOf<BookEntity>()

            for ((key, value) in allEntries) {
                if (key.startsWith("book_") && key.endsWith("_name")) {
                    val bookId = key.removePrefix("book_").removeSuffix("_name")
                    if (bookId.isBlank()) {
                        continue
                    }

                    try {
                        val storedName = value as? String ?: continue
                        val uri = preferences.getString("book_${bookId}_uri", null)
                        if (uri.isNullOrBlank()) {
                            continue
                        }

                        val name = storedName.ifBlank { "PDF" }
                        val lastPage = preferences.getInt("book_${bookId}_last_page", 0)
                        val progress = preferences.getInt("book_${bookId}_progress", 0)
                        val pageCount = preferences.getInt("book_${bookId}_page_count", 0)
                        val storedAddedAt = preferences.getLong("book_${bookId}_added_at", 0L)
                        val addedAt = if (storedAddedAt > 0L) storedAddedAt else 1L
                        val lastReadAt = preferences.getLong("book_${bookId}_last_read_at", 0L)

                        booksToMigrate.add(
                            BookEntity(
                                id = bookId,
                                uri = uri,
                                name = name,
                                lastPage = lastPage,
                                progress = progress,
                                pageCount = pageCount,
                                addedAt = addedAt,
                                lastReadAt = lastReadAt
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipping malformed book record for ID: $bookId", e)
                    }
                }
            }

            try {
                for (book in booksToMigrate) {
                    bookDao.insertBook(book)
                }

                preferences.edit()
                    .putBoolean(KEY_MIGRATION_COMPLETED, true)
                    .apply()

                Log.i(
                    TAG,
                    "Room migration completed successfully. Migrated ${booksToMigrate.size} books."
                )
                return@withLock true
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Database error during Room migration. Marker will not be set so it can retry.",
                    e
                )
                return@withLock false
            }
        }
    }

    fun migrateAsync(scope: CoroutineScope = CoroutineScope(ioDispatcher)) {
        scope.launch {
            migrateIfNeeded()
        }
    }

    companion object {
        private const val TAG = "BookMigrationHelper"
        const val KEY_MIGRATION_COMPLETED = "room_migration_completed"
        private val migrationMutex = Mutex()

        fun migrateIfNeededAsync(context: Context) {
            val appContext = context.applicationContext
            val database = AppDatabase.getDatabase(appContext)
            val helper = BookMigrationHelper(appContext, database.bookDao())
            helper.migrateAsync()
        }
    }
}
