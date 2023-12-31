package com.bignerdranch.android.newsapp

import android.text.format.DateUtils
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bignerdranch.android.newsapp.database.NewsFeed
import com.bignerdranch.android.newsapp.database.NewsFeedRepository
import com.bignerdranch.android.newsapp.models.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.ocpsoft.prettytime.PrettyTime
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KClass


class ArticleListViewModel : ViewModel() {

    private val newsFeedRepository = NewsFeedRepository.get()

    private val _articles: MutableStateFlow<List<Article>> = MutableStateFlow(emptyList())
    val articles: StateFlow<List<Article>> get() = _articles.asStateFlow()

    private val _originalArticles: MutableStateFlow<List<Article>> = MutableStateFlow(emptyList())
    val originalArticles: StateFlow<List<Article>> get() = _originalArticles.asStateFlow()

    private val _publishers: MutableSet<String> = mutableSetOf()
    val publishers: Set<String> get() = _publishers

    val onDataFetched: MutableLiveData<Unit> = MutableLiveData()
    val onDataFiltered: MutableLiveData<Unit> = MutableLiveData()

    var loadedInitialArticles = false
    var isFiltered = false


    /* Filter variables */

    private var _sortByOption: SortByOption = SortByOption.MOST_POPULAR

    val sortByOption: SortByOption
        get() = _sortByOption

    private var _dateOption: DateRelevance = DateRelevance.ANYTIME
    val dateRelevance: DateRelevance
        get() = _dateOption


    private var _readTimeOption: MutableSet<ReadTimeOption> =
        ReadTimeOption.values().toMutableSet()
    val readTimeOption: MutableSet<ReadTimeOption>
        get() = _readTimeOption

    private var _publisherOption: MutableSet<String> = mutableSetOf()
    val publisherOption: MutableSet<String>
        get() = _publisherOption

    private var _resourceOption: MutableSet<ResourceOption> = mutableSetOf()
    val resourceOption: MutableSet<ResourceOption>
        get() = _resourceOption

    private var _customResourceOption: HashMap<String, Boolean> = HashMap()
    val customResourceOption: HashMap<String, Boolean>
        get() = _customResourceOption

    private var _sourceOption: HashMap<String, Boolean> = HashMap()
    val sourceOption: HashMap<String, Boolean>
        get() = _sourceOption


    fun setSortByOption(sortOption: SortByOption) {
        _sortByOption = sortOption

    }
    suspend fun updateSortByOption() {
        val intValue = sortByOption.ordinal
        newsFeedRepository.updateSortByOption(newsFeedId, intValue)
    }

    fun setDateRelevance(dateOption: DateRelevance) {
        _dateOption = dateOption
    }

    suspend fun updateDateRelevanceOption() {
        val intValue = _dateOption.ordinal
        newsFeedRepository.updateDateRelevanceOption(newsFeedId, intValue)
    }

    fun setReadTimeOption(readTimeOption: MutableSet<ReadTimeOption>) {
        _readTimeOption = readTimeOption
    }

    suspend fun updateReadTimeOption() {
        val readTimeOptionList: MutableList<ReadTimeOption> = readTimeOption.toMutableList()
        newsFeedRepository.updateReadTimeOption(newsFeedId, readTimeOptionList)
    }

    fun setPublisherOption(publisherOption: MutableSet<String>) {
        _publisherOption = publisherOption
    }

    suspend fun updatePublisherOption() {
        if (publisherOption.contains("INIT_NEWSFEED") && publisherOption.size >= 2) {
            publisherOption.remove("INIT_NEWSFEED")
        }
        val publisherOptionList: MutableList<String> = publisherOption.toMutableList()
        newsFeedRepository.updatePublisherOption(newsFeedId, publisherOptionList)
    }

    fun setResourceOption(resourceOption: MutableSet<ResourceOption>) {
        _resourceOption = resourceOption
    }

