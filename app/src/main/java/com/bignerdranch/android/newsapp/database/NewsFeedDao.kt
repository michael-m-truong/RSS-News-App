package com.bignerdranch.android.newsapp.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.bignerdranch.android.newsapp.NewsFeed
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface NewsFeedDao {
    @Query("SELECT * FROM NewsFeed ORDER BY orderNumber ASC")
    fun getNewsFeeds(): Flow<List<NewsFeed>>

    @Query("SELECT * FROM newsfeed WHERE id=(:id)")
    suspend fun getNewsFeed(id: UUID): NewsFeed

    @Update
    suspend fun updateNewsFeed(newsFeed: NewsFeed)

    @Insert
    suspend fun addCrime(newsFeed: NewsFeed)

    @Query("DELETE FROM newsfeed WHERE id =(:id)")
    suspend fun deleteNewsFeed(id: UUID): Int

    @Query("UPDATE NewsFeed SET orderNumber = :newOrderNumber WHERE id = :newsFeedId")
    fun updateOrderNumber(newsFeedId: UUID, newOrderNumber: Int)

    @Query("UPDATE NewsFeed SET sortByOption = :defaultSortOption WHERE id = :newsFeedId")
    suspend fun initializeSortByOption(newsFeedId: UUID, defaultSortOption: Int)

    @Query("UPDATE NewsFeed SET sortByOption = :newSortOption WHERE id = :newsFeedId")
    suspend fun updateSortByOption(newsFeedId: UUID, newSortOption: Int)


}