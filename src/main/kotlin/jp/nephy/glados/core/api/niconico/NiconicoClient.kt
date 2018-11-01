package jp.nephy.glados.core.api.niconico

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import io.ktor.client.request.get
import io.ktor.http.encodeURLParameter
import io.ktor.http.userAgent
import jp.nephy.glados.core.api.niconico.model.Ranking
import jp.nephy.glados.core.api.niconico.model.SearchResult
import jp.nephy.glados.core.api.niconico.param.RankingCategory
import jp.nephy.glados.core.api.niconico.param.RankingPeriod
import jp.nephy.glados.core.api.niconico.param.RankingType
import jp.nephy.glados.core.api.niconico.param.SearchTarget
import jp.nephy.glados.core.audio.player.*
import jp.nephy.glados.httpClient
import jp.nephy.glados.userAgent
import jp.nephy.jsonkt.delegation.JsonModel
import jp.nephy.jsonkt.parse
import java.net.URL
import java.util.*

class NiconicoClient {
    private suspend inline fun <reified T: JsonModel> get(url: String): T {
        return httpClient.get<String>(url) {
            userAgent(userAgent)
        }.parse()
    }

    private fun getRanking(type: RankingType = RankingType.Top, period: RankingPeriod = RankingPeriod.Daily, category: RankingCategory = RankingCategory.All): Ranking {
        val url = URL("http://www.nicovideo.jp/ranking/${type.internalName}/${period.name}/${category.internalName}?rss=2.0&lang=ja-jp")
        val feed = SyndFeedInput().build(XmlReader(url))
        return Ranking(feed, "${category.friendlyName}${period.friendlyName}${type.friendlyName}")
    }

    fun play(guildPlayer: GuildPlayer, type: RankingType, period: RankingPeriod, category: RankingCategory) {
        val groupId = Date().time
        getRanking(type, period, category).videos.filter { PlayableURL.Niconico.match(it.link) }.forEach { ranking ->
            guildPlayer.loadTrack(ranking.link, TrackType.NicoNicoRanking, object: PlayerLoadResultHandler {
                override fun onLoadTrack(track: AudioTrack) {
                    track.data.also {
                        it.groupId = groupId
                        it.nicoNicoRanking = ranking
                    }
                    guildPlayer.controls += track
                }
            })
        }
    }

    suspend fun search(query: String, target: SearchTarget = SearchTarget.Title, limit: Int = 10): SearchResult {
        val url = "http://api.search.nicovideo.jp/api/v2/snapshot/video/contents/search?q=${query.encodeURLParameter()}&targets=${target.name.toLowerCase()}&fields=contentId,title,description,tags,categoryTags,viewCounter,mylistCounter,commentCounter,lengthSeconds&_sort=-viewCounter&_limit=$limit&_context=GLaDOS"
        return get(url)
    }
}
