package com.bignerdranch.android.newsapp

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        customizeUI()
    }

    private fun customizeUI() {
        // Get the ActionBar
        val actionBar: ActionBar? = supportActionBar

        // Define ColorDrawable object and set it as the background
        val colorDrawable = ColorDrawable(Color.TRANSPARENT)
        actionBar?.setBackgroundDrawable(colorDrawable)

        actionBar?.setDisplayShowTitleEnabled(true) // Ensure the title is shown

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
}


