package com.bignerdranch.android.newsapp

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bignerdranch.android.newsapp.database.NewsFeedRepository
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
import java.lang.Math.ceil
import java.text.SimpleDateFormat
import java.util.Date

class ArticleListViewModel : ViewModel() {

    private val _articles: MutableStateFlow<List<Article>> = MutableStateFlow(emptyList())
    val articles: StateFlow<List<Article>> get() = _articles.asStateFlow()

    private val _originalArticles: MutableStateFlow<List<Article>> = MutableStateFlow(emptyList())
    val originalArticles: StateFlow<List<Article>> get() = _originalArticles.asStateFlow()

    private val _publishers: MutableSet<String> = mutableSetOf()
    val publishers: Set<String> get() = _publishers

    val onDataFetched: MutableLiveData<Unit> = MutableLiveData()

    var loadedInitialArticles = false
    var isFiltered = false


    /* Filter variables */

    private var _sortByOption: SortByOption = SortByOption.MOST_POPULAR

    val sortByOption: SortByOption
        get() = _sortByOption

    private var _dateOption: DateRelevance = DateRelevance.ANYTIME
    val dateRelevance: DateRelevance
        get() = _dateOption


    private var _readTimeOption: ReadTimeOption = ReadTimeOption.oneTOthree
    val readTimeOption: ReadTimeOption
        get() = _readTimeOption

    private var _publisherOption: MutableSet<String> = mutableSetOf()
    val publisherOption: MutableSet<String>
        get() = _publisherOption

    private var _resourceOption: ResourceOption = ResourceOption.Google
    val resourceOption: ResourceOption
        get() = _resourceOption

    fun setSortByOption(sortOption: SortByOption) {
        _sortByOption = sortOption
    }

    fun setDateRelevance(dateOption: DateRelevance) {
        _dateOption = dateOption
    }

    fun setReadTimeOption(readTimeOption: ReadTimeOption){
        _readTimeOption = readTimeOption
    }

    fun addPublisherOption(publisher: String){
        _publisherOption.add(publisher)
    }

    fun removePublisherOption(publisher: String){
        _publisherOption.remove(publisher)
    }

    fun setPublisherOption(publisherOption: MutableSet<String>) {
        _publisherOption = publisherOption
    }

    fun setResourceOption(resourceOption: ResourceOption){
        _resourceOption = resourceOption
    }


    /* Static variables */
    companion object {
        var searchQueries: MutableList<String> = mutableListOf()
        var excludeSearchQueries: MutableList<String> = mutableListOf()
        var sortByOption: Int = 0
    }

    init {
        fetchArticles()
        val sortByOption = SortByOption.values().getOrElse(ArticleListViewModel.sortByOption) { SortByOption.NEWEST }
        setSortByOption(sortByOption)
        setDateRelevance(DateRelevance.ANYTIME);
        setReadTimeOption(ReadTimeOption.oneTOthree);
        setResourceOption(ResourceOption.Google);

    }

    fun applyFilters() {
        viewModelScope.launch(Dispatchers.IO) {
            // Apply your filter criteria
            val filteredArticles = filterArticles(originalArticles.value)

            withContext(Dispatchers.Main) {
                _articles.value = filteredArticles
                //onDataFetched.postValue(Unit) // Notify the initial data load
                //loadedInitialArticles = true
            }
        }
    }

    private fun filterArticles(articles: List<Article>): List<Article> {
        // Apply your filter criteria here
        var filteredArticles = articles

        if (_publishers.isNotEmpty()) {
            filteredArticles = filterByPublisher(filteredArticles)
        }

        // Add more filters as needed

        return filteredArticles
    }

