package com.muktar.mindsetreader.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey
    val id: String,
    val uri: String,
    val name: String,
    @ColumnInfo(name = "last_page")
    val lastPage: Int = 0,
    val progress: Int = 0,
    @ColumnInfo(name = "page_count")
    val pageCount: Int = 0,
    @ColumnInfo(name = "added_at")
    val addedAt: Long = 0L,
    @ColumnInfo(name = "last_read_at")
    val lastReadAt: Long = 0L
)
