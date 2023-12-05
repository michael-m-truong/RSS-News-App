package com.bignerdranch.android.newsapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.android.newsapp.databinding.FragmentSavedNewsfeedListBinding
import kotlinx.coroutines.launch

class SavedArticlesListFragment: Fragment() {

    private val savedArticlesListViewModel : SavedArticlesListViewModel by viewModels()
    private lateinit var articleAdapter: SavedArticleListAdapter // Assuming you have an ArticleAdapter
    private lateinit var recyclerView: RecyclerView

    private var _binding: FragmentSavedNewsfeedListBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        articleAdapter = SavedArticleListAdapter()
        _binding = FragmentSavedNewsfeedListBinding.inflate(inflater, container, false)
        binding.savedNewsFeedList.layoutManager = LinearLayoutManager(context)
        binding.savedNewsFeedList.adapter = articleAdapter
        recyclerView = binding.savedNewsFeedList


        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                savedArticlesListViewModel.isListEmpty.collect {isEmpty ->
                    if (isEmpty) {
                        binding.savedNewsFeedList.visibility = View.GONE
                        binding.noSavedArticlesTextView.visibility = View.VISIBLE
                    } else {
                        binding.savedNewsFeedList.visibility = View.VISIBLE
                        binding.noSavedArticlesTextView.visibility = View.GONE
                    }
                }
            }
        }

        fun updateEmptyStateVisibility() {
            if (savedArticlesListViewModel.articles.value.isEmpty()) {
                binding.savedNewsFeedList.visibility = View.GONE
                binding.noSavedArticlesTextView.visibility = View.VISIBLE
            } else {
                binding.savedNewsFeedList.visibility = View.VISIBLE
                binding.noSavedArticlesTextView.visibility = View.GONE
            }
        }

        savedArticlesListViewModel.onDataFiltered.observe(viewLifecycleOwner, Observer {
            updateEmptyStateVisibility()
            recyclerView.post {
                recyclerView.scrollToPosition(0)
            }
        })

        return binding.root

    }

}