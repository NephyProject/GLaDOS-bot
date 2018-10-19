package jp.nephy.glados.features

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import jp.nephy.glados.config
import jp.nephy.glados.core.audio.music.GuildPlayer
import jp.nephy.glados.core.audio.music.PlayerLoadResultHandler
import jp.nephy.glados.core.audio.music.TrackType
import jp.nephy.glados.core.audio.music.player
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Listener
import jp.nephy.glados.core.isFalseOrNull
import jp.nephy.utils.randomChoice
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

class SoundBot: BotFeature() {
    companion object {
        private const val prefix = "."
        private val soundsPath = Paths.get("sounds")
        private val extensions = arrayOf("mp3", "wav", "ogg")

        fun listSounds(): List<Path> {
            return Files.list(soundsPath).toList()
        }
    }

    @Listener
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        val config = config.forGuild(event.guild)
        if (config?.boolOption("enable_soundbot").isFalseOrNull() || event.channel != config?.textChannel("bot")) {
            return
        }

        event.message.contentDisplay.lines().filter { it.startsWith(prefix) }.forEach {
            event.guild.player?.play(it.removePrefix(prefix))
        }
    }

    private fun GuildPlayer.playRandom() {
        val sound = listSounds().randomChoice()
        play(sound)
    }

    private fun GuildPlayer.play(filename: String) {
        if (filename == "random") {
            playRandom()
        } else {
            val fullPath = extensions.map { Paths.get(soundsPath.toString(), "$filename.$it") }.find { Files.exists(it) }
                    ?: return logger.warn { "不明なサウンドファイル: $filename" }

            play(fullPath)
        }
    }

    private fun GuildPlayer.play(path: Path) {
        loadTrack(path.toAbsolutePath().toString(), TrackType.Sound, object: PlayerLoadResultHandler {
            override fun onLoadTrack(track: AudioTrack) {
                controls += track
                logger.info { "サウンド: $path をロードしました." }
            }
        })
    }
}
