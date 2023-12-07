package com.bignerdranch.android.newsapp

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
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
import kotlinx.coroutines.launch

class NewsFeedDetailFragment : Fragment() {

    //private lateinit var newsFeed: NewsFeed
    private var _binding: FragmentNewsfeedDetailBinding? = null
    private val args: NewsFeedDetailFragmentArgs by navArgs()


    private val newsFeedDetailViewModel: NewsFeedDetailViewModel by viewModels {
        NewsFeedDetailViewModelFactory(args.newsfeedId)
    }
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            FragmentNewsfeedDetailBinding.inflate(layoutInflater, container, false)

        return binding.root
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

                if (wordbank.size == 1 && wordbank.contains("")) {
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

                if (wordbank.size == 1 && wordbank.contains("")) {
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
                                oldNewsFeed.wordBank.remove(word)
                                oldNewsFeed.copy(wordBank = oldNewsFeed.wordBank)
                            }
                            chipGroup.removeView(chip)
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
                }
            }
        }
        //include a date for users to see not select


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun updateUi(newsFeed: NewsFeed) {
        binding.apply {

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
            if (wordbank != null && !(wordbank.size == 1 && wordbank.contains(""))) {
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_newsfeed_detail, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save -> {
                fragmentManager?.popBackStack()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}