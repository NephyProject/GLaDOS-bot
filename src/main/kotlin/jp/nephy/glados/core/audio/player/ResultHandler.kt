package jp.nephy.glados.core.audio.player

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import jp.nephy.glados.core.audio.player.api.niconico.model.Ranking
import jp.nephy.glados.core.audio.player.api.niconico.model.SearchResult
import jp.nephy.glados.core.audio.player.api.soundcloud.ChartModel
import jp.nephy.glados.core.audio.player.api.youtubedl.YouTubeDL

interface PlayerLoadResultHandler {
    fun onLoadTrack(track: AudioTrack) {}
    fun onLoadPlaylist(playlist: AudioPlaylist) {}
    fun onEmptyResult() {}
    fun onFailed(exception: FriendlyException) {}
}

interface PlayerSearchResultHandler {
    fun onFoundNiconicoResult(result: SearchResult) {}
    fun onFoundYouTubeResult(result: List<com.google.api.services.youtube.model.SearchResult>) {}
    fun onEmptyResult() {}
}

data class TrackUserData(private val track: AudioTrack, val type: TrackType, var groupId: Long? = null) {
    var onTrackEnd: (AudioTrack) -> Unit = {}

    val youTubeDL by lazy {
        YouTubeDL(track.info.uri)
    }
    var soundCloudCharts: ChartModel.Collection? = null
    var nicoNicoSearch: SearchResult.SearchData? = null
    var nicoNicoRanking: Ranking.Video? = null
    var youTubeSearch: com.google.api.services.youtube.model.SearchResult? = null
}
