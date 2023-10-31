package com.bignerdranch.android.newsapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bignerdranch.android.newsapp.databinding.FragmentNewsfeedListBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.util.Date
import java.util.UUID

private const val TAG = "NewsFeedListFragment"

class NewsFeedListFragment : Fragment() {

    private val newsFeedListViewModel: NewsFeedListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    private var job: Job? = null
    private var _binding: FragmentNewsfeedListBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNewsfeedListBinding.inflate(inflater, container, false)

        binding.newsfeedRecyclerView.layoutManager = LinearLayoutManager(context)

        swipeToDelete()

        return binding.root
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                newsFeedListViewModel.newsFeeds.collect { newsfeeds ->
                    binding.newsfeedRecyclerView.adapter =
                        NewsFeedListAdapter(newsfeeds)  {newsfeedId ->
                            findNavController().navigate(
                                NewsFeedListFragmentDirections.showCrimeDetail(newsfeedId)
                            )
                        }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_newsfeed_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_newsfeed -> {
                showNewNewsFeed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun showNewNewsFeed() {
        viewLifecycleOwner.lifecycleScope.launch {
            val newNewsFeed = NewsFeed(
                id = UUID.randomUUID(),
                title = "",
                date = Date(),
                wordBank = mutableListOf<String>()
            )
            newsFeedListViewModel.addNewsFeed(newNewsFeed)
            findNavController().navigate(
                NewsFeedListFragmentDirections.showCrimeDetail(newNewsFeed.id)
            )
        }
    }

    private fun swipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                // Not used in this case
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val newsFeed = newsFeedListViewModel.getNewsFeedByPosition(position)
                if (newsFeed != null) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        newsFeedListViewModel.deleteNewsFeed(newsFeed.id)
                    }
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.newsfeedRecyclerView)
    }


}