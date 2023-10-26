package com.bignerdranch.android.newsapp

import androidx.lifecycle.ViewModel
import java.util.Date
import java.util.UUID

class NewsFeedListViewModel : ViewModel() {
    val newsFeeds = mutableListOf<NewsFeed>()
    init {
        for (i in 0 until 100) {
            val newsFeed = NewsFeed(
                id = UUID.randomUUID(),
                title ="Newsfeed #$i",
                date = Date(),
                wordBank = mutableListOf<String>()
            )
            newsFeeds += newsFeed
        }
    }
}