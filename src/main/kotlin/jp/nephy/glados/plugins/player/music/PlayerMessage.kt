package jp.nephy.glados.plugins.player.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState
import jp.nephy.glados.core.audio.player.*
import jp.nephy.glados.core.extensions.*
import jp.nephy.glados.core.extensions.messages.EmbedBuilder
import jp.nephy.glados.core.extensions.messages.HexColor
import jp.nephy.glados.core.plugins.Plugin
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object PlayerMessage: Plugin() {
    private fun EmbedBuilder.update(guildPlayer: GuildPlayer, track: AudioTrack) {
        val data = track.data
        val youTubeDLInfo = data.youTubeDL.info
        val soundCloud = data.soundCloudCharts
        val nico = data.nicoNicoSearch
        val nicoRanking = data.nicoNicoRanking

        title(track.info.effectiveTitle, track.info.uri)
        descriptionBuilder {
            appendln("by ${track.info.author}\n")
            if (soundCloud != null) {
                appendln("${soundCloud.track.description.take(150)}...")
                if (soundCloud.track.labelName != null) {
                    append("Released by:\n    ${soundCloud.track.labelName}")
                }
            } else if (nico != null) {
                appendln("登録タグ: ${nico.tags.split(" ").joinToString(" ") { "__**$it**__" }}")
                appendln("再生数: ${nico.viewCounter} / コメント数: ${nico.commentCounter} / マイリスト数: ${nico.mylistCounter}\n")
                append("${nico.description.replace("<.+?>".toRegex(), "").take(150)}...")
            } else if (youTubeDLInfo?.description != null) {
                append("${youTubeDLInfo.description!!.replace("<.+?>".toRegex(), "").take(150)}...")
            }
        }
        footer("${track.position.toMilliSecondString()} / ${track.duration.toMilliSecondString()} | ボリューム: ${guildPlayer.controls.volume}%")

        when {
            youTubeDLInfo?.thumbnailUrl != null -> thumbnail(youTubeDLInfo.thumbnailUrl!!)
            soundCloud?.track?.artworkUrl != null -> thumbnail(soundCloud.track.artworkUrl!!)
        }

        when (data.type) {
            TrackType.SoundCloudCharts -> {
                author("♪ Now Playing (${PlayableURL.SoundCloud.friendlyName} #${soundCloud?.track?.genre} を自動再生中)", PlayableURL.SoundCloud.url, PlayableURL.SoundCloud.faviconUrl)
                color(PlayableURL.SoundCloud.color)
            }
            TrackType.NicoNicoRanking -> {
                author("♪ Now Playing (${PlayableURL.SoundCloud.friendlyName} ${nicoRanking?.rankingName}を自動再生中)", PlayableURL.Niconico.url, PlayableURL.Niconico.faviconUrl)
                title(nicoRanking?.title ?: track.info.effectiveTitle, nicoRanking?.link ?: track.info.uri)
                color(PlayableURL.Niconico.color)
            }
            TrackType.UserRequest -> {
                val service = PlayableURL.values().find { it.match(track.info.uri) }
                if (service != null) {
                    author("♪ Now Playing (${service.friendlyName})", service.url, service.faviconUrl)
                    color(service.color)
                } else {
                    author("♪ Now Playing")
                    color(HexColor.Plain)
                }
            }
            TrackType.Sound -> {
            }
        }

        timestamp()
    }

    private val messageUpdateStatus = arrayOf(AudioTrackState.LOADING, AudioTrackState.SEEKING, AudioTrackState.PLAYING)

    override suspend fun onTrackStart(guildPlayer: GuildPlayer, player: AudioPlayer, track: AudioTrack) {
        logger.info { "[${track.sourceManager.sourceName}] 現在再生中の曲は \"${track.info.effectiveTitle}\" by ${track.info.author} (${track.info.length.toMilliSecondString()}) です。" }

        val data = track.data
        if (data.type == TrackType.Sound) {
            return
        }

        val message = guildPlayer.guildConfig.withTextChannel("bot") {
            it.message {
                embed {
                    update(guildPlayer, track)
                }
            }.await()
        } ?: return

        GlobalScope.launch {
            for (e in PlayerEmoji.values()) {
                message.addReaction(e.emoji).await()
            }
        }

        while (track.state in messageUpdateStatus) {
            delay(5000)

            message.edit {
                embed {
                    update(guildPlayer, track)
                }
            }.await()
        }

        message.delete().await()
    }

    override suspend fun onTrackEnd(guildPlayer: GuildPlayer, player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if (!endReason.mayStartNext) {
            logger.info { "[${track.sourceManager.sourceName}] \"${track.info.effectiveTitle}\" by ${track.info.author} (${track.info.length.toMilliSecondString()}) の再生が停止しました. (${endReason.name})" }
        }
    }

    override suspend fun onTrackStuck(guildPlayer: GuildPlayer, player: AudioPlayer, track: AudioTrack, thresholdMs: Long) {
        logger.error { "\"${track.info.title}\" by ${track.info.author} (${track.info.length.toMilliSecondString()}) の再生がスタックしました. (閾値: ${thresholdMs.toMilliSecondString()})" }
    }

    override suspend fun onTrackException(guildPlayer: GuildPlayer, player: AudioPlayer, track: AudioTrack, exception: FriendlyException) {
        if (track.data.type == TrackType.Sound) {
            return
        }

        logger.error(exception) { "トラック \"${track.info.effectiveTitle}\" の再生中に例外が発生しました。" }

        guildPlayer.guildConfig.withTextChannel("bot") {
            it.message {
                embed {
                    author("例外レポート")
                    title("\"${track.info.effectiveTitle}\" の再生中に例外が発生しました。")
                    description { "${exception.javaClass.canonicalName}: ${exception.localizedMessage}" }
                    color(HexColor.Bad)
                    timestamp()
                }
            }.launchAndDelete(30, TimeUnit.SECONDS)
        }
    }
}
