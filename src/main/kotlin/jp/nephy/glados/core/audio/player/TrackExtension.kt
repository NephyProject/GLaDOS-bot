package jp.nephy.glados.core.audio.player

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import jp.nephy.glados.core.extensions.sumBy

val AudioTrack.remainingMillis
    get() = duration - position

val AudioTrack.data
    get() = userData as TrackUserData

val List<AudioTrack>.totalDurationMillis
    get() = sumBy { it.duration }

val AudioTrackInfo.effectiveTitle: String
    get() = title ?: identifier ?: uri