    fun fetchArticles() {
        _publishers.clear()
        viewModelScope.launch(Dispatchers.IO) {
            // Load and display the initial articles
            val initialArticles = performWebScraping()

            withContext(Dispatchers.Main) {
                _articles.value = initialArticles
                _originalArticles.value = initialArticles.toList()
                onDataFetched.postValue(Unit) // Notify the initial data load
                loadedInitialArticles = true
            }

            // Asynchronously update the articles in the background

            if (initialArticles.isNotEmpty()) {
                val deferredUpdates = initialArticles.map { article ->
                    async {
                        updateArticleUrlAndText(article.link, article)
                    }
                }

                // Wait for all updates to complete
                deferredUpdates.awaitAll()
                var filteredArticles = emptyList<Article>()
                // Filter articles based on word count and reading time
                /*val filteredArticles = articlesToUpdate.filter { article ->
                    val text = article.text
                    val words = text.split(Regex("\\s+"))
                    val wordCount = words.size

                    // Calculate minutes to read based on word count
                    val wordsPerMinute = 238 // Adjust this value based on your assumptions
                    val minutesToRead = ceil(wordCount.toDouble() / wordsPerMinute).toInt().coerceAtLeast(1)

                    // Adjust the conditions as needed
                    (text.isEmpty() || minutesToRead in 4..6)
                } */

                if (_publishers.isNotEmpty()) {
                    filteredArticles= filterByPublisher(initialArticles)
                }

                // Update the articles with the filtered data
                if (filteredArticles.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        _articles.value = filteredArticles
                        onDataFetched.postValue(Unit) // Notify the completion of data fetching
                        isFiltered = true
                    }
                }
            }
        }

    }

    private fun filterByPublisher(initialArticles: List<Article>): List<Article> {
        // Filter articles based on the selected publishers
        return initialArticles.filter { article ->
            // Check if the article's publisher is in the selected publishers set
            val isPublisherSelected = _publisherOption.isEmpty() || article.publisher in _publisherOption
            isPublisherSelected
        }
    }


    private suspend fun performWebScraping(): List<Article> {
        val exactStrings = searchQueries.joinToString("+") { "%22$it%22" }
        val excludeStrings = excludeSearchQueries.joinToString("+") { "-$it"}
        val queryStrings = exactStrings + excludeStrings
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
                    val publisherImgSrc: String?
                    if (articleElements.size > 1) {
                        publisherImgSrc = articleElement.select("div").select("img").attr("src")
                    }
                    else{
                        publisherImgSrc = articleElement.select("img").attr("src")
                    }
//                    val articleTextDeferred = viewModelScope.async {
//                        getArticleText(url)
//                    }
//                    val articleText = articleTextDeferred.await()
                    //val articleText = getArticleText(headlineLink)
                    val articleText = ""
                    count +=1
                    if (count == 10) {
                        break
                    }
                    val headlineDateElement = articleElement.select("time")
                    val headlineDateTime = headlineDateElement.attr("datetime")
                    Log.d("datetime",headlineDateTime)
                    val parsedDate = parseDateTime(headlineDateTime)

                    var article = Article(headlineText, headlineLink, headlineDate, parsedDate, headlinePublisher, imgSrc, publisherImgSrc, articleText)
                    articles.add(article)
                    _publishers.add(headlinePublisher)
                }
            }
        } catch (e: Exception) {
            Log.d("bad","bad")
            e.printStackTrace()
        }

        /*viewModelScope.launch(Dispatchers.IO) {
            val deferredUpdates = articles.map { article ->
                async {
                    updateArticleUrlAndText(article.link, article)
                }
            }

            deferredUpdates.awaitAll()
        }*/

        if (sortByOption == SortByOption.NEWEST) {
            val sortedArticles = articles.sortedByDescending { it.datetime }
            return sortedArticles
        }
        else {
            return articles
        }

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

    private fun updateArticleUrlAndText(url: String, article: Article) {
        try {
            var htmlContent = Jsoup.connect(url).get().html()
            var document = Jsoup.parse(htmlContent)
            val articleLinkElement = document.select("a[rel=nofollow]")
            var finalUrl = url
            if (articleLinkElement.size > 0) {
                finalUrl = articleLinkElement.attr("href")
            }
            article.link = finalUrl
            htmlContent = Jsoup.connect(finalUrl)
                .method(Connection.Method.GET)
                .userAgent("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.5414.117 Mobile Safari/537.36")
                .referrer("https://www.google.com")
                .header("accept", "*/*")
                .timeout(3000)
                .header("content-type", "text/plain;charset=UTF-8")
                .get()
                .html()
            val articleDocument = Jsoup.parse(htmlContent)
            Log.d("finalurl", finalUrl)
            val paragraphs = articleDocument.select("p")
            article.text = paragraphs.joinToString(" ") { it.text() }
            Log.d("huh",article.toString())

        } catch (e: Exception) {
            Log.d("exception", e.message.toString())
        }
    }

}
