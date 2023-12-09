package com.bignerdranch.android.newsapp

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
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
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.bignerdranch.android.newsapp.database.SavedArticles
import com.bignerdranch.android.newsapp.databinding.FragmentArticlePageBinding
import com.bignerdranch.android.newsapp.models.Article
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
                        val actionBar = (activity as? AppCompatActivity)?.supportActionBar
                        if (isAdded && actionBar != null) {
                            actionBar.title = ""
                            actionBar.setDisplayHomeAsUpEnabled(true)
                            val upArrow = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_arrow_forward_24)
                            actionBar.setHomeAsUpIndicator(upArrow)
                        }

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


        contains = checkIfContains()
        save = contains
        Log.d("articlePageFragment", "will save $save")

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.fragment_article_page_add, menu)
                if (save) {
                    menu.getItem(0).setIcon(R.drawable.baseline_bookmark_remove_24)
                } else {
                    menu.getItem(0).setIcon((R.drawable.baseline_bookmark_add_24))
                }
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
                    R.id.bookmarkArticle -> {
                        save = !save
                        val messageId = if (save) R.string.saved_article else R.string.removed_article
                        view?.let { Snackbar.make(it, messageId, Snackbar.LENGTH_SHORT).show() }
                        if (save)
                            menuItem.setIcon(R.drawable.baseline_bookmark_remove_24)
                        else
                            menuItem.setIcon(R.drawable.baseline_bookmark_add_24)
                        true
                    }
                    else -> false
                }

            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        super.onViewCreated(view, savedInstanceState)
    }

    fun init_styles() {
        val actionBar = (activity as? AppCompatActivity?)!!.supportActionBar
        actionBar?.title = ""
        actionBar?.setDisplayHomeAsUpEnabled(true)
        val upArrow = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_arrow_forward_24)
        actionBar?.setHomeAsUpIndicator(upArrow)

        //setHasOptionsMenu(true)

        if (actionBar != null) {
            val text: Spannable = SpannableString(actionBar.title)
            text.setSpan(
                ForegroundColorSpan(Color.BLACK),
                0,
                text.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            actionBar.title = text
        }
    }

    /*override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_article_page_add, menu)

        if (save) {
            menu.getItem(0).setIcon(R.drawable.baseline_bookmark_remove_24)
        } else {
            menu.getItem(0).setIcon((R.drawable.baseline_bookmark_add_24))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.bookmarkArticle -> {
                save = !save
                val messageId = if (save) R.string.saved_article else R.string.removed_article
                view?.let { Snackbar.make(it, messageId, Snackbar.LENGTH_SHORT).show() }
                if (save)
                    item.setIcon(R.drawable.baseline_bookmark_remove_24)
                else
                    item.setIcon(R.drawable.baseline_bookmark_add_24)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    } */


    private fun checkIfContains() : Boolean {

        runBlocking {
            contains = savedArticlesListViewModel.contains(article.link)
            Log.d("articlePageFragment", "contains is $contains")
        }
        return contains
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
