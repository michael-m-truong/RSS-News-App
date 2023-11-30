package com.bignerdranch.android.newsapp

import android.app.AlertDialog
import android.app.Dialog
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.CheckBox
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
import com.bignerdranch.android.newsapp.models.DateRelevance
import com.bignerdranch.android.newsapp.models.ReadTimeOption
import com.bignerdranch.android.newsapp.models.ResourceOption
import com.bignerdranch.android.newsapp.models.SortByOption
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
    ): View {
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

            if (articleListViewModel.articles.value.isEmpty()) {
                binding.articleRecyclerView.visibility = View.GONE
                binding.emptyTextView.visibility = View.VISIBLE
            } else {
                binding.articleRecyclerView.visibility = View.VISIBLE
                binding.emptyTextView.visibility = View.GONE
            }

            if (articleListViewModel.isFiltered) {
                val snackbarMessage = "Filtered by reading time"
                Snackbar.make(requireView(), snackbarMessage, Snackbar.LENGTH_SHORT).show()
            }
            articleListViewModel.isFiltered = false
            binding.articleRecyclerView.smoothScrollToPosition(0)
        })


        //val nightMode = AppCompatDelegate.getDefaultNightMode()
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        val filterButtonBackgroundColor = if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            Color.parseColor("#141414")  // night mode color
        } else {
            Color.WHITE
        }

        val filterButtonTextColor = if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            Color.WHITE
        } else {
            Color.parseColor("#4C4C4C")  // Use the color value for light mode
        }

        binding.filter1Button.setBackgroundColor(filterButtonBackgroundColor)
        binding.filter1Button.setTextColor(filterButtonTextColor)

        binding.filter2Button.setBackgroundColor(filterButtonBackgroundColor)
        binding.filter2Button.setTextColor(filterButtonTextColor)

        binding.filter3Button.setBackgroundColor(filterButtonBackgroundColor)
        binding.filter3Button.setTextColor(filterButtonTextColor)

        binding.filter4Button.setBackgroundColor(filterButtonBackgroundColor)
        binding.filter4Button.setTextColor(filterButtonTextColor)

        binding.filter5Button.setBackgroundColor(filterButtonBackgroundColor)
        binding.filter5Button.setTextColor(filterButtonTextColor)

        binding.filter6Button.setBackgroundColor(filterButtonBackgroundColor)
        binding.filter6Button.setTextColor(filterButtonTextColor)

        val showSortButton = binding.filter1Button
        val showReadButton = binding.filter2Button
        val showViewButton = binding.filter3Button
        val showPublisherButton = binding.filter4Button
        val showResourceButton = binding.filter5Button
        val clearFiltersButton = binding.filter6Button


        // Set a click listener for the button to show the popup
        showSortButton.setOnClickListener {
            showInputPopup(R.layout.sort_button_view)
        }

        showReadButton.setOnClickListener {
            showInputPopup(R.layout.sort_by_read_time_view)
        }

        showViewButton.setOnClickListener {
            showInputPopup(R.layout.sort_by_view)
        }

        showPublisherButton.setOnClickListener {
            showInputPopup(R.layout.sort_publisher_view)
        }

        showResourceButton.setOnClickListener {
            showInputPopup(R.layout.sort_by_sources)
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            articleListViewModel.fetchArticles()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        clearFiltersButton.setOnClickListener {
            val alertdialog = AlertDialog.Builder(context)
                .setTitle("Clear Filters")
                .setMessage("Are you sure you want to clear filters?")
                .setPositiveButton("YES") { _, _ ->
                    articleListViewModel.clearFilters(requireView())
                    articleListViewModel.applyFilters(null)
                }
                .setNegativeButton("NO") { dialog, _ ->
                    dialog.dismiss() // Close the dialog when the user clicks "Cancel"
                }
                .setCancelable(true) // Allow dialog dismissal by clicking outside
                .show()

        }


        return binding.root
    }

    private fun cancelFilter(dialog: Dialog) {
        // Dismiss the dialog
        dialog.dismiss()
    }

    private fun showInputPopup(view: Int) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(view) // Replace with your input group layout
        dialog.setCancelable(true)

        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        Log.d("night", nightMode.toString())
        val backgroundColor = if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            Color.parseColor("#121212")
        } else {
            Color.WHITE
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(backgroundColor))

        // Get the window attributes and set width and height
        val lp = WindowManager.LayoutParams()
        val window = dialog.window
        lp.copyFrom(window?.attributes)
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT

        // Set the gravity to make the dialog appear at the bottom
        lp.gravity = Gravity.BOTTOM

        window?.attributes = lp

        //val radioButton = dialog.findViewById<RadioButton>(R.id.newest) // Replace with the actual ID
        //radioButton.isChecked = true
        if (view == R.layout.sort_by_view) {
            initialize_sortby(dialog)
        } else if (view == R.layout.sort_button_view) {
            initialize_date(dialog)
        } else if (view == R.layout.sort_by_read_time_view) {
            initialize_reading(dialog)
        } else if (view == R.layout.sort_publisher_view) {
            initialize_publisher(dialog)
        } else if (view == R.layout.sort_by_sources) {
            initialize_resource(dialog)
        }

        dialog.show()
    }


    private fun initialize_date(dialog: Dialog) {
        val dateOption = articleListViewModel.dateRelevance
        val applyButton = dialog.findViewById<MaterialButton>(R.id.apply_button)
        val anytimeButton = dialog.findViewById<RadioButton>(R.id.anytime)
        val hourButton = dialog.findViewById<RadioButton>(R.id.hour)
        val todayButton = dialog.findViewById<RadioButton>(R.id.today)
        val lastWeekButton = dialog.findViewById<RadioButton>(R.id.last_week)
        //val allButton = dialog.findViewById<RadioButton>(R.id.all)

        val radioGroup = dialog.findViewById<RadioGroup>(R.id.datePicker)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.anytime -> {
                    articleListViewModel.setDateRelevance(DateRelevance.ANYTIME)
                }

                R.id.hour -> {
                    articleListViewModel.setDateRelevance(DateRelevance.PASTHOUR)
                }

                R.id.today -> {
                    articleListViewModel.setDateRelevance(DateRelevance.TODAY)
                }

                R.id.last_week -> {
                    articleListViewModel.setDateRelevance(DateRelevance.LASTWEEK)
                }

//                R.id.all -> {
//                    articleListViewModel.setDateRelevance(DateRelevance.ALL)
//                }
            }
        }

        applyButton.setOnClickListener {
            // Dismiss the dialog or perform other actions if needed
            articleListViewModel.applyFilters(DateRelevance::class)
            dialog.dismiss()
        }

        when (dateOption) {
            DateRelevance.ANYTIME -> {
                anytimeButton.isChecked = true
                hourButton.isChecked = false
                todayButton.isChecked = false
                lastWeekButton.isChecked = false
                //allButton.isChecked = false
            }

            DateRelevance.PASTHOUR -> {
                anytimeButton.isChecked = false
                hourButton.isChecked = true
                todayButton.isChecked = false
                lastWeekButton.isChecked = false

            }

            DateRelevance.TODAY -> {
                anytimeButton.isChecked = false
                hourButton.isChecked = false
                todayButton.isChecked = true
                lastWeekButton.isChecked = false
                //allButton.isChecked = false
            }

            DateRelevance.LASTWEEK -> {
                anytimeButton.isChecked = false
                hourButton.isChecked = false
                todayButton.isChecked = false
                lastWeekButton.isChecked = true
                //allButton.isChecked = false
            }

//            DateRelevance.ALL -> {
//                anytimeButton.isChecked = false
//                todayButton.isChecked = false
//                lastWeekButton.isChecked = false
//                allButton.isChecked = true
//            }

        }
    }


    private fun initialize_reading(dialog: Dialog) {
        val readOption = articleListViewModel.readTimeOption
        val applyButton = dialog.findViewById<MaterialButton>(R.id.apply_button)
        val oneTothreeButton = dialog.findViewById<MaterialCheckBox>(R.id.read_choice1)
        val fourTosixButton = dialog.findViewById<MaterialCheckBox>(R.id.read_choice2)
        val sixPlusButton = dialog.findViewById<MaterialCheckBox>(R.id.read_choice3)

        val radioGroup = dialog.findViewById<RadioGroup>(R.id.button_group)

        applyButton.setOnClickListener {
            val readOptions = mutableSetOf<ReadTimeOption>()
            if (oneTothreeButton.isChecked) readOptions.add(ReadTimeOption.oneTOthree)
            if (fourTosixButton.isChecked) readOptions.add(ReadTimeOption.fourTOsix)
            if (sixPlusButton.isChecked) readOptions.add(ReadTimeOption.sixPlus)

            articleListViewModel.setReadTimeOption(readOptions)

            // Dismiss the dialog or perform other actions if needed
            // not the cleanest solution, but for now to get type just do this XD
            articleListViewModel.applyFilters(ReadTimeOption::class)
            dialog.dismiss()
        }

        if (readOption.contains(ReadTimeOption.oneTOthree)) oneTothreeButton.isChecked = true
        if (readOption.contains(ReadTimeOption.fourTOsix)) fourTosixButton.isChecked = true
        if (readOption.contains(ReadTimeOption.sixPlus)) sixPlusButton.isChecked = true
    }

    private fun initialize_publisher(dialog: Dialog) {
        val publishers = articleListViewModel.publishers
        val checkBoxContainer = dialog.findViewById<RadioGroup>(R.id.button_group)
        val applyButton = dialog.findViewById<MaterialButton>(R.id.apply_button)
        val radioGroup = dialog.findViewById<RadioGroup>(R.id.button_group)

        // Create a set to track selected publishers temporarily
        val selectedPublishers: MutableSet<String> = articleListViewModel.publisherOption.toMutableSet()

        // Create a listener to handle checkbox clicks
        val checkBoxClickListener = View.OnClickListener { view ->
            if (view is CheckBox) {
                if (view.isChecked) {
                    // Checkbox is checked, add the publisher to the temporary set
                    selectedPublishers.add(view.text.toString())
                    Log.d("heree!", "bad")
                } else {
                    // Checkbox is unchecked, remove the publisher from the temporary set
                    selectedPublishers.remove(view.text.toString())
                }
            }
        }

        // Set the listener for each checkbox
        for (publisher in publishers) {
            val checkBox = CheckBox(requireContext())
            checkBox.text = publisher
            checkBoxContainer.addView(checkBox)
            checkBox.isChecked = selectedPublishers.contains(publisher) // Check if the publisher is already selected
            checkBox.setOnClickListener(checkBoxClickListener)
        }

        // Set the click listener for the Apply button
        applyButton.setOnClickListener {
            // Apply the selected publishers to the ViewModel only when Apply is clicked
            articleListViewModel.setPublisherOption(selectedPublishers)

            // Dismiss the dialog or perform other actions if needed
            articleListViewModel.applyFilters(null)
            dialog.dismiss()
        }
    }


    private fun initialize_sortby(dialog: Dialog) {
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
            articleListViewModel.applyFilters(SortByOption::class)
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

    private fun initialize_resource(dialog: Dialog) {

        val resourceOption = articleListViewModel.resourceOption
        val applyButton = dialog.findViewById<MaterialButton>(R.id.apply_button)
        val googleButton = dialog.findViewById<MaterialCheckBox>(R.id.google)
        val redditButton = dialog.findViewById<MaterialCheckBox>(R.id.reddit)
        val twitterButton = dialog.findViewById<MaterialCheckBox>(R.id.twitter)

        applyButton.setOnClickListener {
            val sources = mutableSetOf<ResourceOption>()
            if (googleButton.isChecked) sources.add(ResourceOption.Google)
            if (redditButton.isChecked) sources.add(ResourceOption.Reddit)
            if (twitterButton.isChecked) sources.add(ResourceOption.Twitter)

            articleListViewModel.setResourceOption(sources)

            // Dismiss the dialog or perform other actions if needed
            articleListViewModel.fetchArticles()
            dialog.dismiss()
        }

        if (resourceOption.contains(ResourceOption.Google)) googleButton.isChecked = true
        if (resourceOption.contains(ResourceOption.Reddit)) redditButton.isChecked = true
        if (resourceOption.contains(ResourceOption.Twitter)) twitterButton.isChecked = true

    }


}

