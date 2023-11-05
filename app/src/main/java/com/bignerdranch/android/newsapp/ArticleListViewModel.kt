package com.bignerdranch.android.newsapp

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bignerdranch.android.newsapp.Article
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Date

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
        Log.d("url",url)
        val articles = mutableListOf<Article>()

        try {
            val htmlContent = Jsoup.connect(url).get().html()
            val document = Jsoup.parse(htmlContent)
            val toplevelElements = document.select("div[jslog]")
            var count = 0

            for (element in toplevelElements) {
                val articleElements = element.select("article")
                if (count == 10) {
                    break
                }
                for (articleElement in articleElements) {
                    val headlineText = articleElement.select("h3").text()
                    if (headlineText.isEmpty()) {
                        continue
                    }
                    val headlineLink = "https://news.google.com" + articleElement.select("a").attr("href").substring(1)  //remove the initial "."
                    val headlineDate = articleElement.select("time[datetime]").text()
                    val headlinePublisher = articleElement.select("a[data-n-tid]").text()
                    var imgSrc: String?

                    if (articleElements.size > 1) {
                        imgSrc = articleElement.select("img").attr("src")
                    }
                    else{
                        imgSrc = element.select("figure img").attr("src")
                    }

                    val articleTextDeferred = viewModelScope.async {
                        getArticleText(url)
                    }
                    val articleText = articleTextDeferred.await()
                    //val articleText = getArticleText(headlineLink)
                    Log.d("text",articleText)
                    count +=1
                    if (count == 10) {
                        break
                    }
                    val headlineDateElement = articleElement.select("time")
                    val headlineDateTime = headlineDateElement.attr("datetime")
                    Log.d("datetime",headlineDateTime)
                    val parsedDate = parseDateTime(headlineDateTime)

                    var article = Article(headlineText, headlineLink, headlineDate, parsedDate, headlinePublisher, imgSrc, articleText)
                    articles.add(article)
                }
            }
        } catch (e: Exception) {
            Log.d("bad","bad")
            e.printStackTrace()
        }
        val sortedArticles = articles.sortedByDescending { it.datetime }
        return sortedArticles
    }

    // Define the parseDateTime function
    private fun parseDateTime(dateTimeString: String): Date? {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        try {
            return dateFormat.parse(dateTimeString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // As of now,text will be null in the article object if ran async; but we dont need text rn
    private fun getArticleText(url: String): String {
        try {
            var htmlContent = Jsoup.connect(url).get().html()
            var document = Jsoup.parse(htmlContent)
            val articleLinkElement = document.select("a[rel=nofollow]")
            var finalUrl = url
            if (articleLinkElement.size > 0) {
                finalUrl = articleLinkElement.attr("href")
            }

            htmlContent = Jsoup.connect(finalUrl).get().html()
            val articleDocument = Jsoup.parse(htmlContent)
            Log.d("finalurl", finalUrl)
            val paragraphs = articleDocument.select("p")
            return paragraphs.joinToString(" ") { it.text() }
        } catch (e: Exception) {
            return "Error fetching article text: ${e.message}"
        }
    }


}
