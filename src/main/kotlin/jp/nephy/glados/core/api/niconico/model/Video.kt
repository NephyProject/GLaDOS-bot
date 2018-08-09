package jp.nephy.glados.core.api.niconico.model

import com.rometools.rome.feed.synd.SyndEntry

class Video(private val entry: SyndEntry, val rankingName: String) {
    val title by lazy { entry.title!! }
    val link by lazy { entry.link!! }
    val description by lazy { entry.description.value!! }
}