    suspend fun setCustomResourceOption(k: String, v: Boolean) {
        _customResourceOption[k] = v
        updateSourceOption()
    }

    fun setSourceOption(sourceOption: HashMap<String, Boolean>) {
        _sourceOption = sourceOption

    }

    suspend fun updateSourceOption() {
        try {
            _sourceOption["Google"] = _resourceOption.contains(ResourceOption.Google)
            _sourceOption["Reddit"] = _resourceOption.contains(ResourceOption.Reddit)
            _sourceOption["Twitter"] = _resourceOption.contains(ResourceOption.Twitter)
            _sourceOption.putAll(customResourceOption)
            newsFeedRepository.updateSourceOption(newsFeedId, _sourceOption)
        } catch (e: Exception) {
            // Log the exception using Log.d
        }
    }



    suspend fun updateLastCheckedDate() {
        newsFeedRepository.updateNewsfeedDate(newsFeedId, Date())
    }

    /* Static variables */
    companion object {
        var searchQueries: MutableList<String> = mutableListOf()
        var excludeSearchQueries: MutableList<String> = mutableListOf()
        var sortByOption: Int = 0
        var readTimeOption: MutableList<ReadTimeOption> = mutableListOf()
        var dateRelevanceOption: Int = 0
        var publisherOption: MutableList<String> = mutableListOf()
        var newsFeedId: UUID = UUID.randomUUID()
        var newsfeedTitle: String = ""
        var sourceOption = HashMap<String, Boolean>().apply {
            put("Google", true)
            put("Reddit", false)
            put("Twitter", false)
        }

    }

    init {
        //fetchArticles()
        val sortByOption =
            SortByOption.values().getOrElse(ArticleListViewModel.sortByOption) { SortByOption.MOST_POPULAR }
        setSortByOption(sortByOption)

        val dateRelevanceOption =
            DateRelevance.values().getOrElse(ArticleListViewModel.dateRelevanceOption) { DateRelevance.ANYTIME }
        setDateRelevance(dateRelevanceOption)
        // add get or else
        setReadTimeOption(ArticleListViewModel.readTimeOption.toMutableSet())
        setPublisherOption(ArticleListViewModel.publisherOption.toMutableSet())
        setSourceOption(ArticleListViewModel.sourceOption)
        val sourceOption = ArticleListViewModel.sourceOption
        if ((sourceOption["Google"]) == true) {
            resourceOption.add(ResourceOption.Google)
        }
        if ((sourceOption["Reddit"]) == true) {
            resourceOption.add(ResourceOption.Reddit)
        }
        if ((sourceOption["Twitter"]) == true) {
            resourceOption.add(ResourceOption.Twitter)
        }
        for (source in sourceOption.keys) {
            if (source == "Google" || source == "Reddit" || source == "Twitter") {
                continue
            }
            sourceOption[source]?.let { customResourceOption.put(source, it) }

        }
    }

    fun applyFilters(change: KClass<*>?) {
        viewModelScope.launch(Dispatchers.IO) {
            // Apply your filter criteria
            val filteredArticles = filterArticles(originalArticles.value.toList())
            when (change) {
                SortByOption::class-> {
                    // Code to handle String type
                    updateSortByOption()
                }
                ReadTimeOption::class-> {
                    updateReadTimeOption()
                }
                DateRelevance::class-> {
                    updateDateRelevanceOption()
                }
                String::class-> {
                    updatePublisherOption()
                }
                ResourceOption::class-> {
                    updateSourceOption()
                }
                else -> {
                    // Code to handle other types
                }
            }

            withContext(Dispatchers.Main) {
                _articles.value = filteredArticles
                onDataFiltered.postValue(Unit)
                //onDataFetched.postValue(Unit) // Notify the initial data load
                //loadedInitialArticles = true
            }
        }
    }

