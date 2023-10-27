package com.bignerdranch.android.newsapp.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import com.bignerdranch.android.newsapp.NewsFeed
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface NewsFeedDao {
    @Query("SELECT * FROM newsfeed")
    fun getNewsFeeds(): Flow<List<NewsFeed>>

    @Query("SELECT * FROM newsfeed WHERE id=(:id)")
    suspend fun getNewsFeed(id: UUID): NewsFeed

    @Update
    suspend fun updateNewsFeed(newsFeed: NewsFeed)
}