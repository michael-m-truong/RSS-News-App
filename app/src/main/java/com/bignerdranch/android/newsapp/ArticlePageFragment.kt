package com.bignerdranch.android.newsapp

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.Date

class ArticlePageFragment : Fragment() {

    private val args: ArticlePageFragmentArgs by navArgs()
    private val savedArticlesListViewModel : SavedArticlesListViewModel by viewModels()
    private var save : Boolean = false
    private var contains: Boolean = false
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

        checkIfContains()
        save = contains
        Log.d("articlePageFragment", "will save $save")
        setHasOptionsMenu(true)

        binding.apply {
            progressBar.max = 100

            webView.apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
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

                webViewClient = object : WebViewClient() {

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return try {
                            val url = request?.url.toString()

                            if (url.contains("reddit.com")) {
                                // Open Reddit app
                                val redditIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                redditIntent.setPackage("com.reddit.frontpage") // Reddit app package name
                                view?.context?.startActivity(redditIntent)
                            } else if (url.contains("twitter.com")) {
                                // Open Twitter app
                                val twitterIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                twitterIntent.setPackage("com.twitter.android") // Twitter app package name
                                view?.context?.startActivity(twitterIntent)
                            } else {
                                // Continue loading the URL in the WebView for other URLs
                                return false
                            }

                            true // Indicate that the URL has been handled
                        } catch (e: ActivityNotFoundException) {
                            Log.e("test", "Could not load URL: ${request?.url}")
                            false // Continue loading the URL in the WebView
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


    private fun checkIfContains() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            contains = savedArticlesListViewModel.contains(article.link)
            Log.d("articlePageFragment", "contains is $contains")
        }
    }

    override fun onPause() {

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
        Log.d("articlePageFrag", "created article")



        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            Log.d("articlePageFragment", "will save $save")
            if (save) {
                if (!contains) {
                    savedArticlesListViewModel.addArticle(savedArticle)
                    Log.d("articlepagefrag", "Added article")
                }
            }else {
                savedArticlesListViewModel.removeArticle(savedArticle.link)
                Log.d("articlepagefrag", "removed Article")
            }
        }
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
