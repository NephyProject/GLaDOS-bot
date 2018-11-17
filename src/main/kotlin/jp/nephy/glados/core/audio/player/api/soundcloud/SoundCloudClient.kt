package jp.nephy.glados.core.audio.player.api.soundcloud

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import io.ktor.client.request.get
import io.ktor.http.userAgent
import jp.nephy.glados.core.audio.player.api.soundcloud.param.ChartType
import jp.nephy.glados.core.audio.player.api.soundcloud.param.Genre
import jp.nephy.glados.core.audio.player.*
import jp.nephy.glados.httpClient
import jp.nephy.jsonkt.*
import jp.nephy.jsonkt.delegation.*
import java.util.*

class SoundCloudClient(private val clientId: String) {
    private suspend inline fun <reified T: JsonModel> get(url: String): T {
        return httpClient.get<String>(url) {
            userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36")
        }.parse()
    }

    private suspend fun getCharts(type: ChartType = ChartType.Top, genre: Genre = Genre.AllMusic, limit: Int = 50): ChartModel {
        return get("https://api-v2.soundcloud.com/charts?kind=${type.internalName}&genre=soundcloud:genres:${genre.internalName}&client_id=$clientId&limit=$limit")
    }

    suspend fun play(guildPlayer: GuildPlayer, type: ChartType, genre: Genre) {
        val groupId = Date().time
        getCharts(type, genre).collection.filter { PlayableURL.SoundCloud.match(it.track.permalinkUrl) }.forEach { charts ->
            guildPlayer.loadTrack(charts.track.permalinkUrl, TrackType.SoundCloudCharts, object: PlayerLoadResultHandler {
                override fun onLoadTrack(track: AudioTrack) {
                    track.data.also {
                        it.groupId = groupId
                        it.soundCloudCharts = charts
                    }
                    guildPlayer.controls += track
                }
            })
        }
    }
}
