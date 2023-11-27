package com.bignerdranch.android.newsapp.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [NewsFeed::class], version = 1)
@TypeConverters(NewsFeedTypeConverters::class)
abstract class NewsFeedDatabase : RoomDatabase() {
    abstract fun newsfeedDao(): NewsFeedDao
}