package com.bignerdranch.android.newsapp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bignerdranch.android.newsapp.database.NewsFeed
import com.bignerdranch.android.newsapp.database.NewsFeedRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*


private const val TAG = "NewsFeedListViewModel"


class NewsFeedListViewModel : ViewModel() {
    private val newsFeedRepository = NewsFeedRepository.get()

    private val _newsFeeds: MutableStateFlow<List<NewsFeed>> = MutableStateFlow(emptyList())
    val newsFeeds: StateFlow<List<NewsFeed>>
        get() = _newsFeeds.asStateFlow()

    // New StateFlow to represent whether the list is empty or not
    private val _isListEmpty: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isListEmpty: StateFlow<Boolean>
        get() = _isListEmpty.asStateFlow()

    init {
        viewModelScope.launch {
            newsFeedRepository.getNewsFeeds().collect {
                _newsFeeds.value = it
                // Update the isListEmpty StateFlow based on the new list of crimes
                _isListEmpty.value = it.isEmpty()
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

    suspend fun updateNewsFeedOrder(fromPosition: Int, toPosition: Int) {
        val newsFeedsList = _newsFeeds.value.toMutableList()
        val lowerPosition = minOf(fromPosition, toPosition)
        val higherPosition = maxOf(fromPosition, toPosition)

        val deferredUpdates = mutableListOf<Deferred<Unit>>()

        withContext(Dispatchers.IO) {
            for (i in lowerPosition..higherPosition) {
                val newsFeed = newsFeedsList[i]
                val deferred = async {
                    newsFeedRepository.updateOrderNumber(newsFeed.id, i)
                }
                deferredUpdates.add(deferred)
            }
        }

        // Wait for all the updates to complete
        deferredUpdates.awaitAll()
    }


    fun reorderNewsFeeds(fromPosition: Int, toPosition: Int) {
        val newsFeedsList = _newsFeeds.value.toMutableList()
        Log.d("idx", "test")
        if (fromPosition in 0 until newsFeedsList.size && toPosition in 0 until newsFeedsList.size) {
            val itemToMove = newsFeedsList.removeAt(fromPosition)
            newsFeedsList.add(toPosition, itemToMove)
            val lowerPosition = minOf(fromPosition, toPosition)
            val higherPosition = maxOf(fromPosition, toPosition)

            _newsFeeds.value = newsFeedsList

            // Run the for loop in a background thread using a coroutine
//            withContext(Dispatchers.IO) {
//                for (i in lowerPosition..higherPosition) {
//                    val newsFeed = newsFeedsList[i]
//                    newsFeedRepository.updateOrderNumber(newsFeed.id, i)
//                }
//            }
        }
    }


    fun getCount(): Int {
        return _newsFeeds.value.size
    }

}