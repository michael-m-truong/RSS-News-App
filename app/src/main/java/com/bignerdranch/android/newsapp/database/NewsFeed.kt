package com.bignerdranch.android.newsapp.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bignerdranch.android.newsapp.models.DateRelevance
import com.bignerdranch.android.newsapp.models.ReadTimeOption
import java.util.*
import kotlin.collections.HashMap

@Entity
data class NewsFeed(
    @PrimaryKey val id: UUID,
    val title: String,
    val date: Date,
    val wordBank: MutableList<String>,  // exact match word bank
    val excludeWordBank: MutableList<String>,
    val orderNumber: Int,
    val sortByOption: Int,
    val readTimeOption: MutableList<ReadTimeOption>,
    val dateRelevanceOption: Int,
    val publisherOption: MutableList<String>,
    val sourceOption: HashMap<String, Boolean>
)




