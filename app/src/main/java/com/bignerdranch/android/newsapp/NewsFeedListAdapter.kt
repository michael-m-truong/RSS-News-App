package com.bignerdranch.android.newsapp

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.android.newsapp.databinding.ListItemNewsfeedBinding

class NewsFeedHolder(
    private val binding: ListItemNewsfeedBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(newsFeed: NewsFeed) {
        binding.newsfeedTitle.text = newsFeed.title
        binding.newsfeedDate.text = newsFeed.date.toString()

        binding.root.setOnClickListener {
            Toast.makeText(
                binding.root.context,
                "${newsFeed.title} clicked!",
                Toast.LENGTH_SHORT
            ).show()
        }

    }
}

class NewsFeedListAdapter(
    private val newsFeeds: List<NewsFeed>
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
        holder.bind(newsFeed)
    }
    override fun getItemCount() = newsFeeds.size
}
