package com.bignerdranch.android.newsapp

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.bignerdranch.android.newsapp.Article
import com.bignerdranch.android.newsapp.databinding.ListItemArticleBinding
import com.bignerdranch.android.newsapp.databinding.ListItemNewsfeedBinding



class ArticleListAdapter(
    private val onItemClicked: (Uri) -> Unit
) :
    ListAdapter<Article, ArticleListAdapter.ArticleViewHolder>(ArticleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemArticleBinding.inflate(inflater, parent, false)
        return ArticleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val article = getItem(position)
        holder.bind(article, onItemClicked)
    }

    inner class ArticleViewHolder(private val binding: ListItemArticleBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(article: Article, onItemClicked: (Uri) -> Unit) {
            binding.textHeadline.text = article.headline
            binding.textDate.text = article.date
            if (article.imgSrc != null) {
                binding.imageView.load(article.imgSrc)
            } else {
                binding.imageView.visibility = View.GONE
            }

            // Make article clickable
            binding.root.setOnClickListener {
                Log.d("click","Clicked on article")
                //onItemClicked("")
            }
        }
    }
}

class ArticleDiffCallback : DiffUtil.ItemCallback<Article>() {
    override fun areItemsTheSame(oldItem: Article, newItem: Article): Boolean {
        return oldItem.headline == newItem.headline
    }

    override fun areContentsTheSame(oldItem: Article, newItem: Article): Boolean {
        return oldItem == newItem
    }
}
