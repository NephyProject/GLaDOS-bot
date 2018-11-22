package jp.nephy.glados.core.plugins.extensions.jda

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo

val AudioTrack.remainingMillis
    get() = duration - position

val List<AudioTrack>.totalDurationMillis
    get() = map { it.duration }.sum()

val AudioTrackInfo.effectiveTitle: String
    get() = title ?: identifier
