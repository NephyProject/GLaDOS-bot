package jp.nephy.glados.feature.listener.music

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import jp.nephy.glados.component.audio.music.*
import jp.nephy.glados.component.helper.*
import jp.nephy.glados.component.helper.prompt.YesNoEmoji
import jp.nephy.glados.feature.ListenerFeature
import jp.nephy.glados.logger
import jp.nephy.utils.characterLength
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.util.concurrent.TimeUnit


class FindVideoURL: ListenerFeature() {
    private val playCommandSyntax = "^!(!)?play".toRegex()

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        val config = bot.config.getGuildConfig(event.guild)
        if (! config.option.useFindVideoURL || playCommandSyntax.containsMatchIn(event.message.contentDisplay)) {
            return
        }

        PlayableVideoURL.values().forEach eachUrl@{ url ->
            url.regexes.forEach {
                val result = it.find(event.message.contentDisplay) ?: return@eachUrl

                helper.promptBuilder(event.channel, event.member) {
                    emojiPrompt<YesNoEmoji, YesNoEmoji>(
                            title = "動画URLを検出しました",
                            description = "プレイヤーのキューに追加しますか？",
                            timeoutSec = 30
                    ) { emoji, _, event ->
                        when (emoji) {
                            YesNoEmoji.Yes -> {
                                val guildPlayer = bot.playerManager.getGuildPlayer(event.guild)
                                guildPlayer.loadTrack(result.value, TrackType.UserRequest, object: PlayerLoadResultHandler {
                                    override fun onLoadTrack(track: AudioTrack) {
                                        event.channel.embedMention(event.member) {
                                            author("動画URLを検出しました")
                                            title("\"${track.info.effectiveTitle}\" (${track.info.length.toMilliSecondString()}) を再生キューに追加します")
                                            description {
                                                if ((guildPlayer.controls.isEmptyQueue && ! guildPlayer.controls.isPlaying) || guildPlayer.controls.currentTrack == track) {
                                                    "まもなく再生されます。"
                                                } else {
                                                    "再生まであと${guildPlayer.controls.queue.size + 1}曲 (およそ${guildPlayer.controls.totalDuration.toMilliSecondString()})"
                                                }
                                            }
                                            color(Color.Good)
                                            timestamp()
                                        }.deleteQueue(30, TimeUnit.SECONDS)

                                        guildPlayer.controls.add(track)
                                    }

                                    override fun onLoadPlaylist(playlist: AudioPlaylist) {
                                        event.channel.embedMention(event.member) {
                                            author("プレイリストURLが見つかりました")
                                            title("プレイリスト \"${playlist.name}\" (${playlist.tracks.size}曲, ${playlist.tracks.totalDuration.toMilliSecondString()}) を再生キューに追加します")

                                            description {
                                                if ((guildPlayer.controls.isEmptyQueue && ! guildPlayer.controls.isPlaying) || guildPlayer.controls.currentTrack == playlist.tracks.firstOrNull()) {
                                                    "まもなく再生されます。"
                                                } else {
                                                    "再生まであと${guildPlayer.controls.queue.size + 1}曲 (およそ${guildPlayer.controls.totalDuration.toMilliSecondString()})"
                                                }
                                            }

                                            playlist.tracks.forEachIndexed { i, audioTrack ->
                                                field("\n#${(i + 1).toString().padEnd(playlist.tracks.size.characterLength)}") { audioTrack.info.effectiveTitle }
                                            }
                                            color(Color.Good)
                                            timestamp()
                                        }.deleteQueue(30, TimeUnit.SECONDS)

                                        guildPlayer.controls.addAll(playlist.tracks)
                                    }

                                    override fun onNoResult() {
                                        event.channel.embedMention(event.member) {
                                            author("エラー")
                                            title("`${result.value}` の結果は見つかりませんでした。")
                                            color(Color.Bad)
                                            timestamp()
                                        }.deleteQueue(60, TimeUnit.SECONDS)
                                    }

                                    override fun onFailed(exception: FriendlyException) {
                                        event.channel.embedMention(event.member) {
                                            author("エラー")
                                            title("`${result.value}` の読み込みに失敗しました。")
                                            description { exception.localizedMessage }
                                            color(Color.Bad)
                                            timestamp()
                                        }.deleteQueue(60, TimeUnit.SECONDS)
                                    }
                                })
                            }
                            else -> {
                                logger.info { "\"${result.value}\" の再生を提案しましたが ${event.member.fullNameWithoutGuild}は拒否しました." }
                            }
                        }
                    }
                }

                return logger.info { "動画形式(${url.friendlyName})の文字列: ${result.value} を検出しました. [#${event.channel.name}] ${event.member.fullName}" }
            }
        }
    }
}
