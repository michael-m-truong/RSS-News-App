package com.bignerdranch.android.newsapp

import android.app.AlertDialog
import android.app.Dialog
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.*
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.android.newsapp.database.SavedArticles
import com.bignerdranch.android.newsapp.databinding.FragmentArticleListBinding
import com.bignerdranch.android.newsapp.databinding.FragmentSavedNewsfeedListBinding
import com.bignerdranch.android.newsapp.models.DateRelevance
import com.bignerdranch.android.newsapp.models.ReadTimeOption
import com.bignerdranch.android.newsapp.models.ResourceOption
import com.bignerdranch.android.newsapp.models.SortByOption
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.Date


private const val TAG = "ArticleListFragment"

class ArticleListFragment : Fragment() {

    private val articleListViewModel: ArticleListViewModel by viewModels()
    private lateinit var articleAdapter: ArticleListAdapter // Assuming you have an ArticleAdapter
    private lateinit var recyclerView: RecyclerView
    private val savedArticlesListViewModel: SavedArticlesListViewModel by viewModels()

    private var _savedArticleBinding: FragmentSavedNewsfeedListBinding? = null
    private val savedArticleBinding
        get() = checkNotNull(_savedArticleBinding) {
            "Cannot access binding because it is null. Is the view Visible?"
        }

    private lateinit var loadingProgressBar: ProgressBar

    override fun onPause() {
        super.onPause()

        // Start a coroutine to update the Newsfeed date in the database
        lifecycleScope.launch {
            articleListViewModel.updateLastCheckedDate()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Perform data fetching or any other setup tasks here
        articleListViewModel.fetchArticles()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                //menuInflater.inflate(R.menu.fragment_newsfeed_detail, menu)
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
                    else -> false
                }

            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        val actionBar = (activity as AppCompatActivity?)!!.supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(false)
        super.onDestroyView()
    }

    fun init_styles() {
        val actionBar = (activity as AppCompatActivity?)!!.supportActionBar
        actionBar?.title = ArticleListViewModel.newsfeedTitle
        actionBar?.setDisplayHomeAsUpEnabled(true)
        val upArrow = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_arrow_forward_24)
        actionBar?.setHomeAsUpIndicator(upArrow)

