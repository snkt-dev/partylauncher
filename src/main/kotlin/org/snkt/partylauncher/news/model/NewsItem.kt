package org.snkt.partylauncher.news.model

import kotlinx.serialization.Serializable
import org.snkt.partylauncher.news.util.NewsDateFormatter

@Serializable
data class NewsItem(
    val id: String,
    val title: String,
    val text: String,
    val imageUrl: String = "",
    val sourceUrl: String = "",
    val timestampEpochMs: Long = 0L
) {
    val formattedDate: String
        get() = NewsDateFormatter.format(timestampEpochMs)
}
