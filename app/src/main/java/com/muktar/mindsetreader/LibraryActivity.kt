package com.muktar.mindsetreader

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
class LibraryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)
        val mindsetBookButton = findViewById<Button>(R.id.mindsetBookButton)

        mindsetBookButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("open_pdf", true)
            startActivity(intent)
        }
    }
}