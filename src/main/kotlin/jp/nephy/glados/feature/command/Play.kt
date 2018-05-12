package jp.nephy.glados.feature.command

import com.jagrosh.jdautilities.command.CommandEvent
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import jp.nephy.glados.component.api.niconico.model.SearchResult
import jp.nephy.glados.component.audio.music.*
import jp.nephy.glados.component.helper.*
import jp.nephy.glados.feature.CommandFeature
import jp.nephy.glados.logger
import jp.nephy.utils.characterLength
import java.util.concurrent.TimeUnit


class Play: CommandFeature() {
    init {
        name = "play"
        help = "指定されたURLのメディアを再生します。"

        isMusicCommand = true
        requireSameChannel = true
        arguments = "<検索ワード|動画URL|プレイリストURL>"
    }

    override fun executeCommand(event: CommandEvent) {
        if (PlayableVideoURL.values().any { it.match(event.args) }) {
            guildPlayer.loadTrack(event.args, TrackType.UserRequest, object: PlayerLoadResultHandler {
                override fun onLoadTrack(track: AudioTrack) {
                    event.embedMention {
                        author("トラックが見つかりました")
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

                    logger.info { "${event.member.fullName}が `${track.info.effectiveTitle}` を再生キューに追加しました. (${track.sourceManager.sourceName})" }
                }

                override fun onLoadPlaylist(playlist: AudioPlaylist) {
                    if (playlist.selectedTrack != null) {
                        event.embedMention {
                            author("トラックが見つかりました")
                            title("\"${playlist.selectedTrack.info.effectiveTitle}\" (${playlist.selectedTrack.info.length.toMilliSecondString()}) を再生キューに追加します")
                            description {
                                if ((guildPlayer.controls.isEmptyQueue && ! guildPlayer.controls.isPlaying) || guildPlayer.controls.currentTrack == playlist.selectedTrack) {
                                    "まもなく再生されます。"
                                } else {
                                    "再生まであと${guildPlayer.controls.queue.size + 1}曲 (およそ${guildPlayer.controls.totalDuration.toMilliSecondString()})"
                                }
                            }
                            color(Color.Good)
                            timestamp()
                        }.deleteQueue(30, TimeUnit.SECONDS)

                        guildPlayer.controls.add(playlist.selectedTrack)
                    } else {
                        event.embedMention {
                            author("プレイリストが見つかりました")
                            title("プレイリスト \"${playlist.name}\" (${playlist.tracks.size}曲, ${playlist.tracks.totalDuration.toMilliSecondString()}) を再生キューに追加します")
                            playlist.tracks.take(20).forEachIndexed { i, audioTrack ->
                                field("#${(i + 1).toString().padEnd(playlist.tracks.size.characterLength)}") { audioTrack.info.effectiveTitle }
                            }
                            if (playlist.tracks.size > 20) {
                                field("...") { "#21以降のトラックは省略されました。" }
                            }
                            color(Color.Good)
                            timestamp()
                        }.deleteQueue(30, TimeUnit.SECONDS)

                        guildPlayer.controls.addAll(playlist.tracks)
                    }
                }

                override fun onNoResult() {
                    event.embedMention {
                        author("エラー")
                        title("`${event.args}` の結果は見つかりませんでした。")
                        color(Color.Bad)
                        timestamp()
                    }.deleteQueue(60, TimeUnit.SECONDS)
                }

                override fun onFailed(exception: FriendlyException) {
                    event.embedMention {
                        author("エラー")
                        title("`${event.args}` の読み込みに失敗しました。")
                        description { exception.localizedMessage }
                        color(Color.Bad)
                        timestamp()
                    }.deleteQueue(60, TimeUnit.SECONDS)
                }
            })
        } else {
            val (priority, word) = when {
                event.args.startsWith("n:") -> {
                    SearchPriority.Niconico to event.args.removePrefix("n:")
                }
                event.args.startsWith("y:") -> {
                    // "ytsearch:${event.args}"
                    SearchPriority.YouTube to event.args.removePrefix("y:")
                }
                else -> SearchPriority.Undefined to event.args
            }

            guildPlayer.searchTrack(word, priority, handler = object: PlayerSearchResultHandler {
                override fun onFoundNiconicoResult(result: SearchResult) {
                    helper.promptBuilder(event.textChannel, event.member) {
                        listPrompt(
                                result.data, result.data.first(), { "${it.title} (${it.lengthSeconds}秒)" }, { "${it.description.take(20)}..." },
                                author = "ニコニコ動画で${result.meta.totalCount}件の動画が見つかりました",
                                title = "\"$word\" の検索結果です",
                                color = Color.Niconico,
                                timeoutSec = 30
                        ) { selected, _, _ ->
                            guildPlayer.loadTrack("http://www.nicovideo.jp/watch/${selected.contentId}", TrackType.UserRequest, object: PlayerLoadResultHandler {
                                override fun onLoadTrack(track: AudioTrack) {
                                    track.nicoCacheSetter = selected

                                    event.embedMention {
                                        author("トラックが見つかりました")
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

                                    logger.info { "${event.member.fullName}が `${track.info.effectiveTitle}` を再生キューに追加しました. (${track.sourceManager.sourceName})" }
                                }

                                override fun onNoResult() {
                                    event.embedMention {
                                        author("エラー")
                                        title("`$word` の結果は見つかりませんでした。")
                                        color(Color.Bad)
                                        timestamp()
                                    }.deleteQueue(60, TimeUnit.SECONDS)
                                }

                                override fun onFailed(exception: FriendlyException) {
                                    event.embedMention {
                                        author("エラー")
                                        title("`$word` の読み込みに失敗しました。")
                                        description { exception.localizedMessage }
                                        color(Color.Bad)
                                        timestamp()
                                    }.deleteQueue(60, TimeUnit.SECONDS)
                                }
                            })
                        }
                    }
                }

                override fun onFoundYouTubeResult(result: List<com.google.api.services.youtube.model.SearchResult>) {
                    helper.promptBuilder(event.textChannel, event.member) {
                        listPrompt(
                                result, result.first(), { it.snippet.title }, { "${it.snippet.description.orEmpty().take(20)}..." },
                                author = "YouTubeで${result.size}件の動画が見つかりました",
                                title = "\"$word\" の検索結果です",
                                color = Color.YouTube,
                                timeoutSec = 30
                        ) { selected, _, _ ->
                            guildPlayer.loadTrack("https://www.youtube.com/watch?v=${selected.id.videoId}", TrackType.UserRequest, object: PlayerLoadResultHandler {
                                override fun onLoadTrack(track: AudioTrack) {
                                    track.youtubeCacheSetter = selected

                                    event.embedMention {
                                        author("トラックが見つかりました")
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

                                    logger.info { "${event.member.fullName}が `${track.info.effectiveTitle}` を再生キューに追加しました. (${track.sourceManager.sourceName})" }
                                }

                                override fun onNoResult() {
                                    event.embedMention {
                                        author("エラー")
                                        title("`$word` の結果は見つかりませんでした。")
                                        color(Color.Bad)
                                        timestamp()
                                    }.deleteQueue(60, TimeUnit.SECONDS)
                                }

                                override fun onFailed(exception: FriendlyException) {
                                    event.embedMention {
                                        author("エラー")
                                        title("`$word` の読み込みに失敗しました。")
                                        description { exception.localizedMessage }
                                        color(Color.Bad)
                                        timestamp()
                                    }.deleteQueue(60, TimeUnit.SECONDS)
                                }
                            })
                        }
                    }
                }

                override fun onNoResult() {
                    event.embedMention {
                        author("エラー")
                        title("`$word` の検索結果は見つかりませんでした。")
                        color(Color.Bad)
                        timestamp()
                    }.deleteQueue(60, TimeUnit.SECONDS)
                }
            })
        }
    }
}
