package jp.nephy.glados.core.api.niconico.model

import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndFeed

class Ranking(private val feed: SyndFeed, name: String) {
    val title by lazy { feed.title!! }
    val link by lazy { feed.link!! }

    val videos by lazy { feed.entries.map { Video(it, name) } }

    data class Video(private val entry: SyndEntry, val rankingName: String) {
        val title by lazy { entry.title!! }
        val link by lazy { entry.link!! }
        val description by lazy { entry.description.value!! }
    }
}
