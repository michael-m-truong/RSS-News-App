package com.bignerdranch.android.newsapp

import android.net.Uri
import java.util.Date

data class Article(
    val headline: String,
    var link: String,
    val date: String,
    val datetime: Date?,
    val publisher: String,
    val imgSrc: String?,
    var text: String
 ) {
    val articlePageUri: Uri
        get() = Uri.parse(link)
}
