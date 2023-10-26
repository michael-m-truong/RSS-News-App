package com.bignerdranch.android.newsapp.database

import android.content.Context
import androidx.room.Room
import com.bignerdranch.android.newsapp.NewsFeed
import kotlinx.coroutines.flow.Flow
import java.util.UUID

private const val DATABASE_NAME = "newsfeed-database"

class NewsFeedRepository private constructor(context: Context) {

    private val database: NewsFeedDatabase = Room
        .databaseBuilder(
            context.applicationContext,
            NewsFeedDatabase::class.java,
            DATABASE_NAME
        )
        .createFromAsset(DATABASE_NAME)
        .build()

    fun getNewsFeeds(): Flow<List<NewsFeed>>
            = database.newsfeedDao().getNewsFeeds()

    suspend fun getNewsFeed(id: UUID): NewsFeed = database.newsfeedDao().getNewsFeed(id)


    companion object {
        private var INSTANCE: NewsFeedRepository? = null
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = NewsFeedRepository(context)
            }
        }
        fun get(): NewsFeedRepository {
            return INSTANCE ?:
            throw IllegalStateException("NewsFeedRepository must be initialized")
        }
    }
}