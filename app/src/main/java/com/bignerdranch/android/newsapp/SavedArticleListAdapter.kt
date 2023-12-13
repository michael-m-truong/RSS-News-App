package com.bignerdranch.android.newsapp

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.bignerdranch.android.newsapp.database.SavedArticles
import com.bignerdranch.android.newsapp.database.SavedArticlesRepository
import com.bignerdranch.android.newsapp.databinding.ListItemArticleBinding
import com.bignerdranch.android.newsapp.models.Article
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.math.atan2


class SavedArticleListAdapter :
    ListAdapter<SavedArticles, SavedArticleListAdapter.ArticleViewHolder>(SavedArticleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemArticleBinding.inflate(inflater, parent, false)
        return ArticleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val savedArticles = getItem(position)
        holder.bind(savedArticles)
    }

    inner class ArticleViewHolder(private val binding: ListItemArticleBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(article: SavedArticles) {
            binding.root.setOnLongClickListener {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, article.link)
                    type = "text/plain"
                }

                itemView.context.startActivity(Intent.createChooser(shareIntent, null))

                return@setOnLongClickListener true
            }

            binding.textHeadline.text = article.headline
            binding.textDate.text = article.date
            if (article.imgSrc != null) {
                binding.imageView.load(article.imgSrc)
            } else {
                binding.imageView.visibility = View.GONE
            }
            if (article.publisherImgSrc != null) {
                binding.publisherImageView.load(article.publisherImgSrc)
            } else {
                binding.publisherImageView.visibility = View.GONE
            }

            // Make article clickable
            binding.root.setOnClickListener {
                val jsonString = Gson().toJson(article)
                val action = SavedArticlesListFragmentDirections.actionSavedArticlesFragmentToPhotoPageFragment(jsonString)
                it.findNavController().navigate(action)
            }
        }
    }
}

class SavedArticleDiffCallback : DiffUtil.ItemCallback<SavedArticles>() {
    override fun areItemsTheSame(oldItem: SavedArticles, newItem: SavedArticles): Boolean {
        return oldItem.headline == newItem.headline
    }

    override fun areContentsTheSame(oldItem: SavedArticles, newItem: SavedArticles): Boolean {
        return oldItem == newItem
    }
}