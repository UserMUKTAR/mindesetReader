package com.muktar.mindsetreader

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.github.barteksc.pdfviewer.PDFView

class MainActivity : AppCompatActivity() {
    private lateinit var pdfView: PDFView
    private lateinit var preferences: android.content.SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preferences = getSharedPreferences("reader", MODE_PRIVATE)

        pdfView = findViewById(R.id.pdfView)
        val lastPage = preferences.getInt("last_page", 0)

        pdfView.fromAsset("mindset.pdf")
            .defaultPage(lastPage)
            .onPageChange { page, pageCount ->
                preferences.edit()
                    .putInt("last_page", page)
                    .apply()
            }
            .load()

    }
}