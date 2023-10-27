package com.bignerdranch.android.newsapp

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.android.newsapp.databinding.ListItemNewsfeedBinding
import java.util.UUID

class NewsFeedHolder(
    private val binding: ListItemNewsfeedBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(newsFeed: NewsFeed, onNewsFeedClicked: (newsfeedId: UUID) -> Unit) {
        binding.newsfeedTitle.text = newsFeed.title
        binding.newsfeedDate.text = newsFeed.date.toString()

        binding.root.setOnClickListener {
            onNewsFeedClicked(newsFeed.id)
        }

    }
}

class NewsFeedListAdapter(
    private val newsFeeds: List<NewsFeed>,
    private val onNewsFeedClicked: (newsfeedId: UUID) -> Unit
) : RecyclerView.Adapter<NewsFeedHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) : NewsFeedHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemNewsfeedBinding.inflate(inflater, parent, false)
        return NewsFeedHolder(binding)
    }
    override fun onBindViewHolder(holder: NewsFeedHolder, position: Int) {
        val newsFeed = newsFeeds[position]
        holder.bind(newsFeed, onNewsFeedClicked)
    }
    override fun getItemCount() = newsFeeds.size
}
