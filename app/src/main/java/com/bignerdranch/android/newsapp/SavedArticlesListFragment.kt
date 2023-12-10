package com.bignerdranch.android.newsapp

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
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

        init_styles()

        articleAdapter = SavedArticleListAdapter()
        _binding = FragmentSavedNewsfeedListBinding.inflate(inflater, container, false)
        binding.savedNewsFeedList.layoutManager = LinearLayoutManager(context)
        binding.savedNewsFeedList.adapter = articleAdapter
        recyclerView = binding.savedNewsFeedList


        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                savedArticlesListViewModel.isListEmpty.collect {isEmpty ->
//                    if (isEmpty) {
//                        binding.savedNewsFeedList.visibility = View.GONE
//                        binding.noSavedArticlesTextView.visibility = View.VISIBLE
//                    } else {
                        binding.savedNewsFeedList.visibility = View.VISIBLE
                        binding.noSavedArticlesTextView.visibility = View.GONE
//                    }
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                savedArticlesListViewModel.articles.collect { articles ->
                    articleAdapter.submitList(articles)
                    recyclerView.post {
                        recyclerView.scrollToPosition(0)
                    }
                }
            }
        }
        
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        /* stupid thing to fix depreciation for sethasoptionsmenu */
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                //menuInflater.inflate(R.menu.fragment_newsfeed_detail, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    android.R.id.home -> {
                        val actionBar = (activity as AppCompatActivity?)!!.supportActionBar
                        actionBar?.setDisplayHomeAsUpEnabled(false)
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                        return true
                    }
                    else -> false
                }

            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        val actionBar = (activity as AppCompatActivity?)!!.supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(false)
        super.onDestroyView()
    }

    fun init_styles() {
        val actionBar = (activity as AppCompatActivity?)!!.supportActionBar
        actionBar?.title = "Saved"
        actionBar?.setDisplayHomeAsUpEnabled(true)
        val upArrow = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_arrow_forward_24)
        actionBar?.setHomeAsUpIndicator(upArrow)

        //setHasOptionsMenu(true)
        // Set ActionBar text color based on night mode
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val textColor = if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            Color.WHITE // Night mode color
        } else {
            Color.BLACK // Day mode color
        }

        if (actionBar != null) {
            val text: Spannable = SpannableString(actionBar.title)
            text.setSpan(
                ForegroundColorSpan(textColor),
                0,
                text.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            actionBar.title = text
        }
    }



}