package com.bignerdranch.android.newsapp

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity
data class NewsFeed(
    @PrimaryKey val id: UUID,
    val title: String,
    val date: Date,
    val wordBank: MutableList<String>,
    val orderNumber: Int
)




