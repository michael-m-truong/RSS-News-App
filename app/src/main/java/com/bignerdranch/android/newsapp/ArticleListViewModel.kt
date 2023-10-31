package com.bignerdranch.android.newsapp

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bignerdranch.android.newsapp.Article
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class ArticleListViewModel : ViewModel() {
    private val _articles: MutableStateFlow<List<Article>> = MutableStateFlow(emptyList())
    val articles: StateFlow<List<Article>> get() = _articles.asStateFlow()

    val onDataFetched: MutableLiveData<Unit> = MutableLiveData()

    companion object {
        var searchQueries: MutableList<String> = mutableListOf()
    }

    init {
        fetchArticles()
    }

    private fun fetchArticles() {
        viewModelScope.launch(Dispatchers.IO) {
            val articles = performWebScraping()
            withContext(Dispatchers.Main) {
                _articles.value = articles
                onDataFetched.postValue(Unit) // Notify the completion of data fetching
            }
        }
    }

    // Function to update search queries based on the selected NewsFeed
    fun setSearchQueriesFromNewsFeed(newsFeed: NewsFeed) {
        // Use the wordBank from the selected NewsFeed as search queries
        searchQueries = newsFeed.wordBank
    }


    private suspend fun performWebScraping(): List<Article> {
        val queryStrings = searchQueries.joinToString("+") { "%22$it%22" }
        val url = "https://news.google.com/search?q=$queryStrings&hl=en-US&gl=US&ceid=US:en"

        val articles = mutableListOf<Article>()

        try {
            val htmlContent = Jsoup.connect(url).get().html()
            val document = Jsoup.parse(htmlContent)
            val toplevelElements = document.select("div[jslog]")
            var count = 0

            for (element in toplevelElements) {
                val attributes = element.attributes()
                if (attributes.size() != 2) {
                    continue
                }
                count +=1
                if (count == 10) {
                    break
                }

                val articleElement = element.select("article")
                val headlineText = articleElement.select("h3").text()
                val headlineLink = "https://news.google.com" + articleElement.select("a").attr("href")
                val headlineDate = articleElement.select("time[datetime]").text()
                val headlinePublisher = articleElement.select("a[data-n-tid]").text()
                val imgSrc = try {
                    element.select("figure img").attr("src")
                } catch (e: Exception) {
                    null
                }

                val articleTextDeferred = viewModelScope.async { getArticleText(headlineLink) }
                val articleText = articleTextDeferred.await()

                val article = Article(headlineText, headlineLink, headlineDate, headlinePublisher, imgSrc, articleText)
                articles.add(article)

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return articles
    }

    private fun getArticleText(url: String): String {
//        return ""
        try {
            val document = Jsoup.connect(url).get()
            val paragraphs = document.select("p")
            return paragraphs.joinToString(" ") { it.text() }
        } catch (e: Exception) {
            return "Error fetching article text: ${e.message}"
        }
    }
}
