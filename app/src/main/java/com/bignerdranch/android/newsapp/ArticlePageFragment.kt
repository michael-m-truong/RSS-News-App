package com.bignerdranch.android.newsapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.bignerdranch.android.newsapp.database.SavedArticles
import com.bignerdranch.android.newsapp.database.SavedArticlesRepository
import com.bignerdranch.android.newsapp.databinding.FragmentArticlePageBinding
import com.bignerdranch.android.newsapp.models.Article
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.Date

class ArticlePageFragment : Fragment() {

    private val args: ArticlePageFragmentArgs by navArgs()
    private val savedArticlesRepository = SavedArticlesRepository.get()
    private var save : Boolean = false
    private lateinit var article : Article

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentArticlePageBinding.inflate(
            inflater,
            container,
            false
        )

        article = Gson().fromJson(args.savedArticleJson, Article::class.java)

        setHasOptionsMenu(true)

        binding.apply {
            progressBar.max = 100

            webView.apply {
                settings.javaScriptEnabled = true
                webViewClient = WebViewClient()
                loadUrl(article.link)

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(
                        webView: WebView,
                        newProgress: Int
                    ) {
                        if (newProgress == 100) {
                            progressBar.visibility = View.GONE
                        } else {
                            progressBar.visibility = View.VISIBLE
                            progressBar.progress = newProgress
                        }
                    }
                }
            }
        }

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_article_page, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.bookmarkArticle -> {
                save = !save
                val messageId = when {
                    save -> R.string.saved_article
                    else -> R.string.removed_article
                }
                view?.let { Snackbar.make(it, messageId, Snackbar.LENGTH_SHORT).show() }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewLifecycleOwner.lifecycleScope.launch {
            val savedArticle = SavedArticles(
                    link = article.link,
                    headline = article.headline,
                    date = article.date,
                    dateAdded = Date(),
                    publisher = article.publisher,
                    imgSrc = article.imgSrc,
                    text = article.text,
                    source = article.source,
                    datetime = article.datetime,
                    publisherImgSrc = article.publisherImgSrc
            )
            if (save) savedArticlesRepository.addSavedArticles(savedArticle)
            else savedArticlesRepository.deleteSavedArticle(savedArticle.link)
        }
    }
}
