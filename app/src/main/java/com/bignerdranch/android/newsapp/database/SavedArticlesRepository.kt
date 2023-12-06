package com.bignerdranch.android.newsapp.database

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow


private const val DATABASE_NAME = "savedarticles-database"
class SavedArticlesRepository private constructor(
    context: Context,
    private val coroutineScope: CoroutineScope = GlobalScope
){
    private val database: SavedArticlesDatabase = Room
        .databaseBuilder(
            context.applicationContext,
            SavedArticlesDatabase::class.java,
            DATABASE_NAME
        ).build()

    fun getSavedArticles(): Flow<List<SavedArticles>> = database.savedArticlesDao().getSavedArticles()

    suspend fun getSavedArticles(link: String): SavedArticles? = database.savedArticlesDao().getSavedArticles(link)

    suspend fun addSavedArticles(savedArticles: SavedArticles) {
        database.savedArticlesDao().addArticle(savedArticles)
    }

    suspend fun deleteSavedArticle(link: String) {
        database.savedArticlesDao().deleteArticle(link)
    }

    suspend fun searchByHeadline(headLine: String) {
        database.savedArticlesDao().getArticleByHeadline(headLine)
    }
    companion object {
        private var INSTANCE: SavedArticlesRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = SavedArticlesRepository(context)
            }
        }

        fun get(): SavedArticlesRepository {
            return INSTANCE ?: throw IllegalStateException("SavedArticlesRepository must be initialized")
        }
    }

}