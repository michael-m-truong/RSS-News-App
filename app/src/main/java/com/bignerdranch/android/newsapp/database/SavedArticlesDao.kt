package com.bignerdranch.android.newsapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedArticlesDao {

    @Query("Select * FROM SavedArticles ORDER BY dateAdded ASC")
    fun getSavedArticles(): Flow<List<SavedArticles>>

    @Query("SELECT * FROM SavedArticles WHERE link = (:link)")
    suspend fun getSavedArticles(link: String): SavedArticles?

    @Query("SELECT * FROM SavedArticles WHERE headline LIKE '%' || :headline || '%'")
    suspend fun getArticleByHeadline(headline: String) : SavedArticles

    @Insert
    fun addArticle(savedArticles: SavedArticles)

    @Query("DELETE FROM SavedArticles WHERE link = :link")
    fun deleteArticle(link: String): Int
}