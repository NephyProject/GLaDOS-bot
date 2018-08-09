package jp.nephy.glados.core.api.niconico.model

import com.rometools.rome.feed.synd.SyndFeed

class Ranking(private val feed: SyndFeed, name: String) {
    val title by lazy { feed.title!! }
    val link by lazy { feed.link!! }

    val videos by lazy { feed.entries.map { Video(it, name) } }
}
