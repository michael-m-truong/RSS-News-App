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


class ArticleListAdapter :
    ListAdapter<Article, ArticleListAdapter.ArticleViewHolder>(ArticleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemArticleBinding.inflate(inflater, parent, false)
        return ArticleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val article = getItem(position)
        holder.bind(article)
    }

    inner class ArticleViewHolder(private val binding: ListItemArticleBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(article: Article) {
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
                Log.d("click", article.link)
                val action = ArticleListFragmentDirections.showArticle(jsonString)
                it.findNavController().navigate(action)
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