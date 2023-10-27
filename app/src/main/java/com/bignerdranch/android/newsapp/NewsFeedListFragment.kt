package com.bignerdranch.android.newsapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
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

private const val TAG = "NewsFeedListFragment"

class NewsFeedListFragment : Fragment() {

    private val newsFeedListViewModel: NewsFeedListViewModel by viewModels()
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
                        NewsFeedListAdapter(newsfeeds)  {crimeId ->
                            findNavController().navigate(
                                NewsFeedListFragmentDirections.showCrimeDetail(crimeId)
                            )
                        }
                }
            }
        }
    }

}