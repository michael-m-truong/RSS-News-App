package com.bignerdranch.android.newsapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bignerdranch.android.newsapp.database.NewsFeedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class NewsFeedDetailViewModel(newsfeedId: UUID) : ViewModel() {
    private val newsFeedRepository = NewsFeedRepository.get()
    private val _newsFeed: MutableStateFlow<NewsFeed?> = MutableStateFlow(null)
    val newsFeed: StateFlow<NewsFeed?> = _newsFeed.asStateFlow()
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

}
class NewsFeedDetailViewModelFactory(
    private val newsfeedId: UUID
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NewsFeedDetailViewModel(newsfeedId) as T
    }
}