        //setHasOptionsMenu(true)
        // Set ActionBar text color based on night mode
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val textColor = if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            Color.WHITE // Night mode color
        } else {
            Color.BLACK // Day mode color
        }

        if (actionBar != null) {
            val text: Spannable = SpannableString(actionBar.title)
            text.setSpan(
                ForegroundColorSpan(textColor),
                0,
                text.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            actionBar.title = text
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        init_styles()

        _savedArticleBinding = FragmentSavedNewsfeedListBinding.inflate(inflater, container, false)
        savedArticleBinding.savedNewsFeedList.layoutManager = LinearLayoutManager(context)

        swipeToSave()
        val binding = FragmentArticleListBinding.inflate(inflater, container, false)

        articleAdapter = ArticleListAdapter() // Initialize your RecyclerView adapter
        binding.articleRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.articleRecyclerView.adapter = articleAdapter

        // Show loading animation
        binding.loadingProgressBar.visibility = View.VISIBLE

        // Init recyclerview
        recyclerView = binding.articleRecyclerView
        loadingProgressBar = binding.loadingProgressBar

        // Observe the articles from the ViewModel and update the RecyclerView when they change
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                articleListViewModel.articles.collect { articles ->
                    articleAdapter.submitList(articles)
                    recyclerView.post {
                        recyclerView.scrollToPosition(0)
                    }
                }
            }
        }

        fun updateEmptyStateVisibility() {
            if (articleListViewModel.articles.value.isEmpty()) {
                binding.articleRecyclerView.visibility = View.GONE
                if (articleListViewModel.publishers.intersect(articleListViewModel.publisherOption).isEmpty() && !articleListViewModel.publisherOption.contains("INIT_NEWSFEED") && !articleListViewModel.publisherOption.contains("ALL_NEWSFEEDS")) {
                    binding.noPublishersView.visibility = View.VISIBLE
                    binding.emptyTextView.visibility = View.GONE
                }
                else {
                    binding.emptyTextView.visibility = View.VISIBLE
                    binding.noPublishersView.visibility = View.GONE
                }

            } else {
                binding.articleRecyclerView.visibility = View.VISIBLE
                binding.emptyTextView.visibility = View.GONE
                binding.noPublishersView.visibility = View.GONE
            }
        }

        articleListViewModel.onDataFiltered.observe(viewLifecycleOwner, Observer {
            updateEmptyStateVisibility()
            recyclerView.post {
                recyclerView.scrollToPosition(0)
            }
            /*if (articleListViewModel.isFiltered) {
                val snackbarMessage = "Filtered by reading time"
                Snackbar.make(requireView(), snackbarMessage, Snackbar.LENGTH_SHORT).show()
            }
            articleListViewModel.isFiltered = false*/
        })

        // Listen for data fetching completion and hide the progress bar
        articleListViewModel.onDataFetched.observe(viewLifecycleOwner, Observer {
            // Hide loading progress bar and show the RecyclerView when data is ready
            binding.loadingProgressBar.visibility = View.INVISIBLE
            binding.articleRecyclerView.visibility = View.VISIBLE
            binding.swipeRefreshLayout.isRefreshing = false

            updateEmptyStateVisibility()
            recyclerView.post {
                recyclerView.scrollToPosition(0)
            }
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
            binding.swipeRefreshLayout.isRefreshing = true
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
            recyclerView.smoothScrollToPosition(0)
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
            recyclerView.smoothScrollToPosition(0)
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
        val checkBoxes = mutableListOf<CheckBox>()

        // Create a set to track selected publishers temporarily
        val selectedPublishers: MutableSet<String> = articleListViewModel.publisherOption

        // Create a listener to handle checkbox clicks
        /*val checkBoxClickListener = View.OnClickListener { view ->
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
        }*/

        // Create the "All" checkbox
        val allCheckBox = CheckBox(requireContext())
        allCheckBox.text = "All"
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Set paddingLeft
        allCheckBox.setPadding(8, 0, 0, 0);

        // Apply the layout parameters
        allCheckBox.layoutParams = layoutParams
        checkBoxContainer.addView(allCheckBox)

        // Add a listener to the "All" checkbox to handle checking/unchecking all other checkboxes
        allCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                for (checkBox in checkBoxes) {
                    checkBox.isEnabled = false
                    //checkBox.isChecked = true  // to make sure users know their selection saves
                }
            } else {
                for (checkBox in checkBoxes) {
                    checkBox.isEnabled = true
                    checkBox.isChecked = selectedPublishers.contains(checkBox.text.toString())
                }
            }
        }

        // Set the listener for each checkbox
        for (publisher in publishers) {
            val checkBox = CheckBox(requireContext())
            checkBoxes.add(checkBox)
            checkBox.text = publisher
            val layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            // Set paddingLeft
            checkBox.setPadding(8, 0, 0, 0);

            // Apply the layout parameters
            checkBox.layoutParams = layoutParams

            checkBoxContainer.addView(checkBox)
            Log.d("selectedpub",selectedPublishers.toString())
            if (selectedPublishers.contains("INIT_NEWSFEED")) {
                checkBox.isChecked = true
            }
            else if (selectedPublishers.contains(publisher)) {
                checkBox.isChecked = true // Check if the publisher is already selected
            }

            //checkBox.setOnClickListener(checkBoxClickListener)
        }

        allCheckBox.isChecked = selectedPublishers.contains("INIT_NEWSFEED") || selectedPublishers.contains("ALL_PUBLISHERS")


        // Set the click listener for the Apply button
        applyButton.setOnClickListener {
            // Apply the selected publishers to the ViewModel only when Apply is clicked
            if (allCheckBox.isChecked) {
                selectedPublishers.add("ALL_PUBLISHERS")
            }
            else {
                selectedPublishers.remove("ALL_PUBLISHERS")
                for (checkbox in checkBoxes) {
                    if (checkbox.isChecked) {
                        selectedPublishers.add(checkbox.text.toString())
                    }
                    else {
                        selectedPublishers.remove(checkbox.text.toString())
                    }
                }
            }

            Log.d("pubz",selectedPublishers.toString())
            articleListViewModel.publisherOption.remove("INIT_NEWSFEED")
            articleListViewModel.setPublisherOption(selectedPublishers)
            // Dismiss the dialog or perform other actions if needed
            recyclerView.smoothScrollToPosition(0)
            articleListViewModel.applyFilters(String::class)
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
            recyclerView.smoothScrollToPosition(0)
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

            // so articles of diff sources appear
            articleListViewModel.publisherOption.add("ALL_PUBLISHERS")

            // Dismiss the dialog or perform other actions if needed
            recyclerView.visibility = View.INVISIBLE
            loadingProgressBar.visibility = View.VISIBLE

            recyclerView.smoothScrollToPosition(0)
            articleListViewModel.fetchArticles()
            dialog.dismiss()
        }

        if (resourceOption.contains(ResourceOption.Google)) googleButton.isChecked = true
        if (resourceOption.contains(ResourceOption.Reddit)) redditButton.isChecked = true
        if (resourceOption.contains(ResourceOption.Twitter)) twitterButton.isChecked = true

    }

    private fun swipeToSave() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val savedArticle = savedArticlesListViewModel.getArticleByPosition(position)

                val alertdialog = AlertDialog.Builder(context)
                    .setTitle("Save Article")
                    .setMessage("Save Article?")
                    .setPositiveButton("Save") { _, _ ->
                        if (savedArticle != null) {
                            viewLifecycleOwner.lifecycleScope.launch {
                                savedArticlesListViewModel.addArticle(savedArticle)
                            }
                        }
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                        viewHolder.bindingAdapter?.notifyItemChanged(position)
                    }
                    .setCancelable(true)
                    .setOnCancelListener {
                        viewHolder.bindingAdapter?.notifyItemChanged(position)
                    }
                    .show()
            }

            // Customize the swipe appearance
            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val background = ColorDrawable(Color.RED)
                    val deleteIcon =
                        ContextCompat.getDrawable(requireContext(), R.drawable.baseline_delete_24)
                    val iconMargin = (itemView.height - deleteIcon?.intrinsicHeight!!) / 2

                    // Draw the red background
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    background.draw(c)
                    // Draw the delete icon if the swipe is in progress
                    if (isCurrentlyActive) {
                        deleteIcon.setBounds(
                            itemView.right - iconMargin - deleteIcon.intrinsicWidth,
                            itemView.top + iconMargin,
                            itemView.right - iconMargin,
                            itemView.bottom - iconMargin
                        )
                        deleteIcon.draw(c)
                    }
                }

                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(savedArticleBinding.savedNewsFeedList)
    }
}

