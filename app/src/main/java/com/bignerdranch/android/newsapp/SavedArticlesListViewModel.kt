package com.bignerdranch.android.newsapp

import android.text.format.DateUtils
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private val _articles: MutableStateFlow<List<Article>> = MutableStateFlow(emptyList())

    val articles: StateFlow<List<Article>> get() = _articles.asStateFlow()

    private val _originalArticles: MutableStateFlow<List<Article>> = MutableStateFlow(emptyList())

    val originalArticles: StateFlow<List<Article>> get() = _originalArticles.asStateFlow()

    private val _publishers: MutableSet<String> = mutableSetOf()
    val publishers: Set<String> get() = _publishers

    val onDataFetched: MutableLiveData<Unit> = MutableLiveData()
    val onDataFiltered: MutableLiveData<Unit> = MutableLiveData()

    var loadedInitialArticles = false

    private var _resourceOption: MutableSet<ResourceOption> = mutableSetOf()
    val resourceOption: MutableSet<ResourceOption>
        get() = _resourceOption


    private var _publisherOption: MutableSet<String> = mutableSetOf()
    val publisherOption: MutableSet<String>
        get() = _publisherOption

    private var _readTimeOption: MutableSet<ReadTimeOption> =
        ReadTimeOption.values().toMutableSet()
    val readTimeOption: MutableSet<ReadTimeOption>
        get() = _readTimeOption

    private var _dateOption: DateRelevance = DateRelevance.ANYTIME
    val dateRelevance: DateRelevance
        get() = _dateOption


    private var _sortByOption: SortByOption = SortByOption.MOST_POPULAR

    val sortByOption: SortByOption
        get() = _sortByOption


    private fun formatPrettyTime(dateTime: Date?): String {
        val prettyTime = PrettyTime()
        return dateTime?.let { prettyTime.format(it) } ?: ""
    }
    private fun getSubReddit(input: String): String? {
        val regex = """^(.*?/.*?/.*?)/.*$""".toRegex()
        val matchResult = regex.find(input)

        return matchResult?.groups?.get(1)?.value
    }
    private suspend fun performWebScraping_Reddit(): List<Article> {
        val query = "nba"
        val url = "https://www.reddit.com/search/?q=%22$query%22&sort=hot"
        Log.d("url", url)
        val articles = mutableListOf<Article>()

        try {
            val htmlContent = Jsoup.connect(url).get().html()
            val document = Jsoup.parse(htmlContent)
            Log.d("not empty", "test")
            val postElements = document.select("post-consume-tracker")
            val tf = document.select("faceplate-timeago")
            Log.d("ongg", tf.size.toString())
            Log.d("ongg", tf.outerHtml())
            for (element in tf) {
                val attributes = element.attributes()
                for (attribute in attributes) {
                    Log.d("Attribute", "${attribute.key} = ${attribute.value}")
                }
            }

            val allElements = document.getAllElements()

            for (element in allElements) {
                if (element.text().contains("hours ago")) {
                    // Log or process the element as needed
                    Log.d("FoundElement", "Element with 'hours ago': ${element.outerHtml()}")
                }
            }
            var count = 0

            Log.d("totalamt", postElements.size.toString())
            for (postElement in postElements) {
                if (count == 25) {
                    break
                }

                val title = postElement.select("span.invisible").text()
                Log.d("DISDATITLE",title)
                if (title.isEmpty()) {
                    continue
                }

                val link = "https://www.reddit.com" + postElement.select("a[href^='/r/'][href*='/comments/']").attr("href")

                var datetime = parseDateTime_reddit(postElement.select("faceplate-timeago").attr("ts"))
                var date = formatPrettyTime(datetime)
                Log.d("plss",postElement.select("faceplate-timeago").attr("ts"))
                var publisher = postElement.select("a[href^='/r/']").attr("href")
                publisher = getSubReddit(publisher)

                date += "  $publisher"

                val imgs = postElement.select("faceplate-img")
                for (img in imgs) {
                    Log.d("imgz", img.attr("src"))
                }
                var imgSrc = imgs[1].attr("src")
                var publisherSrc = imgs[0].attr("src")
                if (imgs.size == 3) {
                    imgSrc = imgs[2].attr("src")
                    publisherSrc = postElement.select("faceplate-img")[0].attr("src")
                }
                if (imgs.size == 4) {
                    imgSrc = imgs[3].attr("src")
                    publisherSrc = imgs[2].attr("src")
                }

                Log.d("testt", postElement.select("faceplate-img").size.toString())
                if (imgSrc == null) {
                    imgSrc = ""
                }
                if (publisherSrc == null) {
                    publisherSrc = ""
                }

                val articleText = "" // You may fetch article text if needed

                val article = Article(
                    title,
                    link,
                    date,
                    datetime,
                    publisher,
                    imgSrc,
                    publisherSrc, // Reddit doesn't have a publisher image in the same way as Google News
                    articleText,
                    ResourceOption.Reddit,
                    Date(),
                )

                articles.add(article)
                count++

                _publishers.add(publisher)
            }

        } catch (e: Exception) {
            Log.d("bad", e.message.toString())
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

        //if (sortByOption == SortByOption.NEWEST) {
        //    val sortedArticles = articles.sortedByDescending { it.datetime }
        //    return sortedArticles
        //} else {
        _originalArticles.value += articles.toList()
        return filterArticles(articles)
        //}

    }
    fun fetchArticles() {
        _publishers.clear()
        viewModelScope.launch(Dispatchers.IO) {
            // Load and display the initial articles
            _originalArticles.value = mutableListOf<Article>()
            if (resourceOption.contains(ResourceOption.Google)) {
                performWebScraping()
            }
            if (resourceOption.contains(ResourceOption.Reddit)) {
                performWebScraping_Reddit()
            }
            if (resourceOption.contains(ResourceOption.Twitter)) {
                performWebScraping(ResourceOption.Twitter)
            }
            val initialArticles = filterArticles(_originalArticles.value.toList())

            withContext(Dispatchers.Main) {
                _articles.value = initialArticles
                onDataFetched.postValue(Unit) // Notify the initial data load
                loadedInitialArticles = true
            }

            // Asynchronously update the articles in the background

            if (initialArticles.isNotEmpty() && resourceOption.size == 1 && resourceOption.contains(ResourceOption.Google)) {
                val deferredUpdates = initialArticles.map { article ->
                    async {
                        updateArticleUrlAndText(article.link, article)
                    }
                }

                // Wait for all updates to complete
                deferredUpdates.awaitAll()
                //var filteredArticles = emptyList<Article>()
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

                /*if (_publishers.isNotEmpty()) {
                    filteredArticles = filterByPublisher(initialArticles)
                }

                // Update the articles with the filtered data
                if (filteredArticles.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        _articles.value = filteredArticles
                        onDataFetched.postValue(Unit) // Notify the completion of data fetching
                        isFiltered = true
                    }
                }*/
            }
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
            Log.d("huh", article.toString())

        } catch (e: Exception) {
            Log.d("exception", e.message.toString())
        }
    }

    private fun parseDateTime(dateTimeString: String): Date? {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        try {
            return dateFormat.parse(dateTimeString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun parseDateTime_reddit(dateTimeString: String): Date? {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXX")
        try {
            return dateFormat.parse(dateTimeString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private suspend fun performWebScraping(option: ResourceOption? = null): List<Article> {
        val exactStrings = ArticleListViewModel.searchQueries.joinToString("+") { "%22$it%22" }
        val excludeStrings = ArticleListViewModel.excludeSearchQueries.joinToString("+") { "-$it" }
        var queryStrings = exactStrings + "%20" + excludeStrings

        if (option == ResourceOption.Twitter) {
            queryStrings += "%20" + "site:twitter.com"
        }

        val url = "https://news.google.com/search?q=$queryStrings&hl=en-US&gl=US&ceid=US:en"
        Log.d("url", url)
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
                    val headlineText = articleElement.select("h4").text()
                    if (headlineText.isEmpty()) {
                        continue
                    }
                    val headlineLink = "https://news.google.com" + articleElement.select("a").attr("href")
                        .substring(1)  //remove the initial "."
                    val headlineDate = articleElement.select("time[datetime]").text()
                    val headlinePublisher = articleElement.select("div[data-n-tid]").text()
                    var imgSrc: String?

                    //if (articleElements.size <= 1) {
                    //    imgSrc = articleElement.select("img").attr("src")
                    //} else {
                    imgSrc = articleElement.select("figure img").attr("src")
                    Log.d("imgsrc", imgSrc)
                    //}
                    val publisherImgSrc: String?
                    if (articleElements.size <= 1) {
                        publisherImgSrc = articleElement.select("div").select("img").attr("src")
                    } else {
                        publisherImgSrc = articleElement.select("img").attr("src")
                    }
//                    val articleTextDeferred = viewModelScope.async {
//                        getArticleText(url)
//                    }
//                    val articleText = articleTextDeferred.await()
                    //val articleText = getArticleText(headlineLink)
                    val articleText = ""
                    count += 1
                    if (count == 10) {
                        break
                    }
                    val headlineDateElement = articleElement.select("time")
                    val headlineDateTime = headlineDateElement.attr("datetime")
                    Log.d("datetime", headlineDateTime)
                    val parsedDate = parseDateTime(headlineDateTime)

                    val article = Article(
                        headlineText,
                        headlineLink,
                        headlineDate,
                        parsedDate,
                        headlinePublisher,
                        imgSrc,
                        publisherImgSrc,
                        articleText,
                        if (option == ResourceOption.Twitter) option else ResourceOption.Google,
                        dateAdded = Date()
                    )
                    articles.add(article)
                    _publishers.add(headlinePublisher)
                }
            }
        } catch (e: Exception) {
            Log.d("bad", "bad")
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

        //if (sortByOption == SortByOption.NEWEST) {
        //    val sortedArticles = articles.sortedByDescending { it.datetime }
        //    return sortedArticles
        //} else {
        _originalArticles.value += articles.toList()
        return filterArticles(articles)
        //}

    }

    private fun getArticleReadTime(article: Article): Int {
        val text = article.text
        val words = text.split("\\s+".toRegex())
        val numWords = words.size
        val readTimeMinutes = numWords / 250.0
        return kotlin.math.round(readTimeMinutes).toInt()
    }

    private fun filterByPublisher(initialArticles: List<Article>): List<Article> {
        // Filter articles based on the selected publishers
        if (publisherOption.contains("ALL_PUBLISHERS"))  {
            return initialArticles
        }
        return initialArticles.filter { article ->
            // Check if the article's publisher is in the selected publishers set
            val isPublisherSelected = article.publisher in _publisherOption
            isPublisherSelected
        }
    }
    private fun filterArticles(articles: List<Article>, multiSourceArticles: List<List<Article>>? = null): List<Article> {
        // Apply your filter criteria here
        var filteredArticles = articles

        print(filteredArticles.size)

        if (_sortByOption == SortByOption.MOST_POPULAR) {
            if (resourceOption.size == 1) {
                filteredArticles = _originalArticles.value
            }
            else {
                // Separate articles by source
                val googleArticles = filteredArticles.filter { it.source == ResourceOption.Google }
                val redditArticles = filteredArticles.filter { it.source == ResourceOption.Reddit }
                val twitterArticles = filteredArticles.filter { it.source == ResourceOption.Twitter }

                // Interleave articles in the desired pattern
                val interleavedArticles = mutableListOf<Article>()
                val maxCount = maxOf(googleArticles.size, redditArticles.size, twitterArticles.size)
                for (i in 0 until maxCount) {
                    if (i < googleArticles.size) interleavedArticles.add(googleArticles[i])
                    if (i < redditArticles.size) interleavedArticles.add(redditArticles[i])
                    if (i < twitterArticles.size) interleavedArticles.add(twitterArticles[i])
                }

                filteredArticles = interleavedArticles
            }
        } else if (_sortByOption == SortByOption.NEWEST) {
            filteredArticles = filteredArticles.sortedByDescending { it.datetime }
        }


//        if (_dateOption == DateRelevance.ANYTIME) {
//
//        } else
        when (_dateOption) {
            DateRelevance.PASTHOUR -> {
                val currentDateTime = Date()
                filteredArticles = filteredArticles
                    .filter { it.datetime?.let { datetime ->
                        val oneHourAgo = currentDateTime.time - DateUtils.HOUR_IN_MILLIS
                        datetime.time in oneHourAgo until currentDateTime.time
                    } ?: false }
            }
            DateRelevance.TODAY -> {
                filteredArticles = filteredArticles.filter {
                    DateUtils.isToday(it.datetime?.time ?: 0)
                }
            }
            DateRelevance.LASTWEEK -> {
                val oneWeekAgo = Calendar.getInstance()
                oneWeekAgo.add(Calendar.WEEK_OF_YEAR, -1)
                filteredArticles = filteredArticles.filter {
                    it.datetime?.after(oneWeekAgo.time) ?: false
                }
            }
            else -> {}
        }

        if (_readTimeOption.isNotEmpty()) {
            val readTimeArticles = mutableListOf<Article>()

            if (_readTimeOption.contains(ReadTimeOption.oneTOthree)) {
                readTimeArticles += filteredArticles.filter {
                    val readTimeMin = getArticleReadTime(it)
                    readTimeMin <= 3
                }
            }

            if (_readTimeOption.contains(ReadTimeOption.fourTOsix)) {
                readTimeArticles += filteredArticles.filter {
                    val readTimeMin = getArticleReadTime(it)
                    readTimeMin in 4..6
                }
            }

            if (_readTimeOption.contains(ReadTimeOption.oneTOthree)) {
                readTimeArticles += filteredArticles.filter {
                    val readTimeMin = getArticleReadTime(it)
                    readTimeMin >= 7
                }
            }

            filteredArticles = readTimeArticles
        }


        if (!publisherOption.contains("INIT_NEWSFEED")) {
            filteredArticles = filterByPublisher(filteredArticles)
        }

        Log.d("filtered", filteredArticles.toString())


        // Add more filters as needed

        print(filteredArticles.size)

        return filteredArticles
    }

    companion object {
        var searchQueries: MutableList<String> = mutableListOf()
        var excludeSearchQueries: MutableList<String> = mutableListOf()
        var savedArticleId: UUID = UUID.randomUUID()
    }

    init {
        fetchArticles()
    }
}