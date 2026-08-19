package com.muktar.mindsetreader

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.github.barteksc.pdfviewer.PDFView
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.content.Intent
import android.net.Uri
import androidx.activity.OnBackPressedCallback
class MainActivity : AppCompatActivity() {
    private lateinit var pdfView: PDFView
    private lateinit var preferences: android.content.SharedPreferences
    private lateinit var readingProgress: ProgressBar
    private lateinit var progressText: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val openPdf = intent.getBooleanExtra("open_pdf", false)
        val fromLibrary = intent.getBooleanExtra("from_library", false)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (pdfView.visibility == View.VISIBLE) {
                    if (fromLibrary) {
                        finish()
                    } else {
                        pdfView.visibility = View.GONE
                        findViewById<LinearLayout>(R.id.homeLayout).visibility = View.VISIBLE
                    }
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        preferences = getSharedPreferences("library", MODE_PRIVATE)

        pdfView = findViewById(R.id.pdfView)

        val pdfUri = intent.getStringExtra("pdf_uri")
            ?: preferences.getString("last_opened_book_uri", null)

        val bookId = pdfUri?.hashCode()?.toString()

        if (pdfUri != null) {
            preferences.edit()
                .putString("last_opened_book_uri", pdfUri)
                .apply()
        }


        readingProgress = findViewById(R.id.readingProgress)
        progressText = findViewById(R.id.progressText)

        val homeBookTitle = findViewById<TextView>(R.id.homeBookTitle)
        val continueReadingCard =
            findViewById<LinearLayout>(R.id.continueReadingCard)

        continueReadingCard.visibility =
            if (pdfUri != null) View.VISIBLE else View.GONE

        if (pdfUri != null) {
            val libraryPreferences = getSharedPreferences("library", MODE_PRIVATE)
            val savedName = libraryPreferences.getString(
                "book_${bookId}_name",
                null
            )

            if (savedName != null) {
                homeBookTitle.text = savedName
            }
        }

        readingProgress.progress = preferences.getInt("book_${bookId}_progress", 0)

        val savedProgress = preferences.getInt("book_${bookId}_progress", 0)
        progressText.text = getString(R.string.reading_progress_percent, savedProgress)


        val continueButton = findViewById<Button>(R.id.continueButton)
        val libraryButton = findViewById<Button>(R.id.libraryButton)
        libraryButton.setOnClickListener {
            val intent = Intent(this, LibraryActivity::class.java)
            startActivity(intent)
        }

        val homeLayout = findViewById<LinearLayout>(R.id.homeLayout)


        continueButton.setOnClickListener {
            val uriToOpen = intent.getStringExtra("pdf_uri")
                ?: preferences.getString("last_opened_book_uri", null)

            val bookIdToOpen = uriToOpen?.hashCode()?.toString()

            homeLayout.visibility = View.GONE
            pdfView.visibility = View.VISIBLE

            val lastPage = preferences.getInt(
                "book_${bookIdToOpen}_last_page",
                0
            )

            if (uriToOpen == null) {
                return@setOnClickListener
            }

            val pdfLoader = pdfView.fromUri(Uri.parse(uriToOpen))

            pdfLoader
                .defaultPage(lastPage)
                .onPageChange { page, pageCount ->
                    preferences.edit()
                        .putInt("book_${bookIdToOpen}_last_page", page)
                        .apply()

                    val progress = ((page + 1) * 100) / pageCount

                    readingProgress.progress = progress

                    preferences.edit()
                        .putInt("book_${bookIdToOpen}_progress", progress)
                        .apply()

                    progressText.text = getString(
                        R.string.reading_progress_percent,
                        progress
                    )
                }
                .load()
        }

        if (openPdf) {
            continueButton.performClick()
        }
    }
    override fun onResume() {
        super.onResume()

        val homeBookTitle = findViewById<TextView>(R.id.homeBookTitle)
        val dailyQuoteText = findViewById<TextView>(R.id.dailyQuoteText)

        val quotes = resources.getStringArray(R.array.reading_quotes)

        val dayOfYear = java.util.Calendar.getInstance()
            .get(java.util.Calendar.DAY_OF_YEAR)

        dailyQuoteText.text = quotes[dayOfYear % quotes.size]

        val continueReadingCard =
            findViewById<LinearLayout>(R.id.continueReadingCard)

        val libraryPreferences =
            getSharedPreferences("library", MODE_PRIVATE)

        val lastOpenedUri =
            libraryPreferences.getString("last_opened_book_uri", null)

        continueReadingCard.visibility =
            if (lastOpenedUri != null) View.VISIBLE else View.GONE

        if (lastOpenedUri != null) {
            val lastBookId = lastOpenedUri.hashCode().toString()

            val latestProgress = libraryPreferences.getInt(
                "book_${lastBookId}_progress",
                0
            )

            readingProgress.progress = latestProgress

            progressText.text = getString(
                R.string.reading_progress_percent,
                latestProgress
            )
            val savedName = libraryPreferences.getString(
                "book_${lastBookId}_name",
                null
            )

            if (savedName != null) {
                homeBookTitle.text = savedName
                    .substringAfterLast("/")
                    .removePrefix("raw:")
                    .removeSuffix(".pdf")
                    .removeSuffix(".PDF")
                    .trim()
            }
        }

    }


}