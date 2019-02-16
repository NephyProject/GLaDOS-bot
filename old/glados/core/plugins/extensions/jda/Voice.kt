package jp.nephy.glados.core.plugins.extensions.jda

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.VoiceChannel

fun Guild.joinVoiceChannel(channel: VoiceChannel) {
    audioManager.openAudioConnection(channel)
}

val Guild.currentVoiceChannel: VoiceChannel?
    get() = audioManager.connectedChannel ?: audioManager.queuedAudioConnection