    fun clearFilters(view: View? = null) {
        _dateOption = DateRelevance.ANYTIME
        _readTimeOption = ReadTimeOption.values().toMutableSet()
        _sortByOption = SortByOption.MOST_POPULAR
        _publishers.clear()
        _resourceOption = mutableSetOf(ResourceOption.Google)
        //_customResourceOption.clear  // dont erase these on clear filter
        fetchArticles()

        /*if (view != null) {
            val snackbarMessage = "Filtered by reading time"
            Snackbar.make(view, snackbarMessage, Snackbar.LENGTH_SHORT).show()
        }*/
    }

    private fun getArticleReadTime(article: Article): Int {
        val text = article.text
        val words = text.split("\\s+".toRegex())
        val numWords = words.size
        val readTimeMinutes = numWords / 250.0
        return kotlin.math.round(readTimeMinutes).toInt()
    }

    private fun filterArticles(articles: List<Article>): List<Article> {
        // Apply your filter criteria here
        var filteredArticles = articles


        if (_sortByOption == SortByOption.MOST_POPULAR) {
            if (resourceOption.size + customResourceOption.size == 1) {
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
//            for (a in filteredArticles) {
//            }
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
            val tempFilteredArticles = mutableListOf<Article>()

            for (article in filteredArticles) {
                val readTimeMin = getArticleReadTime(article)

                if ((_readTimeOption.contains(ReadTimeOption.oneTOthree) && readTimeMin <= 3) ||
                    (_readTimeOption.contains(ReadTimeOption.fourTOsix) && readTimeMin in 4 .. 6) ||
                    (_readTimeOption.contains(ReadTimeOption.sixPlus) && readTimeMin >= 7)
                ) {
                    tempFilteredArticles.add(article)
                } else if (_readTimeOption.isEmpty()) {
                    // Include articles with no read time information
                    tempFilteredArticles.add(article)
                }
            }

            filteredArticles = tempFilteredArticles
        }



        if (!publisherOption.contains("INIT_NEWSFEED")) {
            filteredArticles = filterByPublisher(filteredArticles)
        }



        // Add more filters as needed


        return filteredArticles
    }

    fun fetchArticles() {
        viewModelScope.launch(Dispatchers.IO) {
            // Load and display the initial articles
            _publishers.clear()
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
            for (source in customResourceOption.keys) {
                if (customResourceOption[source] == false) {
                    continue
                }
                if (source.startsWith("@")) {
                    //performWebScraping_TwitterDirect(source)
                    performWebScraping(ResourceOption.Twitter, source)
                }
                else if (source.startsWith("/r/")) {
                    performWebScraping_Reddit(source)
                }
                else {
                    performWebScraping(ResourceOption.Custom, website = source)
                }
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

    private suspend fun performWebScraping(option: ResourceOption? = null, username: String = "", website: String = ""): List<Article> {
        val exactStrings = searchQueries.joinToString("+") { "%22$it%22" }
        val excludeStrings = excludeSearchQueries.joinToString("+") { "-$it" }
        var queryStrings = exactStrings + "%20" + excludeStrings

        if (option == ResourceOption.Twitter) {
            val username = username.replaceFirst("@", "")
            queryStrings += "%20" + "site:twitter.com/$username"
        }
        else if (option == ResourceOption.Custom) {
            queryStrings += "%20" + "site:$website"
        }

        val url = "https://news.google.com/search?q=$queryStrings&hl=en-US&gl=US&ceid=US:en"
        val articles = mutableListOf<Article>()

        try {
            val htmlContent = Jsoup.connect(url).get().html()
            val document = Jsoup.parse(htmlContent)
            var toplevelElements = document.select("div[jslog]")
            var count = 0
            for (element in toplevelElements) {
                val articleElements = toplevelElements.select("article")
                if (count == 20) {
                    break
                }
                for (articleElement in articleElements) {
                    val headlineText = articleElement.select("a[href]").text()
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
                    val articleText = "fill"
                    count += 1
                    if (count == 20) {
                        break
                    }
                    val headlineDateElement = articleElement.select("time")
                    val headlineDateTime = headlineDateElement.attr("datetime")
                    val parsedDate = parseDateTime(headlineDateTime)

                    val article = Article(
                        headlineText,
                        headlineLink,
                        headlineDate,
                        parsedDate,
                        if (option == ResourceOption.Twitter) extractTwitterPublisher(headlineText) else headlinePublisher,
                        imgSrc,
                        publisherImgSrc,
                        articleText,
                        if (option == ResourceOption.Twitter) option else ResourceOption.Google,
                        dateAdded = Date()
                    )
                    articles.add(article)
                    _publishers.add(article.publisher)
                }
            }
        } catch (e: Exception) {
            //e.printStackTrace()
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

    private suspend fun performWebScraping_Reddit(subreddit: String = ""): List<Article> {
        val exactStrings = searchQueries
            .filter { it.isNotEmpty() } // Filter out empty strings
            .joinToString("+") { "%22$it%22"}
        val url = "https://www.reddit.com$subreddit/search/?q=$exactStrings&sort=hot"
        val articles = mutableListOf<Article>()

        try {
            val htmlContent = Jsoup.connect(url).get().html()
            val document = Jsoup.parse(htmlContent)
            val postElements = document.select("post-consume-tracker")
            val tf = document.select("faceplate-timeago")
            for (element in tf) {
                val attributes = element.attributes()
                for (attribute in attributes) {
                }
            }

            val allElements = document.getAllElements()

            
            var count = 0

            for (postElement in postElements) {
                if (count == 25) {
                    break
                }

                val title = postElement.select("span.invisible").text()
                if (title.isEmpty()) {
                    continue
                }

                val link = "https://www.reddit.com" + postElement.select("a[href^='/r/'][href*='/comments/']").attr("href")

                var datetime = parseDateTime_reddit(postElement.select("faceplate-timeago").attr("ts"))
                var date = formatPrettyTime(datetime)
                var publisher = postElement.select("a[href^='/r/']").attr("href")
                publisher = getSubReddit(publisher)

                date += "  $publisher"

                val imgs = postElement.select("faceplate-img")
                
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

                if (imgSrc == null) {
                    imgSrc = ""
                }
                if (publisherSrc == null) {
                    publisherSrc = ""
                }

                val articleText = "fill" // You may fetch article text if needed

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
            //e.printStackTrace()
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
        articles.filter { article ->
            // List of words to exclude (non-case-sensitive)
            val excludeWords = excludeSearchQueries

            // Splitting the article text by space and checking for exclusion
            val articleWords = article.text.split("\\s+".toRegex())
            val containsExcludedWord = articleWords.any { word ->
                excludeWords.any { excluded -> word.equals(excluded, ignoreCase = true) }
            }

            // Only add the article to the list if it doesn't contain any excluded words
            !containsExcludedWord
        }
        _originalArticles.value += articles.toList()
        return filterArticles(articles)
        //}

    }

    private suspend fun performWebScraping_TwitterDirect(username: String): List<Article> {
        val username = username.replaceFirst("@", "")
        val exactStrings = searchQueries
            .filter { it.isNotEmpty() } // Filter out empty strings
            .joinToString("+") { "%22$it%22"}
        val url = "https://twitter.com/$username"
        val articles = mutableListOf<Article>()

        try {
            val htmlContent = Jsoup.connect(url).get().html()
            val document = Jsoup.parse(htmlContent)
            val timeline = document.select("div.timeline")
            val tweets = timeline.select("div.timeline-item")

            var count = 0

            for (tweet in tweets) {
                if (count == 25) {
                    break
                }
                val tweetContentDiv = document.select("div.tweet-content.media-body").first()

                // Get the text content
                var title = tweetContentDiv?.text()
                if (title == null) {
                    title = ""
                }

                val tweetLink = tweet.select("a.tweet-link")
                // Get the href attribute value if the tweetLink is not null
                var link = tweetLink.attr("href")
                val parts = url.split(link)
                if (parts.size > 1) {
                    val extractedPart = parts[1]
                    link = "https://twiter.com$extractedPart"
                }

                val tweetDateSpan = document.select("span.tweet-date")


                val aElement = tweetDateSpan.select("a").first()

                // Get the title attribute value
                val datetime_string = aElement?.attr("title")

                val datetime = datetime_string?.let { parseDateTime_twitter(it) }

                var date = formatPrettyTime(datetime)

                val usernameLink = document.select("a.username")
                // Get the title attribute value
                val publisher = usernameLink.attr("title")

                date += "  $publisher"


                // Select the <a> element with class "still-image"
                val stillImageLink = document.select("a.still-image").first()

                // Get the href attribute value
                var imgSrc = stillImageLink?.attr("href")
                imgSrc = "https://nitter.net$imgSrc"
                // Select the <img> element with class "avatar round"
                val avatarImage = tweet.select("img.avatar.round")
                // Get the src attribute value
                var publisherSrc = avatarImage?.attr("src")

                if (imgSrc == null) {
                    imgSrc = ""
                }
                if (publisherSrc == null) {
                    publisherSrc = ""
                }

                val articleText = "fill" // You may fetch article text if needed

                val article = Article(
                    title,
                    link,
                    date,
                    datetime,
                    publisher,
                    imgSrc,
                    publisherSrc, // Reddit doesn't have a publisher image in the same way as Google News
                    articleText,
                    ResourceOption.Twitter,
                    Date(),
                )

                articles.add(article)
                count++

                _publishers.add(publisher)
            }

        } catch (e: Exception) {
            //e.printStackTrace()
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
        articles.filter { article ->
            // List of words to exclude (non-case-sensitive)
            val excludeWords = excludeSearchQueries

            // Splitting the article text by space and checking for exclusion
            val articleWords = article.text.split("\\s+".toRegex())
            val containsExcludedWord = articleWords.any { word ->
                excludeWords.any { excluded -> word.equals(excluded, ignoreCase = true) }
            }

            // Only add the article to the list if it doesn't contain any excluded words
            !containsExcludedWord
        }
        _originalArticles.value += articles.toList()
        return filterArticles(articles)
        //}

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

    private fun parseDateTime_reddit(dateTimeString: String): Date? {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXX")
        try {
            return dateFormat.parse(dateTimeString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun parseDateTime_twitter(dateTimeString: String): Date? {
        val dateFormat = SimpleDateFormat("MMM d, yyyy · h:mm a 'UTC'", Locale.US)
        try {
            return dateFormat.parse(dateTimeString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun formatPrettyTime(dateTime: Date?): String {
        val prettyTime = PrettyTime()
        return dateTime?.let { prettyTime.format(it) } ?: ""
    }

    private fun getSubReddit(input: String): String? {
        val regex = """^(.*?/.*?/.*?)/.*$""".toRegex()
        val matchResult = regex.find(input)

        return matchResult?.groups?.get(1)?.value
    }

    private fun extractTwitterPublisher(inputText: String): String {
        // Split the input text at "on X"
        val parts = inputText.split(" on X:")

        // Take the text before "on X" if there are multiple parts
        val result = if (parts.size > 1) {
            parts[0].trim()
        } else {
            // Handle the case where "on X" is not found
            "Name not found"
        }
        return result
    }

    fun getArticleByPosition(index: Int): Article? {
        val articlesList = _articles.value
        if (index >= 0 && index < articlesList.size) {
            return articlesList[index]
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
            val paragraphs = articleDocument.select("p")
            article.text = paragraphs.joinToString(" ") { it.text() }

        } catch (e: Exception) {
        }
    }

}
