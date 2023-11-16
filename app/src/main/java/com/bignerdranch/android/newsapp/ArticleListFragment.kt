package com.bignerdranch.android.newsapp

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bignerdranch.android.newsapp.databinding.FragmentArticleListBinding
import com.bignerdranch.android.newsapp.databinding.SortButtonViewBinding
import com.bignerdranch.android.newsapp.databinding.SortByViewBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

private const val TAG = "ArticleListFragment"

class ArticleListFragment : Fragment() {

    private val articleListViewModel: ArticleListViewModel by viewModels()
    private lateinit var articleAdapter: ArticleListAdapter // Assuming you have an ArticleAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentArticleListBinding.inflate(inflater, container, false)

        articleAdapter = ArticleListAdapter() // Initialize your RecyclerView adapter
        binding.articleRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.articleRecyclerView.adapter = articleAdapter

        // Show loading animation
        binding.loadingProgressBar.visibility = View.VISIBLE

        // Observe the articles from the ViewModel and update the RecyclerView when they change
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                articleListViewModel.articles.collect { articles ->
                    articleAdapter.submitList(articles)
                }
            }
        }

        // Listen for data fetching completion and hide the progress bar
        articleListViewModel.onDataFetched.observe(viewLifecycleOwner, Observer {
            // Hide loading progress bar and show the RecyclerView when data is ready
            binding.loadingProgressBar.visibility = View.INVISIBLE
            binding.articleRecyclerView.visibility = View.VISIBLE

            if (articleListViewModel.isFiltered) {
                val snackbarMessage = "Filtered by reading time"
                Snackbar.make(requireView(), snackbarMessage, Snackbar.LENGTH_SHORT).show()
            }
            articleListViewModel.isFiltered = false
            binding.articleRecyclerView.smoothScrollToPosition(0)
        })


        val showSortButton = binding.filter1Button
        val showReadButton = binding.filter2Button
        val showViewButton = binding.filter3Button


        // Set a click listener for the button to show the popup
        showSortButton.setOnClickListener {
            showInputPopup(R.layout.sort_button_view, R.id.day_cancel_button)
        }

        showReadButton.setOnClickListener {
            showInputPopup(R.layout.read_time_view, R.id.read_cancel_button)
        }

        showViewButton.setOnClickListener {
            showInputPopup(R.layout.sort_by_view, R.id.cancel_button)
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            articleListViewModel.fetchArticles()
            binding.swipeRefreshLayout.isRefreshing = false
        }


        return binding.root
    }

    private fun cancelFilter(dialog: Dialog) {
        // Dismiss the dialog
        dialog.dismiss()
    }

    private fun showInputPopup(view: Int, cancelButtonId: Int) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(view) // Replace with your input group layout
        dialog.setCancelable(true)

        // Make the dialog background white
        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.WHITE))

        // Get the window attributes and set width and height
        val lp = WindowManager.LayoutParams()
        val window = dialog.window
        lp.copyFrom(window?.attributes)
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT

        // Set the gravity to make the dialog appear at the bottom
        lp.gravity = Gravity.BOTTOM

        window?.attributes = lp

        val cancelButton = dialog.findViewById<Button>(cancelButtonId)
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        //val radioButton = dialog.findViewById<RadioButton>(R.id.newest) // Replace with the actual ID
        //radioButton.isChecked = true
        if (view == R.layout.sort_by_view) {
            initalize_sortby(dialog)
        }

        if(view == R.layout.sort_button_view)
        {
            initalize_date(dialog)
        }

        if(view == R.layout.read_time_view)
        {
            initalize_reading(dialog)
        }

        dialog.show()
    }


    private fun initalize_date(dialog: Dialog) {
        val dateOption = articleListViewModel.dateRelevance
        val anytimeButton = dialog.findViewById<RadioButton>(R.id.anytime)
        val todayButton = dialog.findViewById<RadioButton>(R.id.today)
        val lastWeekButton = dialog.findViewById<RadioButton>(R.id.last_week)
        val allButton = dialog.findViewById<RadioButton>(R.id.all)

        val radioGroup = dialog.findViewById<RadioGroup>(R.id.datePicker)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.anytime -> {
                    articleListViewModel.setDateRelevance(DateRelevance.ANYTIME)
                }
                R.id.today -> {
                    articleListViewModel.setDateRelevance(DateRelevance.TODAY)
                }
                R.id.last_week -> {
                    articleListViewModel.setDateRelevance(DateRelevance.LASTWEEK)
                }
                R.id.all -> {
                    articleListViewModel.setDateRelevance(DateRelevance.ALL)
                }
            }
        }

        when (dateOption) {
            DateRelevance.ANYTIME -> {
                anytimeButton.isChecked = true
                todayButton.isChecked = false
                lastWeekButton.isChecked = false
                allButton.isChecked = false
            }
            DateRelevance.TODAY -> {
                anytimeButton.isChecked = false
                todayButton.isChecked = true
                lastWeekButton.isChecked = false
                allButton.isChecked = false
            }
            DateRelevance.LASTWEEK -> {
                anytimeButton.isChecked = false
                todayButton.isChecked = false
                lastWeekButton.isChecked = true
                allButton.isChecked = false
            }
            DateRelevance.ALL -> {
                anytimeButton.isChecked = false
                todayButton.isChecked = false
                lastWeekButton.isChecked = false
                allButton.isChecked = true
            }

        }
    }


    private fun initalize_reading(dialog: Dialog) {
        val readOption = articleListViewModel.readTimeOption
        val oneTothreeButton = dialog.findViewById<MaterialCheckBox>(R.id.read_choice1)
        val fourTosixButton = dialog.findViewById<MaterialCheckBox>(R.id.read_choice2)
        val sixPlusButton = dialog.findViewById<MaterialCheckBox>(R.id.read_choice3)

        val radioGroup = dialog.findViewById<RadioGroup>(R.id.button_group)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.read_choice1 -> {
                    articleListViewModel.setReadTimeOption(ReadTimeOption.oneTOthree)
                }
                R.id.read_choice2 -> {
                    articleListViewModel.setReadTimeOption(ReadTimeOption.fourTOsix)
                }
                R.id.read_choice3 -> {
                    articleListViewModel.setReadTimeOption(ReadTimeOption.sixPlus)
                }

            }
        }

        when (readOption) {
            ReadTimeOption.oneTOthree -> {
                oneTothreeButton.isChecked = true
            }
            ReadTimeOption.fourTOsix -> {
                fourTosixButton.isChecked = true
            }
            ReadTimeOption.sixPlus-> {
                sixPlusButton.isChecked = true
            }
        }
    }


    private fun initalize_sortby(dialog: Dialog) {
        val sortByOption = articleListViewModel.sortByOption
        val newestButton = dialog.findViewById<RadioButton>(R.id.newest) // Replace with the actual ID
        val popularityButton = dialog.findViewById<RadioButton>(R.id.most_popular)
        val radioGroup = dialog.findViewById<RadioGroup>(R.id.button_group)
        val applyButton = dialog.findViewById<MaterialButton>(R.id.apply_button)


        applyButton.setOnClickListener {
            // Save the selected sorting option to the ViewModel here
            // This will only be executed when the apply button is clicked
            val selectedSortByOption = when (radioGroup.checkedRadioButtonId) {
                R.id.newest -> SortByOption.NEWEST
                R.id.most_popular -> SortByOption.MOST_POPULAR
                else -> null // Handle the case when no option is selected
            }

            selectedSortByOption?.let {
                articleListViewModel.setSortByOption(it)
            }

            // Dismiss the dialog or perform other actions if needed
            articleListViewModel.fetchArticles()
            dialog.dismiss()
        }

        when (sortByOption) {
            SortByOption.NEWEST -> {
                newestButton.isChecked = true
                popularityButton.isChecked = false
            }
            SortByOption.MOST_POPULAR -> {
                newestButton.isChecked = false
                popularityButton.isChecked = true
            }
        }
    }


}

