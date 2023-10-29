package com.bignerdranch.android.newsapp

data class Article(
    val headline: String,
    val link: String,
    val date: String,
    val publisher: String,
    val imgSrc: String?,
    val text: String
)