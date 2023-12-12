package com.bignerdranch.android.newsapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bignerdranch.android.newsapp.database.NewsFeed
import com.bignerdranch.android.newsapp.database.NewsFeedRepository
import com.bignerdranch.android.newsapp.models.Filter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*

class NewsFeedDetailViewModel(newsfeedId: UUID) : ViewModel() {
    private val newsFeedRepository = NewsFeedRepository.get()
    private val _newsFeed: MutableStateFlow<NewsFeed?> = MutableStateFlow(null)
    val newsFeed: StateFlow<NewsFeed?> = _newsFeed.asStateFlow()
    var filterState: Filter = Filter.EXACT
    var isOriginalNewsFeedInitialized = false

    init {
        viewModelScope.launch {
            _newsFeed.value = newsFeedRepository.getNewsFeed(newsfeedId)
        }
    }

    fun updateNewsFeed(onUpdate: (NewsFeed) -> NewsFeed) {
        _newsFeed.update { oldNewsFeed ->
            oldNewsFeed?.let { onUpdate(it) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        newsFeed.value?.let { newsFeedRepository.updateNewsFeed(it) }
    }

    suspend fun deleteNewsFeed(id: UUID) {
        newsFeedRepository.deleteNewsFeed(id)
    }

}

class NewsFeedDetailViewModelFactory(
    private val newsfeedId: UUID
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NewsFeedDetailViewModel(newsfeedId) as T
    }
}
