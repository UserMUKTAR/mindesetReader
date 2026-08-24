package com.muktar.mindsetreader

data class PdfBook(
    val id: String,
    val name: String,
    val uri: String,
    val lastPage: Int = 0,
    val progress: Int = 0,
    val pageCount: Int = 0,
    val addedAt: Long = 0,
    val lastReadAt: Long = 0
)