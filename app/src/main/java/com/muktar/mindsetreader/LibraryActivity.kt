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
class LibraryActivity : AppCompatActivity() {

    private lateinit var pdfLibraryContainer: LinearLayout

    private val pdfPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                getSharedPreferences("library", MODE_PRIVATE)
                    .edit()
                    .putString("last_pdf_uri", uri.toString())
                    .apply()

                val bookName = uri.lastPathSegment ?: "PDF"
                val bookId = uri.toString().hashCode().toString()
                val book = PdfBook(
                    id = bookId,
                    name = bookName,
                    uri = uri.toString()
                )
                saveBook(book)
                addBookButton(book)

                val bookButton = Button(this)
                bookButton.text = bookName
                bookButton.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                bookButton.setOnClickListener {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("pdf_uri", uri.toString())
                    intent.putExtra("open_pdf", true)
                    startActivity(intent)
                }

                pdfLibraryContainer.addView(bookButton)

            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        val addPdfButton = findViewById<Button>(R.id.addPdfButton)
        pdfLibraryContainer =
            findViewById(R.id.pdfLibraryContainer)

        addPdfButton.setOnClickListener {
            pdfPicker.launch(arrayOf("application/pdf"))
        }


        val preferences = getSharedPreferences("library", MODE_PRIVATE)

        if (!preferences.getBoolean("mindset_added", false)) {
            val mindsetBook = PdfBook(
                id = "mindset",
                name = getString(R.string.book_title),
                uri = "asset://mindset.pdf"
            )

            saveBook(mindsetBook)

            preferences.edit()
                .putBoolean("mindset_added", true)
                .apply()
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
            .apply()
    }

    private fun loadBooks() {
        val preferences = getSharedPreferences("library", MODE_PRIVATE)

        for ((key, value) in preferences.all) {
            if (key.startsWith("book_") && key.endsWith("_name")) {
                val bookId = key.removePrefix("book_").removeSuffix("_name")

                val name = value as? String ?: continue
                val uri = preferences.getString("book_${bookId}_uri", null) ?: continue
                val lastPage = preferences.getInt("book_${bookId}_last_page", 0)
                val progress = preferences.getInt("book_${bookId}_progress", 0)

                addBookButton(
                    PdfBook(
                        id = bookId,
                        name = name,
                        uri = uri,
                        lastPage = lastPage,
                        progress = progress
                    )
                )
            }
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
        progressText.text = getString(
            R.string.reading_progress_percent,
            book.progress
        )

        val resumeButton =
            itemView.findViewById<Button>(R.id.pdfBookResumeButton)

        resumeButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("pdf_uri", book.uri)
            intent.putExtra("open_pdf", true)
            intent.putExtra("from_library", true)
            startActivity(intent)
        }

        pdfLibraryContainer.addView(itemView)
    }
}