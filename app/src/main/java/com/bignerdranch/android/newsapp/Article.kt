package com.bignerdranch.android.newsapp

import android.net.Uri

data class Article(
    val headline: String,
    val link: String,
    val date: String,
    val publisher: String,
    val imgSrc: String?,
    val text: String
 ) {
    val articlePageUri: Uri
        get() = Uri.parse(link)
}
