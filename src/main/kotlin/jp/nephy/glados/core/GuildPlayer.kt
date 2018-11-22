package jp.nephy.glados.core

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import jp.nephy.glados.core.config.intOption
import jp.nephy.glados.core.plugins.SubscriptionClient
import jp.nephy.glados.core.plugins.extensions.config
import net.dv8tion.jda.core.audio.AudioSendHandler
import net.dv8tion.jda.core.entities.Guild
import java.util.concurrent.ConcurrentHashMap

private val players = ConcurrentHashMap<Guild, GuildPlayer>()

val Guild.player: GuildPlayer
    get() = players.getOrPut(this) {
        val initialVolume = config.intOption("player_volume", 10)
        GuildPlayer(this, initialVolume)
    }

class GuildPlayer internal constructor(val guild: Guild, initialVolume: Int) {
    companion object {
        val audioPlayerManager = DefaultAudioPlayerManager()
    }

    val audioPlayer: AudioPlayer = audioPlayerManager.createPlayer()

    init {
        guild.audioManager.connectionListener = SubscriptionClient.ConnectionEvent.create(guild)
        guild.audioManager.setReceivingHandler(SubscriptionClient.ReceiveAudio.create(this))
        guild.audioManager.sendingHandler = object: AudioSendHandler {
            private var frame: AudioFrame? = null

            override fun canProvide(): Boolean {
                frame = audioPlayer.provide()
                return frame != null
            }

            override fun provide20MsAudio() = frame?.data

            override fun isOpus() = true
        }

        audioPlayer.addListener(SubscriptionClient.AudioEvent.create(this))
        audioPlayer.volume = initialVolume
    }
}
