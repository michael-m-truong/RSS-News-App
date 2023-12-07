package com.bignerdranch.android.newsapp

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatDelegate


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        // Get the ActionBar
        val actionBar: ActionBar? = supportActionBar

        // Define ColorDrawable object and set it as the background
        val colorDrawable = ColorDrawable(Color.TRANSPARENT)
        actionBar?.setBackgroundDrawable(colorDrawable)
    }
}


