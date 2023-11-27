package com.bignerdranch.android.newsapp.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class NewsFeed(
    @PrimaryKey val id: UUID,
    val title: String,
    val date: Date,
    val wordBank: MutableList<String>,  // exact match word bank
    val excludeWordBank: MutableList<String>,
    val orderNumber: Int,
    val sortByOption: Int,
)




