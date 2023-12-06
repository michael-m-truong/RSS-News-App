package com.bignerdranch.android.newsapp

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bignerdranch.android.newsapp.database.SavedArticles
import com.bignerdranch.android.newsapp.database.SavedArticlesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SavedArticlesListViewModel: ViewModel(){

    private val savedArticlesRepository = SavedArticlesRepository.get()


    private val _articles: MutableStateFlow<List<SavedArticles>> = MutableStateFlow(emptyList())
    val articles: StateFlow<List<SavedArticles>>
        get() = _articles.asStateFlow()

    private val _isListEmpty: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isListEmpty: StateFlow<Boolean>
        get() = _isListEmpty.asStateFlow()
    val onDataFiltered: MutableLiveData<Unit> = MutableLiveData()

    init {
        viewModelScope.launch {
            savedArticlesRepository.getSavedArticles().collect() {
                Log.d("savedArticlesListVIewModel", it.isEmpty().toString())
                _articles.value = it
                _isListEmpty.value = it.isEmpty()
            }
        }
    }

    fun getArticleByPosition(index: Int): SavedArticles? {
        val savedArticlesList = _articles.value
        if (index >= 0 && index < savedArticlesList.size) {
            return savedArticlesList[index]
        }
        return null
    }

    suspend fun addArticle(article: SavedArticles) {
        savedArticlesRepository.addSavedArticles(article)
    }

    suspend fun removeArticle(link: String) {
        savedArticlesRepository.deleteSavedArticle(link)
    }

    suspend fun contains(link: String) : Boolean{
        return savedArticlesRepository.getSavedArticles(link) != null
    }

}