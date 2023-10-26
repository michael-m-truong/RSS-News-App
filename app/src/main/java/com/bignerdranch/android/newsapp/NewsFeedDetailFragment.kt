package com.bignerdranch.android.newsapp

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.bignerdranch.android.newsapp.databinding.FragmentNewsfeedDetailBinding
import com.google.android.material.chip.Chip
import java.util.Date
import java.util.UUID
import kotlin.collections.*
class NewsFeedDetailFragment  : Fragment() {

    private lateinit var newsFeed: NewsFeed
    private var _binding: FragmentNewsfeedDetailBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        newsFeed = NewsFeed(
            id = UUID.randomUUID(),
            title = "",
            date = Date(),
            wordBank = mutableListOf<String>()
        )
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
                newsFeed = newsFeed.copy(title = text.toString())
            }

            addChipButton.setOnClickListener {
                val word = inputWord.text.toString().trim()
                if (word.isNotEmpty()) {
                    newsFeed.wordBank.add(word)
                    // Create and add a chip
                    val chip = Chip(requireContext())
                    chip.text = word
                    chip.isCloseIconVisible = true
                    chip.setOnCloseIconClickListener {
                        // Remove the word from the list and the chip
                        newsFeed.wordBank.remove(word)
                        chipGroup.removeView(chip)
                    }
                    chipGroup.addView(chip)
                    inputWord.text.clear() // Clear the input field
                }
            }

            inputWord.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                    val word = inputWord.text.toString().trim()
                    if (word.isNotEmpty()) {
                        newsFeed.wordBank.add(word)
                        val chip = Chip(requireContext())
                        chip.text = word
                        chip.isCloseIconVisible = true
                        chip.setOnCloseIconClickListener {
                            newsFeed.wordBank.remove(word)
                            chipGroup.removeView(chip)
                        }
                        chipGroup.addView(chip)
                        inputWord.text.clear()
                        return@setOnEditorActionListener true
                    }
                }
                return@setOnEditorActionListener false
            }
        }


        //include a date for users to see not select




    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}