package com.bignerdranch.android.newsapp.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bignerdranch.android.newsapp.models.ReadTimeOption
import com.bignerdranch.android.newsapp.models.ResourceOption
import java.util.Date
import java.util.UUID

@Entity
data class SavedArticles (
    @PrimaryKey
    val id: UUID,
    val headline: String,
    var link: String,
    val date: String,
    val datetime: Date?,
    val publisher: String,
    val imgSrc: String?,
    val publisherImgSrc: String?,
    var text: String,
    val source: ResourceOption,
    val dateAdded: Date?
)