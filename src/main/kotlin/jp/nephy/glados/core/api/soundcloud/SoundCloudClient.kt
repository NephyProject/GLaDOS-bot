package jp.nephy.glados.core.api.soundcloud

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import jp.nephy.glados.core.api.soundcloud.model.Charts
import jp.nephy.glados.core.api.soundcloud.param.ChartType
import jp.nephy.glados.core.api.soundcloud.param.Genre
import jp.nephy.glados.core.audio.music.*
import jp.nephy.jsonkt.JsonModel
import jp.nephy.jsonkt.parse
import java.util.*

class SoundCloudClient(private val clientId: String) {
    private val httpClient = HttpClient(Apache)

    private suspend inline fun <reified T: JsonModel> get(url: String): T {
        return httpClient.get<String>(url) {
            header(HttpHeaders.UserAgent, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36")
        }.parse()
    }

    private suspend fun getCharts(type: ChartType = ChartType.Top, genre: Genre = Genre.AllMusic, limit: Int = 50): Charts {
        return get("https://api-v2.soundcloud.com/charts?kind=${type.internalName}&genre=soundcloud:genres:${genre.internalName}&client_id=$clientId&limit=$limit")
    }

    suspend fun play(guildPlayer: GuildPlayer, type: ChartType, genre: Genre) {
        val groupId = Date().time
        getCharts(type, genre).collection.filter { PlayableVideoURL.SoundCloud.match(it.track.permalinkUrl) }.forEach {
            guildPlayer.loadTrack(it.track.permalinkUrl, TrackType.SoundCloud, object: PlayerLoadResultHandler {
                override fun onLoadTrack(track: AudioTrack) {
                    track.groupIdSetter = groupId
                    track.soundCloudCacheSetter = it
                    guildPlayer.controls.add(track)
                }
            })
        }
    }
}
