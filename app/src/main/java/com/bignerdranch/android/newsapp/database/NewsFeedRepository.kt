package com.bignerdranch.android.newsapp.database

import android.content.Context
import androidx.room.Room
import com.bignerdranch.android.newsapp.NewsFeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

private const val DATABASE_NAME = "newsfeed-database"

class NewsFeedRepository private constructor
    (context: Context, private val coroutineScope: CoroutineScope = GlobalScope
) {

    private val database: NewsFeedDatabase = Room
        .databaseBuilder(
            context.applicationContext,
            NewsFeedDatabase::class.java,
            DATABASE_NAME
        )
        //.createFromAsset(DATABASE_NAME)
        .build()

    fun getNewsFeeds(): Flow<List<NewsFeed>>
            = database.newsfeedDao().getNewsFeeds()

    suspend fun getNewsFeed(id: UUID): NewsFeed = database.newsfeedDao().getNewsFeed(id)

    fun updateNewsFeed(newsFeed: NewsFeed) {
        coroutineScope.launch {
            database.newsfeedDao().updateNewsFeed(newsFeed)
        }
    }

    suspend fun addNewsFeed(newsFeed: NewsFeed) {
        database.newsfeedDao().addCrime(newsFeed)
    }

    suspend fun deleteNewsFeed(id: UUID) {
        database.newsfeedDao().deleteNewsFeed(id)
    }

    suspend fun swapOrderNumbers(newsFeedId1: UUID, newsFeedOrderNumber1: Int, newsFeedId2: UUID, newsFeedOrderNumber2: Int) {
        database.newsfeedDao().swapOrderNumbers(newsFeedId1, newsFeedOrderNumber2)
        database.newsfeedDao().swapOrderNumbers(newsFeedId2, newsFeedOrderNumber1)
    }

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