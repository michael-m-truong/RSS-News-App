package com.bignerdranch.android.newsapp

import android.net.Uri
import android.text.format.DateUtils
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bignerdranch.android.newsapp.database.SavedArticles
import com.bignerdranch.android.newsapp.database.SavedArticlesRepository
import com.bignerdranch.android.newsapp.models.Article
import com.bignerdranch.android.newsapp.models.DateRelevance
import com.bignerdranch.android.newsapp.models.ReadTimeOption
import com.bignerdranch.android.newsapp.models.ResourceOption
import com.bignerdranch.android.newsapp.models.SortByOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.ocpsoft.prettytime.PrettyTime
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.UUID

class SavedArticlesListViewModel: ViewModel(){

    private val savedArticlesRepository = SavedArticlesRepository.get()


    private val _articles: MutableStateFlow<List<SavedArticles>> = MutableStateFlow(emptyList())
    val articles: StateFlow<List<SavedArticles>>
        get() = _articles.asStateFlow()

    private val _isListEmpty: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isListEmpty: StateFlow<Boolean>
        get() = _isListEmpty.asStateFlow()
    val onDataFiltered: MutableLiveData<Unit> = MutableLiveData()

    private var _resourceOption: MutableSet<ResourceOption> = mutableSetOf()
    val resourceOption: MutableSet<ResourceOption>
        get() = _resourceOption


    private var _publisherOption: MutableSet<String> = mutableSetOf()
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

}