package com.bignerdranch.android.newsapp

import android.app.Application
import com.bignerdranch.android.newsapp.database.NewsFeedRepository
import com.bignerdranch.android.newsapp.database.SavedArticlesRepository

class NewsFeedApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NewsFeedRepository.initialize(this)
        SavedArticlesRepository.initialize(this)
    }
}