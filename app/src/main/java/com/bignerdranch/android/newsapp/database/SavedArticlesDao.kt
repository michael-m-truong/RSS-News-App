package com.bignerdranch.android.newsapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface SavedArticlesDao {

    @Query("Select * FROM SavedArticles ORDER BY dateAdded ASC")
    fun getSavedArticles(): Flow<List<SavedArticles>>

    @Query("SELECT * FROM SavedArticles WHERE id = (:id)")
    suspend fun getSavedArticles(id: UUID): SavedArticles

    @Query("SELECT * FROM SavedArticles WHERE headline LIKE '%' || :headline || '%'")
    suspend fun getArticleByHeadline(headline: String) : SavedArticles

    @Insert
    suspend fun addArticle(savedArticles: SavedArticles)

    @Query("DELETE FROM SavedArticles WHERE id = :id")
    suspend fun deleteArticle(id: UUID): Int
}