package com.bignerdranch.android.newsapp.database

import android.content.Context
import androidx.room.Room
import com.bignerdranch.android.newsapp.models.ReadTimeOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.*

private const val DATABASE_NAME = "newsfeed-database"

class NewsFeedRepository private constructor
    (
    context: Context, private val coroutineScope: CoroutineScope = GlobalScope
) {

    private val database: NewsFeedDatabase = Room
        .databaseBuilder(
            context.applicationContext,
            NewsFeedDatabase::class.java,
            DATABASE_NAME
        )
        //.createFromAsset(DATABASE_NAME)
        .build()

    fun getNewsFeeds(): Flow<List<NewsFeed>> = database.newsfeedDao().getNewsFeeds()

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

    suspend fun updateOrderNumber(newsFeedId: UUID, newOrderNumber: Int) {
        database.newsfeedDao().updateOrderNumber(newsFeedId, newOrderNumber)
    }

    suspend fun initializeSortByOption(newsFeedId: UUID, defaultSortOption: Int) {
        database.newsfeedDao().initializeSortByOption(newsFeedId, defaultSortOption)
    }

    suspend fun updateSortByOption(newsFeedId: UUID, newSortOption: Int) {
        database.newsfeedDao().updateSortByOption(newsFeedId, newSortOption)
    }

    suspend fun updateReadTimeOption(newsFeedId: UUID, newReadTimeOption: MutableList<ReadTimeOption>) {
        database.newsfeedDao().updateReadTimeOption(newsFeedId, newReadTimeOption)
    }

    suspend fun updateDateRelevanceOption(newsFeedId: UUID, newDateRelevanceOption: Int) {
        database.newsfeedDao().updateDateRelevanceOption(newsFeedId, newDateRelevanceOption)
    }

    suspend fun updatePublisherOption(newsFeedId: UUID, newPublisherOption: MutableList<String>) {
        database.newsfeedDao().updatePublisherOption(newsFeedId, newPublisherOption)
    }


    companion object {
        private var INSTANCE: NewsFeedRepository? = null
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = NewsFeedRepository(context)
            }
        }

        fun get(): NewsFeedRepository {
            return INSTANCE ?: throw IllegalStateException("NewsFeedRepository must be initialized")
        }
    }
}