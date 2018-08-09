package jp.nephy.glados.core.audio.music

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack

interface PlayerLoadResultHandler {
    fun onLoadTrack(track: AudioTrack) {}
    fun onLoadPlaylist(playlist: AudioPlaylist) {}
    fun onNoResult() {}
    fun onFailed(exception: FriendlyException) {}
}
