package com.bignerdranch.android.newsapp

import android.app.AlertDialog
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.size
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.bignerdranch.android.newsapp.database.NewsFeed
import com.bignerdranch.android.newsapp.databinding.FragmentNewsfeedDetailBinding
import com.bignerdranch.android.newsapp.models.Filter
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class NewsFeedDetailFragment : Fragment() {

    //private lateinit var newsFeed: NewsFeed
    private var _binding: FragmentNewsfeedDetailBinding? = null
    private val args: NewsFeedDetailFragmentArgs by navArgs()

    private var originalNewsFeed: NewsFeed? = null

    private val newsFeedDetailViewModel: NewsFeedDetailViewModel by viewModels {
        NewsFeedDetailViewModelFactory(args.newsfeedId)
    }
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        init_styles()

        _binding =
            FragmentNewsfeedDetailBinding.inflate(layoutInflater, container, false)

        return binding.root
    }

    fun init_styles() {
        val actionBar = (activity as AppCompatActivity?)!!.supportActionBar
        actionBar?.title = "Edit newsfeed"
        // we can fix this later not impt
        /*if (feed == null) {
            actionBar?.title = "Create newsfeed"
        }
        if (feed != null && feed.wordBank.size == 0 && feed.excludeWordBank.size == 0 && feed.title.isEmpty()) {
            actionBar?.title = "Create newsfeed"
        }
        else {
            actionBar?.title = "Edit newsfeed"
        } */
        actionBar?.setDisplayHomeAsUpEnabled(true)
        val upArrow = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_arrow_forward_24)
        actionBar?.setHomeAsUpIndicator(upArrow)

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val theme = context?.theme

        // move it to binding.apply
        /*val translationX = binding.exactMatchButton.x
        binding.underline.translationX = translationX

        val underlineParams = binding.underline.layoutParams
        underlineParams.width = binding.exactMatchButton.width
        binding.underline.layoutParams = underlineParams */

        //init_styles()

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.fragment_newsfeed_detail, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    android.R.id.home -> {
                        //TODO make it so that it doenst save to viewmodel
                        showDiscardChangesDialog()
                        return true
                    }
                    R.id.save -> {
                        val actionBar = (activity as AppCompatActivity?)!!.supportActionBar
                        actionBar?.setDisplayHomeAsUpEnabled(false)
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                        return true
                    }
                    else -> false
                }

            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.root.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                binding.root.viewTreeObserver.removeOnPreDrawListener(this)

                binding.apply {
                    val translationX = exactMatchButton.x
                    underline.translationX = translationX

                    val underlineParams = underline.layoutParams
                    underlineParams.width = exactMatchButton.width
                    underline.layoutParams = underlineParams

                    exactMatchButton.setTextColor(Color.BLUE)

                    // ... (rest of your onViewCreated code)
                }

                return true
            }
        })

        binding.apply {

            newsfeedTitle.doOnTextChanged { text, _, _, _ ->
                newsFeedDetailViewModel.updateNewsFeed { oldNewsFeed ->
                    oldNewsFeed.copy(title = text.toString())
                }
            }

            inputWord.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                    val word = inputWord.text.toString().trim()
                    if (word.isNotEmpty()) {
                        //newsFeed.wordBank.add(word)
                        val chip = Chip(requireContext())
                        chip.text = word
                        chip.isCloseIconVisible = true
                        chip.chipMinHeight = 140f
                        chip.setOnCloseIconClickListener {
                            //newsFeed.wordBank.remove(word)
                            if (newsFeedDetailViewModel.filterState == Filter.EXACT) {
                                newsFeedDetailViewModel.updateNewsFeed { oldNewsFeed ->
                                    oldNewsFeed.wordBank.remove(word)
                                    oldNewsFeed.copy(wordBank = oldNewsFeed.wordBank)
                                }
                            } else if (newsFeedDetailViewModel.filterState == Filter.EXCLUDE) {
                                newsFeedDetailViewModel.updateNewsFeed { oldNewsFeed ->
                                    oldNewsFeed.excludeWordBank.remove(word)
                                    oldNewsFeed.copy(excludeWordBank = oldNewsFeed.excludeWordBank)
                                }
                            }
                            if(newsFeedDetailViewModel.newsFeed.value!!.wordBank.size == 0) {
                                if (newsFeedDetailViewModel.filterState == Filter.EXACT) {
                                    val addKeywordsChip = Chip(requireContext())
                                    addKeywordsChip.text = getString(R.string.helper_chip_exact)
                                    addKeywordsChip.chipMinHeight = 140f
                                    addKeywordsChip.isCloseIconVisible = false // This chip won't be removable
                                    addKeywordsChip.isClickable = false // Make the chip unclickable
                                    addKeywordsChip.isFocusable = false // Make the chip unfocusable
                                    addKeywordsChip.id = resources.getIdentifier("helper_exact", "id", requireContext().packageName)
                                    chipGroup.addView(addKeywordsChip)
                                }
                                else if (newsFeedDetailViewModel.filterState == Filter.EXCLUDE) {
                                    val addKeywordsChip = Chip(requireContext())
                                    addKeywordsChip.text = getString(R.string.helper_chip_exclude)
                                    addKeywordsChip.chipMinHeight = 140f
                                    addKeywordsChip.isCloseIconVisible = false // This chip won't be removable
                                    addKeywordsChip.isClickable = false // Make the chip unclickable
                                    addKeywordsChip.isFocusable = false // Make the chip unfocusable
                                    addKeywordsChip.id = resources.getIdentifier("helper_exclude", "id", requireContext().packageName)
                                    chipGroup.addView(addKeywordsChip)
                                }

                            }
//                            newsFeedDetailViewModel.updateNewsFeed { oldNewsFeed ->
//                                oldNewsFeed.wordBank.remove(word)
//                                oldNewsFeed.copy(wordBank = oldNewsFeed.wordBank)
//                            }
                            chipGroup.removeView(chip)
                        }
                        chipGroup.addView(chip)
                        val helperExactText = getString(R.string.helper_chip_exact)
                        val helperExcludeText = getString(R.string.helper_chip_exclude)

                        // Remove the chip with the helper_exact text if it exists
                        var chipToRemove: Chip? = null
                        for (i in 0 until chipGroup.childCount) {
                            val chip = chipGroup.getChildAt(i) as? Chip
                            if (chip?.text == helperExactText) {
                                chipToRemove = chip
                                break
                            }
                        }

                        chipToRemove?.let {
                            chipGroup.removeView(it)
                        }

                        // Remove the chip with the helper_exclude text if it exists
                        chipToRemove = null
                        for (i in 0 until chipGroup.childCount) {
                            val chip = chipGroup.getChildAt(i) as? Chip
                            if (chip?.text == helperExcludeText) {
                                chipToRemove = chip
                                break
                            }
                        }

                        chipToRemove?.let {
                            chipGroup.removeView(it)
                        }


                        inputWord.text?.clear()

                        if (newsFeedDetailViewModel.filterState == Filter.EXACT) {
                            newsFeedDetailViewModel.updateNewsFeed { oldNewsFeed ->
                                oldNewsFeed.wordBank.add(word)
                                oldNewsFeed.copy(wordBank = oldNewsFeed.wordBank)
                            }
                        }
                        if (newsFeedDetailViewModel.filterState == Filter.EXCLUDE) {
                            newsFeedDetailViewModel.updateNewsFeed { oldNewsFeed ->
                                oldNewsFeed.excludeWordBank.add(word)
                                oldNewsFeed.copy(excludeWordBank = oldNewsFeed.excludeWordBank)
                            }
                        }
                        return@setOnEditorActionListener true
                    }
                }
                return@setOnEditorActionListener false
            }

            exactMatchButton.setOnClickListener {
                // Move the underline to the position of the exactMatchButton
                val translationX = exactMatchButton.x
                underline.animate().translationX(translationX).setDuration(200).start()
                Log.d("exact", "exact")

                // Change the text color of the exactMatchButton
                exactMatchButton.setTextColor(Color.BLUE)

                val typedValue = TypedValue()
                context?.theme?.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
                Log.d("exact", "Resolved color: ${typedValue.data}")
                excludeButton.setTextColor(typedValue.data)

                // Dynamically set the width of the underline view to match the width of the exactMatchButton
                val underlineParams = underline.layoutParams
                underlineParams.width = exactMatchButton.width
                underline.layoutParams = underlineParams
                newsFeedDetailViewModel.filterState = Filter.EXACT

                // Handle button click action
                chipGroup.removeAllViews()

                val wordbank = newsFeedDetailViewModel.newsFeed.value?.wordBank!!

                if (wordbank.size == 0) {
                    val addKeywordsChip = Chip(requireContext())
                    addKeywordsChip.text = getString(R.string.helper_chip_exact)
                    addKeywordsChip.chipMinHeight = 140f
                    addKeywordsChip.isCloseIconVisible = false // This chip won't be removable
                    addKeywordsChip.isClickable = false // Make the chip unclickable
                    addKeywordsChip.isFocusable = false // Make the chip unfocusable
                    chipGroup.addView(addKeywordsChip)
                }

                for (word in newsFeedDetailViewModel.newsFeed.value?.wordBank!!) {
                    if (word.isNotEmpty()) { // Add this condition to skip empty words
                        val chip = Chip(requireContext())
                        chip.text = word
                        chip.chipMinHeight = 140f
                        chip.isCloseIconVisible = true

                        chip.setOnCloseIconClickListener {
                            // Remove the word from the list and the chip
                            newsFeedDetailViewModel.updateNewsFeed { oldNewsFeed ->
                                Log.d("sizees", chipGroup.size.toString())
                                oldNewsFeed.wordBank.remove(word)
                                oldNewsFeed.copy(wordBank = oldNewsFeed.wordBank)
                            }
                            chipGroup.removeView(chip)

                            if( newsFeedDetailViewModel.newsFeed.value!!.wordBank.size == 0) {
                                if (newsFeedDetailViewModel.filterState == Filter.EXACT) {
                                    val addKeywordsChip = Chip(requireContext())
                                    addKeywordsChip.text = getString(R.string.helper_chip_exact)
                                    addKeywordsChip.chipMinHeight = 140f
                                    addKeywordsChip.isCloseIconVisible = false // This chip won't be removable
                                    addKeywordsChip.isClickable = false // Make the chip unclickable
                                    addKeywordsChip.isFocusable = false // Make the chip unfocusable
                                    addKeywordsChip.id = resources.getIdentifier("helper_exact", "id", requireContext().packageName)
                                    chipGroup.addView(addKeywordsChip)
                                }
                            }
                        }

                        chipGroup.addView(chip)
                    }
                }
            }

            excludeButton.setOnClickListener {
                // Move the underline to the position of the excludeButton
                val translationX = excludeButton.x
                underline.animate().translationX(translationX).setDuration(200).start()
                Log.d("exclude", "exclude")

                // Change the text color of the excludeButton
                excludeButton.setTextColor(Color.BLUE)

                val typedValue = TypedValue()
                context?.theme?.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
                Log.d("exact", "Resolved color: ${typedValue.data}")
                exactMatchButton.setTextColor(typedValue.data)

                // Dynamically set the width of the underline view to match the width of the excludeButton
                val underlineParams = underline.layoutParams
                underlineParams.width = excludeButton.width
                underline.layoutParams = underlineParams
                newsFeedDetailViewModel.filterState = Filter.EXCLUDE


                chipGroup.removeAllViews()

                val wordbank = newsFeedDetailViewModel.newsFeed.value?.excludeWordBank!!

                if (wordbank.size == 0) {
                    val addKeywordsChip = Chip(requireContext())
                    addKeywordsChip.text = getString(R.string.helper_chip_exclude)
                    addKeywordsChip.chipMinHeight = 140f
                    addKeywordsChip.isCloseIconVisible = false // This chip won't be removable
                    addKeywordsChip.isClickable = false // Make the chip unclickable
                    addKeywordsChip.isFocusable = false // Make the chip unfocusable
                    chipGroup.addView(addKeywordsChip)
                }

                for (word in newsFeedDetailViewModel.newsFeed.value?.excludeWordBank!!) {
                    if (word.isNotEmpty()) { // Add this condition to skip empty words
                        val chip = Chip(requireContext())
                        chip.text = word
                        chip.chipMinHeight = 140f
                        chip.isCloseIconVisible = true

                        chip.setOnCloseIconClickListener {
                            // Remove the word from the list and the chip
                            newsFeedDetailViewModel.updateNewsFeed { oldNewsFeed ->
                                oldNewsFeed.excludeWordBank.remove(word)
                                oldNewsFeed.copy(excludeWordBank = oldNewsFeed.excludeWordBank)
                            }
                            chipGroup.removeView(chip)

                            if(newsFeedDetailViewModel.newsFeed.value!!.excludeWordBank.size == 0) {
                                if (newsFeedDetailViewModel.filterState == Filter.EXCLUDE) {
                                    val addKeywordsChip = Chip(requireContext())
                                    addKeywordsChip.text = getString(R.string.helper_chip_exclude)
                                    addKeywordsChip.chipMinHeight = 140f
                                    addKeywordsChip.isCloseIconVisible = false // This chip won't be removable
                                    addKeywordsChip.isClickable = false // Make the chip unclickable
                                    addKeywordsChip.isFocusable = false // Make the chip unfocusable
                                    addKeywordsChip.id = resources.getIdentifier("helper_exclude", "id", requireContext().packageName)
                                    chipGroup.addView(addKeywordsChip)
                                }

                            }
                        }

                        chipGroup.addView(chip)
                    }
                }
                // Handle button click action
            }


        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                newsFeedDetailViewModel.newsFeed.collect { newsFeed ->
                    newsFeed?.let { updateUi(it) }
                    Log.d("does this run", "nad")
                    if (newsFeed != null && !newsFeedDetailViewModel.isOriginalNewsFeedInitialized) {
                        originalNewsFeed = newsFeed.copy()
                        originalNewsFeed = originalNewsFeed!!.copy(
                            wordBank = originalNewsFeed!!.wordBank.toMutableList(),
                            excludeWordBank = originalNewsFeed!!.excludeWordBank.toMutableList()
                        )
                        newsFeedDetailViewModel.isOriginalNewsFeedInitialized = true
                    }
                }
            }
        }
        //include a date for users to see not select


    }

    override fun onDestroyView() {
        val actionBar = (activity as AppCompatActivity?)!!.supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(false)
        super.onDestroyView()
        _binding = null
    }


    private fun updateUi(newsFeed: NewsFeed) {
        binding.apply {

            //if (originalNewsFeed != null) {
                // Save the original state when it's not saved yet
            //}

            val translationX = exactMatchButton.x
            underline.translationX = translationX

            val underlineParams = underline.layoutParams
            underlineParams.width = exactMatchButton.width
            underline.layoutParams = underlineParams

            if (newsfeedTitle.text.toString() != newsFeed.title) {
                newsfeedTitle.setText(newsFeed.title)
            }
            chipGroup.removeAllViews() // Clear existing chips

            // Iterate over the wordBank and create Chips for each word
            var wordbank: MutableList<String>? = null
            if (newsFeedDetailViewModel.filterState == Filter.EXACT) {
                wordbank = newsFeed.wordBank
            } else if (newsFeedDetailViewModel.filterState == Filter.EXCLUDE) {
                wordbank = newsFeed.excludeWordBank
            }
            if (wordbank != null && !(wordbank.size == 0)) {
                Log.d("wordbank", wordbank.toString())
                Log.d("wordbankk",wordbank.size.toString())
                for (word in wordbank) {
                    if (word.isNotEmpty()) { // Add this condition to skip empty words
                        val chip = Chip(requireContext())
                        chip.text = word
                        chip.chipMinHeight = 140f
                        chip.isCloseIconVisible = true

                        chip.setOnCloseIconClickListener {
                            // Remove the word from the list and the chip
                            newsFeedDetailViewModel.updateNewsFeed { oldNewsFeed ->
                                oldNewsFeed.wordBank.remove(word)
                                oldNewsFeed.copy(wordBank = oldNewsFeed.wordBank)
                            }
                            chipGroup.removeView(chip)

                            if( newsFeedDetailViewModel.newsFeed.value!!.wordBank.size == 0) {
                                if (newsFeedDetailViewModel.filterState == Filter.EXACT) {
                                    val addKeywordsChip = Chip(requireContext())
                                    addKeywordsChip.text = getString(R.string.helper_chip_exact)
                                    addKeywordsChip.chipMinHeight = 140f
                                    addKeywordsChip.isCloseIconVisible = false // This chip won't be removable
                                    addKeywordsChip.isClickable = false // Make the chip unclickable
                                    addKeywordsChip.isFocusable = false // Make the chip unfocusable
                                    addKeywordsChip.id = resources.getIdentifier("helper_exact", "id", requireContext().packageName)
                                    chipGroup.addView(addKeywordsChip)
                                }
                            }
                            if( newsFeedDetailViewModel.newsFeed.value!!.excludeWordBank.size == 0) {
                                    if (newsFeedDetailViewModel.filterState == Filter.EXCLUDE) {
                                        val addKeywordsChip = Chip(requireContext())
                                        addKeywordsChip.text =
                                            getString(R.string.helper_chip_exclude)
                                        addKeywordsChip.chipMinHeight = 140f
                                        addKeywordsChip.isCloseIconVisible = false // This chip won't be removable
                                        addKeywordsChip.isClickable = false // Make the chip unclickable
                                        addKeywordsChip.isFocusable = false // Make the chip unfocusable
                                        addKeywordsChip.id = resources.getIdentifier(
                                            "helper_exclude",
                                            "id",
                                            requireContext().packageName
                                        )
                                        chipGroup.addView(addKeywordsChip)
                                    }

                            }
                        }

                        chipGroup.addView(chip)
                    }
                }
            }
            else {
                if (newsFeedDetailViewModel.filterState == Filter.EXACT) {
                    val addKeywordsChip = Chip(requireContext())
                    addKeywordsChip.text = getString(R.string.helper_chip_exact)
                    addKeywordsChip.chipMinHeight = 140f
                    addKeywordsChip.isCloseIconVisible = false // This chip won't be removable
                    addKeywordsChip.isClickable = false // Make the chip unclickable
                    addKeywordsChip.isFocusable = false // Make the chip unfocusable
                    addKeywordsChip.id = resources.getIdentifier("helper_exact", "id", requireContext().packageName)
                    chipGroup.addView(addKeywordsChip)
                }
                else if (newsFeedDetailViewModel.filterState == Filter.EXCLUDE) {
                    val addKeywordsChip = Chip(requireContext())
                    addKeywordsChip.text = getString(R.string.helper_chip_exclude)
                    addKeywordsChip.chipMinHeight = 140f
                    addKeywordsChip.isCloseIconVisible = false // This chip won't be removable
                    addKeywordsChip.isClickable = false // Make the chip unclickable
                    addKeywordsChip.isFocusable = false // Make the chip unfocusable
                    addKeywordsChip.id = resources.getIdentifier("helper_exclude", "id", requireContext().packageName)
                    chipGroup.addView(addKeywordsChip)
                }
            }
            //newsFeedDate.text = newsFeed.date.toString()
        }
    }


    private fun hasChanges(): Boolean {
        Log.d("ognews", (originalNewsFeed != null && originalNewsFeed != newsFeedDetailViewModel.newsFeed.value).toString())
        Log.d("ognews", (originalNewsFeed != null).toString())
        Log.d("ognews", (originalNewsFeed != newsFeedDetailViewModel.newsFeed.value).toString())
        originalNewsFeed?.let { Log.d("ognews", it.title) }
        return originalNewsFeed != null && originalNewsFeed != newsFeedDetailViewModel.newsFeed.value
    }

    private fun discardChanges() {
        originalNewsFeed?.let { original ->
            newsFeedDetailViewModel.updateNewsFeed { _ ->
                // Revert to the original state
                original.copy()
            }
        }
        val actionBar = (activity as AppCompatActivity?)!!.supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(false)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            if (originalNewsFeed != null && originalNewsFeed!!.wordBank.size == 0 && originalNewsFeed!!.excludeWordBank.size == 0 && originalNewsFeed!!.title.isEmpty()) {
                newsFeedDetailViewModel.deleteNewsFeed(originalNewsFeed!!.id)
            }
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

    }
    private fun showDiscardChangesDialog() {
        val hasChanges = hasChanges()
        if (hasChanges) {
            AlertDialog.Builder(requireContext())
                .setTitle("Discard Changes?")
                .setMessage("Are you sure you want to discard changes?")
                .setPositiveButton("Yes") { _, _ ->
                    // User clicked Yes, discard changes and go back
                    discardChanges()
                }
                .setNegativeButton("No", null)
                .show()
        } else {
            // No changes, simply go back
            val actionBar = (activity as AppCompatActivity?)!!.supportActionBar
            actionBar?.setDisplayHomeAsUpEnabled(false)
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                if (originalNewsFeed != null && originalNewsFeed!!.wordBank.size == 0 && originalNewsFeed!!.excludeWordBank.size == 0 && originalNewsFeed!!.title.isEmpty()) {
                    newsFeedDetailViewModel.deleteNewsFeed(originalNewsFeed!!.id)
                }
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}