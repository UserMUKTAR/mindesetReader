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
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.muktar.mindsetreader.data.AppPreferencesRepository
import com.muktar.mindsetreader.data.BookRepository
import com.muktar.mindsetreader.data.local.BookMigrationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var pdfView: PDFView
    private lateinit var preferences: android.content.SharedPreferences
    private lateinit var readingProgress: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var pdfScreen: FrameLayout
    private lateinit var homeLayout: LinearLayout
    private lateinit var repository: BookRepository
    private lateinit var preferencesRepository: AppPreferencesRepository
    private var currentLastOpenedUri: String? = null
    private var lastProgressJob: Job? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = BookRepository.getRepository(this)
        preferencesRepository = AppPreferencesRepository.getRepository(this)

        BookMigrationHelper.migrateIfNeededAsync(this)

        val openPdf = intent.getBooleanExtra("open_pdf", false)
        val fromLibrary = intent.getBooleanExtra("from_library", false)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            private var isFinishingFromReader = false

            override fun handleOnBackPressed() {
                if (pdfScreen.visibility == View.VISIBLE) {
                    if (fromLibrary) {
                        if (isFinishingFromReader) return
                        isFinishingFromReader = true
                        lifecycleScope.launch {
                            try {
                                lastProgressJob?.join()
                            } finally {
                                finish()
                            }
                        }
                    } else {
                        pdfScreen.visibility = View.GONE
                        findViewById<LinearLayout>(R.id.homeLayout).visibility = View.VISIBLE
                    }
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        preferences = getSharedPreferences("library", MODE_PRIVATE)

        pdfScreen = findViewById(R.id.pdfScreen)
        homeLayout = findViewById(R.id.homeLayout)
        pdfView = findViewById(R.id.pdfView)
        readingProgress = findViewById(R.id.readingProgress)
        progressText = findViewById(R.id.progressText)

        val incomingPdfUri = intent.getStringExtra("pdf_uri")
        val savedUri = savedInstanceState?.getString("last_opened_book_uri")

        if (incomingPdfUri != null) {
            currentLastOpenedUri = incomingPdfUri
            updateContinueReadingUI(incomingPdfUri)
            lifecycleScope.launch {
                preferencesRepository.setLastOpenedBookUri(incomingPdfUri)
            }
        } else if (savedUri != null) {
            currentLastOpenedUri = savedUri
            updateContinueReadingUI(savedUri)
        }

        lifecycleScope.launch {
            preferencesRepository.lastOpenedBookUri.collect { uri ->
                if (incomingPdfUri != null && uri != incomingPdfUri) {
                    return@collect
                }
                currentLastOpenedUri = uri
                updateContinueReadingUI(uri)
            }
        }

        val continueButton = findViewById<Button>(R.id.continueButton)
        val libraryButton = findViewById<Button>(R.id.libraryButton)
        libraryButton.setOnClickListener {
            val intent = Intent(this, LibraryActivity::class.java)
            startActivity(intent)
        }

        continueButton.setOnClickListener {
            val uriToOpen = currentLastOpenedUri ?: return@setOnClickListener
            openBook(uriToOpen)
        }

        val isReaderOpen = savedInstanceState?.getBoolean("is_reader_open", false) ?: false

        if (openPdf && incomingPdfUri != null) {
            openBook(incomingPdfUri)
        } else if (isReaderOpen) {
            val uriToRestore = savedUri ?: currentLastOpenedUri
            if (uriToRestore != null) {
                openBook(uriToRestore)
            }
        }
    }

    private fun openBook(uriToOpen: String) {
        val bookIdToOpen = uriToOpen.hashCode().toString()

        homeLayout.visibility = View.GONE
        pdfScreen.visibility = View.VISIBLE

        val lastPage = preferences.getInt(
            "book_${bookIdToOpen}_last_page",
            0
        )

        val pdfLoader = pdfView.fromUri(Uri.parse(uriToOpen))

        pdfLoader
            .defaultPage(lastPage)
            .onPageChange { page, pageCount ->
                preferences.edit()
                    .putInt("book_${bookIdToOpen}_last_page", page)
                    .apply()
                preferences.edit()
                    .putInt("book_${bookIdToOpen}_page_count", pageCount)
                    .apply()

                val progress = if (pageCount > 0) ((page + 1) * 100) / pageCount else 0

                readingProgress.progress = progress

                preferences.edit()
                    .putInt("book_${bookIdToOpen}_progress", progress)
                    .apply()

                val lastReadAt = System.currentTimeMillis()

                preferences.edit()
                    .putLong(
                        "book_${bookIdToOpen}_last_read_at",
                        lastReadAt
                    )
                    .apply()

                lastProgressJob = lifecycleScope.launch {
                    repository.updateProgress(
                        id = bookIdToOpen,
                        lastPage = page,
                        pageCount = pageCount,
                        progress = progress,
                        lastReadAt = lastReadAt
                    )
                }

                progressText.text = getString(
                    R.string.reading_progress_with_page,
                    progress,
                    page + 1,
                    pageCount
                )
            }
            .onError {
                pdfScreen.visibility = View.GONE
                homeLayout.visibility = View.VISIBLE
                Toast.makeText(
                    this,
                    "Cannot open PDF: file missing or inaccessible",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .load()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("is_reader_open", pdfScreen.visibility == View.VISIBLE)
        outState.putString("last_opened_book_uri", currentLastOpenedUri)
    }
    override fun onResume() {
        super.onResume()

        val dailyQuoteText = findViewById<TextView>(R.id.dailyQuoteText)

        val quotes = resources.getStringArray(R.array.reading_quotes)

        val dayOfYear = java.util.Calendar.getInstance()
            .get(java.util.Calendar.DAY_OF_YEAR)

        dailyQuoteText.text = quotes[dayOfYear % quotes.size]

        updateContinueReadingUI(currentLastOpenedUri)
    }

    private fun updateContinueReadingUI(uri: String?) {
        val continueReadingCard =
            findViewById<LinearLayout>(R.id.continueReadingCard)

        if (uri == null) {
            continueReadingCard.visibility = View.GONE
            return
        }

        val bookId = uri.hashCode().toString()
        val libraryPreferences =
            getSharedPreferences("library", MODE_PRIVATE)
        val savedName = libraryPreferences.getString(
            "book_${bookId}_name",
            null
        )

        if (savedName == null) {
            continueReadingCard.visibility = View.GONE
            return
        }

        continueReadingCard.visibility = View.VISIBLE

        val homeBookTitle = findViewById<TextView>(R.id.homeBookTitle)
        homeBookTitle.text = savedName
            .substringAfterLast("/")
            .removePrefix("raw:")
            .removeSuffix(".pdf")
            .removeSuffix(".PDF")
            .trim()

        val latestProgress = libraryPreferences.getInt(
            "book_${bookId}_progress",
            0
        )
        val lastPage = libraryPreferences.getInt(
            "book_${bookId}_last_page",
            0
        )
        val pageCount = libraryPreferences.getInt(
            "book_${bookId}_page_count",
            0
        )

        readingProgress.progress = latestProgress

        progressText.text =
            if (pageCount > 0) {
                "$latestProgress% · Page ${lastPage + 1} of $pageCount"
            } else {
                getString(
                    R.string.reading_progress_percent,
                    latestProgress
                )
            }
    }

}