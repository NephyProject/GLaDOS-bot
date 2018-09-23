package jp.nephy.glados.core.api.niconico

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import jp.nephy.glados.core.api.niconico.model.Ranking
import jp.nephy.glados.core.api.niconico.model.SearchResult
import jp.nephy.glados.core.api.niconico.param.RankingCategory
import jp.nephy.glados.core.api.niconico.param.RankingPeriod
import jp.nephy.glados.core.api.niconico.param.RankingType
import jp.nephy.glados.core.api.niconico.param.SearchTarget
import jp.nephy.glados.core.audio.music.*
import jp.nephy.jsonkt.JsonModel
import jp.nephy.jsonkt.parse
import java.net.URL
import java.util.*

class NiconicoClient {
    private val httpClient = HttpClient(Apache)

    private suspend inline fun <reified T: JsonModel> get(url: String): T {
        return httpClient.get<String>(url) {
            header(HttpHeaders.UserAgent, "GLaDOS-bot/1.0.0 (+admin@nephy.jp)")
        }.parse()
    }

    private fun getRanking(type: RankingType = RankingType.Top, period: RankingPeriod = RankingPeriod.Daily, category: RankingCategory = RankingCategory.All): Ranking {
        val url = URL("http://www.nicovideo.jp/ranking/${type.internalName}/${period.name}/${category.internalName}?rss=2.0&lang=ja-jp")
        val feed = SyndFeedInput().build(XmlReader(url))
        return Ranking(feed, "${category.friendlyName}${period.friendlyName}${type.friendlyName}")
    }

    fun play(guildPlayer: GuildPlayer, type: RankingType, period: RankingPeriod, category: RankingCategory) {
        val groupId = Date().time
        getRanking(type, period, category).videos.filter { PlayableVideoURL.Niconico.match(it.link) }.forEach {
            guildPlayer.loadTrack(it.link, TrackType.NicoRanking, object: PlayerLoadResultHandler {
                override fun onLoadTrack(track: AudioTrack) {
                    track.groupIdSetter = groupId
                    track.nicoRankingCacheSetter = it
                    guildPlayer.controls.add(track)
                }
            })
        }
    }

    suspend fun search(query: String, target: SearchTarget = SearchTarget.Title, limit: Int = 10): SearchResult {
        val url = "http://api.search.nicovideo.jp/api/v2/snapshot/video/contents/search?q=$query&targets=${target.name.toLowerCase()}&fields=contentId,title,description,tags,categoryTags,viewCounter,mylistCounter,commentCounter,lengthSeconds&_sort=-viewCounter&_limit=$limit&_context=GLaDOS"
        return get(url)
    }
}
