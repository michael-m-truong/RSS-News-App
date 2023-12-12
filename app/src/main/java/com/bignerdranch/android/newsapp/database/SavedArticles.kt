package com.bignerdranch.android.newsapp.database

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bignerdranch.android.newsapp.models.Article
import com.bignerdranch.android.newsapp.models.ReadTimeOption
import com.bignerdranch.android.newsapp.models.ResourceOption
import java.util.Date

@Entity
data class SavedArticles (
    @PrimaryKey
    var link: String,
    val headline: String,
    val date: String,
    val datetime: Date?,
    val publisher: String,
    val imgSrc: String?,
    val publisherImgSrc: String?,
    var text: String,
    val source: ResourceOption,
    val dateAdded: Date?
) {
    constructor(article: Article) : this(
        link = article.link,
        headline = article.headline,
        date = article.date,
        datetime = article.datetime,  // You may need to initialize this property appropriately
        publisher = article.publisher,
        imgSrc = article.imgSrc,
        publisherImgSrc = article.publisherImgSrc,
        text = article.text,
        source = article.source,  // You may need to initialize this property appropriately
        dateAdded = article.dateAdded
    )
}
