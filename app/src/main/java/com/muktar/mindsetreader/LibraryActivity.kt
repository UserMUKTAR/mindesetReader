package com.muktar.mindsetreader

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.net.Uri
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.OpenableColumns
import androidx.appcompat.app.AlertDialog
import android.widget.EditText
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner

class LibraryActivity : AppCompatActivity() {

    private lateinit var pdfLibraryContainer: LinearLayout

    private val pdfPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }

                val bookName = contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                        ).removeSuffix(".pdf")
                            .removeSuffix(".PDF")
                    } else {
                        "PDF"
                    }
                } ?: "PDF"

                val bookId = uri.toString().hashCode().toString()
                val book = PdfBook(
                    id = bookId,
                    name = bookName,
                    uri = uri.toString(),
                    addedAt = System.currentTimeMillis()
                )
                saveBook(book)
                addBookButton(book)
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        val addPdfButton = findViewById<Button>(R.id.addPdfButton)
        pdfLibraryContainer =
            findViewById(R.id.pdfLibraryContainer)

        val librarySearch =
            findViewById<EditText>(R.id.librarySearch)

        val librarySortSpinner =
            findViewById<Spinner>(R.id.librarySortSpinner)
        val sortAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.library_sort_options,
            android.R.layout.simple_spinner_item
        )

        sortAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        librarySortSpinner.adapter = sortAdapter

        librarySortSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    loadBooks()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }

        librarySearch.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                filterBooks(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        addPdfButton.setOnClickListener {
            pdfPicker.launch(arrayOf("application/pdf"))
        }

        loadBooks()
    }

    override fun onResume() {
        super.onResume()

        if (::pdfLibraryContainer.isInitialized) {
            pdfLibraryContainer.removeAllViews()
            loadBooks()
        }
    }
    private fun saveBook(book: PdfBook) {
        val preferences = getSharedPreferences("library", MODE_PRIVATE)

        preferences.edit()
            .putString("book_${book.id}_name", book.name)
            .putString("book_${book.id}_uri", book.uri)
            .putInt("book_${book.id}_last_page", book.lastPage)
            .putInt("book_${book.id}_progress", book.progress)
            .putLong("book_${book.id}_added_at", book.addedAt)
            .putLong("book_${book.id}_last_read_at", book.lastReadAt)
            .apply()
    }
    private fun getDisplayName(uriString: String, fallbackName: String): String {
        val uri = Uri.parse(uriString)

        val displayName = try {
            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                    )
                } else {
                    null
                }
            }
        } catch (e: SecurityException) {
            null
        }

        return (displayName ?: fallbackName)
            .removeSuffix(".pdf")
            .removeSuffix(".PDF")
            .trim()
    }

    private fun sortBooks(books: MutableList<PdfBook>): List<PdfBook> {
        val sortPosition =
            findViewById<Spinner>(R.id.librarySortSpinner).selectedItemPosition

        return when (sortPosition) {
            0 -> books.sortedByDescending { it.lastReadAt }
            1 -> books.sortedByDescending { it.addedAt }
            2 -> books.sortedBy { it.name.lowercase() }
            3 -> books.sortedByDescending { it.progress }
            else -> books
        }
    }

    private fun loadBooks() {
        val preferences = getSharedPreferences("library", MODE_PRIVATE)
        pdfLibraryContainer.removeAllViews()
        val books = mutableListOf<PdfBook>()

        for ((key, value) in preferences.all) {
            if (key.startsWith("book_") && key.endsWith("_name")) {
                val bookId = key.removePrefix("book_").removeSuffix("_name")

                val storedName = value as? String ?: continue
                val uri = preferences.getString("book_${bookId}_uri", null) ?: continue
                val name = getDisplayName(uri, storedName)

                val lastPage = preferences.getInt("book_${bookId}_last_page", 0)
                val progress = preferences.getInt("book_${bookId}_progress", 0)

                val pageCount = preferences.getInt(
                    "book_${bookId}_page_count",
                    0
                )

                val storedAddedAt = preferences.getLong(
                    "book_${bookId}_added_at",
                    0L
                )

                val addedAt = if (storedAddedAt > 0L) {
                    storedAddedAt
                } else {
                    1L
                }

                val lastReadAt = preferences.getLong(
                    "book_${bookId}_last_read_at",
                    0L
                )

                books.add(
                    PdfBook(
                        id = bookId,
                        name = name,
                        uri = uri,
                        lastPage = lastPage,
                        progress = progress,
                        pageCount = pageCount,
                        addedAt = addedAt,
                        lastReadAt = lastReadAt
                    )
                )
            }
        }
        sortBooks(books).forEach { book ->
            addBookButton(book)
        }
    }

    private fun filterBooks(query: String) {
        val searchQuery = query.trim().lowercase()

        pdfLibraryContainer.removeAllViews()

        val preferences = getSharedPreferences("library", MODE_PRIVATE)
        val books = mutableListOf<PdfBook>()

        for ((key, value) in preferences.all) {
            if (key.startsWith("book_") && key.endsWith("_name")) {

                val bookId =
                    key.removePrefix("book_").removeSuffix("_name")

                val storedName = value as? String ?: continue

                val uri =
                    preferences.getString("book_${bookId}_uri", null)
                        ?: continue

                val name = getDisplayName(uri, storedName)

                if (searchQuery.isEmpty() ||
                    name.lowercase().contains(searchQuery)
                ) {
                    val lastPage =
                        preferences.getInt("book_${bookId}_last_page", 0)

                    val progress =
                        preferences.getInt("book_${bookId}_progress", 0)

                    val pageCount = preferences.getInt(
                        "book_${bookId}_page_count",
                        0
                    )

                    val addedAt = preferences.getLong(
                        "book_${bookId}_added_at",
                        1L
                    )

                    val lastReadAt = preferences.getLong(
                        "book_${bookId}_last_read_at",
                        0L
                    )

                    books.add(
                        PdfBook(
                            id = bookId,
                            name = name,
                            uri = uri,
                            lastPage = lastPage,
                            progress = progress,
                            pageCount = pageCount,
                            addedAt = addedAt,
                            lastReadAt = lastReadAt
                        )
                    )
                }
            }
        }
        sortBooks(books).forEach { book ->
            addBookButton(book)
        }
    }

    private fun addBookButton(book: PdfBook) {
        val itemView = layoutInflater.inflate(
            R.layout.item_pdf_book,
            pdfLibraryContainer,
            false
        )

        val bookName = itemView.findViewById<TextView>(R.id.pdfBookName)
        val progressBar = itemView.findViewById<ProgressBar>(R.id.pdfBookProgress)
        val progressText = itemView.findViewById<TextView>(R.id.pdfBookProgressText)

        bookName.text = book.name
        progressBar.progress = book.progress
        progressText.text =
            if (book.pageCount > 0) {
                "${book.progress}% · Page ${book.lastPage + 1} of ${book.pageCount}"
            } else {
                getString(
                    R.string.reading_progress_percent,
                    book.progress
                )
            }

        val resumeButton =
            itemView.findViewById<Button>(R.id.pdfBookResumeButton)

        resumeButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("pdf_uri", book.uri)
            intent.putExtra("open_pdf", true)
            intent.putExtra("from_library", true)
            startActivity(intent)
        }

        val deleteButton =
            itemView.findViewById<Button>(R.id.pdfBookDeleteButton)

        deleteButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete book?")
                .setMessage("Are you sure you want to remove \"${book.name}\" from your library?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    val preferences = getSharedPreferences("library", MODE_PRIVATE)
                    val lastOpenedUri = preferences.getString("last_opened_book_uri", null)

                    if (lastOpenedUri == book.uri) {
                        preferences.edit()
                            .remove("last_opened_book_uri")
                            .apply()
                    }

                    preferences.edit()
                        .remove("book_${book.id}_name")
                        .remove("book_${book.id}_uri")
                        .remove("book_${book.id}_last_page")
                        .remove("book_${book.id}_progress")
                        .remove("book_${book.id}_added_at")
                        .remove("book_${book.id}_last_read_at")
                        .remove("book_${book.id}_page_count")
                        .apply()

                    pdfLibraryContainer.removeView(itemView)                }
                .show()
        }

        pdfLibraryContainer.addView(itemView)
    }
}