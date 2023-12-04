package com.bignerdranch.android.newsapp.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [SavedArticles::class], version = 1)
@TypeConverters(NewsFeedTypeConverters::class)
abstract class SavedArticlesDatabase : RoomDatabase() {
    abstract fun savedArticlesDao(): SavedArticlesDao
}