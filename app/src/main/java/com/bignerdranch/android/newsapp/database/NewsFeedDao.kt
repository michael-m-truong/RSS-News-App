package com.bignerdranch.android.newsapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bignerdranch.android.newsapp.models.ReadTimeOption
import kotlinx.coroutines.flow.Flow
import java.util.*

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

    @Query("UPDATE NewsFeed SET readTimeOption = :newReadTimeOption WHERE id = :newsFeedId")
    suspend fun updateReadTimeOption(newsFeedId: UUID, newReadTimeOption: MutableList<ReadTimeOption>)

    @Query("UPDATE NewsFeed SET dateRelevanceOption = :newDateRelevanceOption WHERE id = :newsFeedId")
    suspend fun updateDateRelevanceOption(newsFeedId: UUID, newDateRelevanceOption: Int)

    @Query("UPDATE NewsFeed SET publisherOption = :newPublisherOption WHERE id = :newsFeedId")
    suspend fun updatePublisherOption(newsFeedId: UUID, newPublisherOption: MutableList<String>)

    @Query("UPDATE NewsFeed SET date = :newDate WHERE id = :newsFeedId")
    suspend fun updateNewsfeedDate(newsFeedId: UUID, newDate: Date)
}