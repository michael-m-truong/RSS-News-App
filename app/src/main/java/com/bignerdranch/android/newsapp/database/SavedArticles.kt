package com.bignerdranch.android.newsapp.database

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
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
)