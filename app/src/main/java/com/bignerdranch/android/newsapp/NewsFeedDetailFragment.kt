package com.bignerdranch.android.newsapp

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.bignerdranch.android.newsapp.databinding.FragmentNewsfeedDetailBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import kotlin.collections.*
class NewsFeedDetailFragment  : Fragment() {

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding =
            FragmentNewsfeedDetailBinding.inflate(layoutInflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            newsfeedTitle.doOnTextChanged { text, _, _, _ ->
                newsFeedDetailViewModel.updateNewsFeed { oldNewsFeed ->
                    oldNewsFeed.copy(title = text.toString())
                }
            }

            addChipButton.setOnClickListener {
                val word = inputWord.text.toString().trim()
                if (word.isNotEmpty()) {
                    //newsFeed.wordBank.add(word)
                    // Create and add a chip
                    val chip = Chip(requireContext())
                    chip.text = word
                    chip.isCloseIconVisible = true
                    chip.setOnCloseIconClickListener {
                        // Remove the word from the list and the chip
                        //newsFeed.wordBank.remove(word)
                        newsFeedDetailViewModel.updateNewsFeed { oldNewsFeed ->
                            oldNewsFeed.wordBank.remove(word)
                            oldNewsFeed.copy(wordBank = oldNewsFeed.wordBank)
                        }
                        chipGroup.removeView(chip)

                    }
                    chipGroup.addView(chip)
                    inputWord.text.clear() // Clear the input field
                    newsFeedDetailViewModel.updateNewsFeed { oldNewsFeed ->
                        oldNewsFeed.wordBank.add(word)
                        oldNewsFeed.copy(wordBank = oldNewsFeed.wordBank)
                    }
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
                        chip.setOnCloseIconClickListener {
                            //newsFeed.wordBank.remove(word)
                            newsFeedDetailViewModel.updateNewsFeed { oldNewsFeed ->
                                oldNewsFeed.wordBank.remove(word)
                                oldNewsFeed.copy(wordBank = oldNewsFeed.wordBank)
                            }
                            chipGroup.removeView(chip)
                        }
                        chipGroup.addView(chip)
                        inputWord.text.clear()

                        newsFeedDetailViewModel.updateNewsFeed { oldNewsFeed ->
                            oldNewsFeed.wordBank.add(word)
                            oldNewsFeed.copy(wordBank = oldNewsFeed.wordBank)
                        }
                        return@setOnEditorActionListener true
                    }
                }
                return@setOnEditorActionListener false
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
            if (newsfeedTitle.text.toString() != newsFeed.title) {
                newsfeedTitle.setText(newsFeed.title)
            }
            chipGroup.removeAllViews() // Clear existing chips

            // Iterate over the wordBank and create Chips for each word
            for (word in newsFeed.wordBank) {
                val chip = Chip(requireContext())
                chip.text = word
                chip.isCloseIconVisible = true
                chipGroup.addView(chip)
            }
            //newsFeedDate.text = newsFeed.date.toString()
        }
    }
}