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

class MainActivity : AppCompatActivity() {
    private lateinit var pdfView: PDFView
    private lateinit var preferences: android.content.SharedPreferences
    private lateinit var readingProgress: ProgressBar
    private lateinit var progressText: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preferences = getSharedPreferences("reader", MODE_PRIVATE)

        pdfView = findViewById(R.id.pdfView)
        val openPdf = intent.getBooleanExtra("open_pdf", false)
        val pdfUri = intent.getStringExtra("pdf_uri")
        val bookId = pdfUri?.hashCode()?.toString() ?: "mindset"
        readingProgress = findViewById(R.id.readingProgress)
        progressText = findViewById(R.id.progressText)
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
            homeLayout.visibility = View.GONE
            pdfView.visibility = View.VISIBLE

            val lastPage = preferences.getInt("book_${bookId}_last_page", 0)

            val pdfLoader = if (pdfUri != null) {
                pdfView.fromUri(Uri.parse(pdfUri))
            } else {
                pdfView.fromAsset("mindset.pdf")
            }

            pdfLoader
                .defaultPage(lastPage)
                .onPageChange { page, pageCount ->
                    preferences.edit()
                        .putInt("book_${bookId}_last_page", page)
                        .apply()

                    val progress = ((page + 1) * 100) / pageCount
                    readingProgress.progress = progress
                    preferences.edit()
                        .putInt("book_${bookId}_progress", progress)
                        .apply()

                    progressText.text = getString(R.string.reading_progress_percent, progress)                }

                .load()
        }
        if (openPdf) {
            continueButton.performClick()
        }
    }
}