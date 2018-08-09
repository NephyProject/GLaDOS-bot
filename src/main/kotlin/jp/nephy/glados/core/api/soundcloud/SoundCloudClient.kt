package jp.nephy.glados.core.api.soundcloud

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import jp.nephy.glados.core.api.soundcloud.model.Charts
import jp.nephy.glados.core.api.soundcloud.param.ChartType
import jp.nephy.glados.core.api.soundcloud.param.Genre
import jp.nephy.glados.core.audio.music.*
import jp.nephy.jsonkt.JsonKt
import jp.nephy.jsonkt.JsonModel
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*

class SoundCloudClient(private val clientId: String) {
    private val httpClient = OkHttpClient()
    private val headers = Headers.Builder()
            .add("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36")
            .build()

    private inline fun <reified T: JsonModel> get(url: String): T {
        val response = httpClient.newCall(Request.Builder()
                .url(url)
                .headers(headers)
                .get()
                .build()
        ).execute()
        return JsonKt.parse(response.body()!!.string())
    }

    private fun getCharts(type: ChartType = ChartType.Top, genre: Genre = Genre.AllMusic, limit: Int = 50): Charts {
        return get("https://api-v2.soundcloud.com/charts?kind=${type.internalName}&genre=soundcloud:genres:${genre.internalName}&client_id=$clientId&limit=$limit")
    }

    fun play(guildPlayer: GuildPlayer, type: ChartType, genre: Genre) {
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
