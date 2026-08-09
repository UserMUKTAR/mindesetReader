package com.muktar.mindsetreader

data class PdfBook(
    val id: String,
    val name: String,
    val uri: String,
    val lastPage: Int = 0,
    val progress: Int = 0
)