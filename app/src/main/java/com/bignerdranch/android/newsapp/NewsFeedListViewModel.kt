package com.bignerdranch.android.newsapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bignerdranch.android.newsapp.database.NewsFeedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "NewsFeedListViewModel"


class NewsFeedListViewModel : ViewModel() {
    private val newsFeedRepository = NewsFeedRepository.get()

    private val _newsFeeds: MutableStateFlow<List<NewsFeed>> = MutableStateFlow(emptyList())
    val newsFeeds: StateFlow<List<NewsFeed>>
        get() = _newsFeeds.asStateFlow()

    init {
        viewModelScope.launch {
            newsFeedRepository.getNewsFeeds().collect {
                _newsFeeds.value = it
            }
        }
    }

    suspend fun addNewsFeed(newsFeed: NewsFeed) {
        newsFeedRepository.addNewsFeed(newsFeed)
    }

    suspend fun deleteNewsFeed(id: UUID) {
        newsFeedRepository.deleteNewsFeed(id)
    }

    fun getNewsFeedByPosition(index: Int): NewsFeed? {
        val newsFeedsList = _newsFeeds.value
        if (index >= 0 && index < newsFeedsList.size) {
            return newsFeedsList[index]
        }
        return null
    }

     fun reorderNewsFeeds(fromPosition: Int, toPosition: Int) {
        val newsFeedsList = _newsFeeds.value.toMutableList()
        if (fromPosition in 0 until newsFeedsList.size && toPosition in 0 until newsFeedsList.size) {
            val newsFeed = newsFeedsList.removeAt(fromPosition)
            newsFeedsList.add(toPosition, newsFeed)
            _newsFeeds.value = newsFeedsList
        }
    }
}