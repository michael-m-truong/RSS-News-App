package com.bignerdranch.android.newsapp

import java.util.Date
import java.util.UUID

data class NewsFeed(
    val id: UUID,
    val title: String,
    val date: Date,
    val wordBank: MutableList<String>
)




