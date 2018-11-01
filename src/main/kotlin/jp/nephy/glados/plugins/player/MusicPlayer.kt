package jp.nephy.glados.plugins.player

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import jp.nephy.glados.config
import jp.nephy.glados.core.api.niconico.NiconicoClient
import jp.nephy.glados.core.api.niconico.model.SearchResult
import jp.nephy.glados.core.api.niconico.param.RankingCategory
import jp.nephy.glados.core.api.niconico.param.RankingPeriod
import jp.nephy.glados.core.api.niconico.param.RankingType
import jp.nephy.glados.core.api.soundcloud.SoundCloudClient
import jp.nephy.glados.core.api.soundcloud.param.ChartType
import jp.nephy.glados.core.api.soundcloud.param.Genre
import jp.nephy.glados.core.audio.player.*
import jp.nephy.glados.core.extensions.*
import jp.nephy.glados.core.extensions.messages.HexColor
import jp.nephy.glados.core.extensions.messages.prompt.YesNoEmoji
import jp.nephy.glados.core.isFalseOrNull
import jp.nephy.glados.core.plugins.Plugin
import jp.nephy.glados.dispatcher
import jp.nephy.glados.plugins.internal.MessageCollector
import jp.nephy.glados.secret
import jp.nephy.utils.characterLength
import jp.nephy.utils.round
import jp.nephy.utils.sumBy
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.core.requests.restaction.MessageAction
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object MusicPlayer: Plugin() {
    private val niconico = NiconicoClient()
    private val soundCloud by lazy { SoundCloudClient(secret.forKey("soundcloud_client_id")) }
    private var skipForwardVote = 0
    private var clearVote = 0

    override suspend fun onReady(event: ReadyEvent) {
        for (it in event.jda.guilds) {
            it.player?.currentVoiceChannel
        }
    }

    @Command(channelType = Command.TargetChannelType.TextChannel, description = "指定されたメディアを再生します。", args = ["検索ワード|動画URL|プレイリストURL"], category = "Music Bot", checkArgsCount = false)
    suspend fun play(event: Command.Event) {
        val guildPlayer = event.guild?.player ?: return
        if (PlayableURL.values().any { it.match(event.args) } || event.args.startsWith("http")) {
            guildPlayer.loadTrack(event.args, TrackType.UserRequest, object: PlayerLoadResultHandler {
                override fun onLoadTrack(track: AudioTrack) {
                    event.reply {
                        embed {
                            author("トラックが見つかりました")
                            title("\"${track.info.effectiveTitle}\" (${track.info.length.toMilliSecondString()}) を再生キューに追加します")
                            description {
                                if ((guildPlayer.controls.isEmptyQueue && !guildPlayer.controls.isPlaying) || guildPlayer.controls.currentTrack == track) {
                                    "まもなく再生されます。"
                                } else {
                                    "再生まであと${guildPlayer.controls.queue.size + 1}曲 (およそ${guildPlayer.controls.totalDuration.toMilliSecondString()})"
                                }
                            }
                            color(HexColor.Good)
                            timestamp()
                        }
                    }.launchAndDelete(30, TimeUnit.SECONDS)

                    guildPlayer.controls += track

                    logger.info { "${event.member?.fullName}が `${track.info.effectiveTitle}` を再生キューに追加しました. (${track.sourceManager.sourceName})" }
                }

                override fun onLoadPlaylist(playlist: AudioPlaylist) {
                    if (playlist.selectedTrack != null) {
                        event.reply {
                            embed {
                                author("トラックが見つかりました")
                                title("\"${playlist.selectedTrack.info.effectiveTitle}\" (${playlist.selectedTrack.info.length.toMilliSecondString()}) を再生キューに追加します")
                                description {
                                    if ((guildPlayer.controls.isEmptyQueue && !guildPlayer.controls.isPlaying) || guildPlayer.controls.currentTrack == playlist.selectedTrack) {
                                        "まもなく再生されます。"
                                    } else {
                                        "再生まであと${guildPlayer.controls.queue.size + 1}曲 (およそ${guildPlayer.controls.totalDuration.toMilliSecondString()})"
                                    }
                                }
                                color(HexColor.Good)
                                timestamp()
                            }
                        }.launchAndDelete(30, TimeUnit.SECONDS)

                        guildPlayer.controls += playlist.selectedTrack
                    } else {
                        event.reply {
                            embed {
                                author("プレイリストが見つかりました")
                                title("プレイリスト \"${playlist.name}\" (${playlist.tracks.size}曲, ${playlist.tracks.totalDurationMillis.toMilliSecondString()}) を再生キューに追加します")
                                playlist.tracks.take(20).forEachIndexed { i, audioTrack ->
                                    field("#${(i + 1).toString().padEnd(playlist.tracks.size.characterLength)}") { audioTrack.info.effectiveTitle }
                                }
                                if (playlist.tracks.size > 20) {
                                    field("...") { "#21以降のトラックは省略されました。" }
                                }
                                color(HexColor.Good)
                                timestamp()
                            }
                        }.launchAndDelete(30, TimeUnit.SECONDS)

                        guildPlayer.controls.addAll(playlist.tracks)
                    }
                }

                override fun onEmptyResult() {
                    event.reply {
                        embed {
                            author("エラー")
                            title("`${event.args}` の結果は見つかりませんでした。")
                            color(HexColor.Bad)
                            timestamp()
                        }
                    }.launchAndDelete(60, TimeUnit.SECONDS)
                }

                override fun onFailed(exception: FriendlyException) {
                    event.reply {
                        embed {
                            author("エラー")
                            title("`${event.args}` の読み込みに失敗しました。")
                            description { exception.localizedMessage }
                            color(HexColor.Bad)
                            timestamp()
                        }
                    }.launchAndDelete(30, TimeUnit.SECONDS)
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
                    event.textChannel?.prompt(event.member!!) {
                        list(
                                result.data, result.data.first(), { "${it.title} (${it.lengthSeconds}秒)" }, { "${it.description.take(20)}..." },
                                author = "ニコニコ動画で${result.meta.totalCount}件の動画が見つかりました",
                                title = "\"$word\" の検索結果です",
                                color = HexColor.Niconico,
                                timeoutSec = 30
                        ) { selected, _, _ ->
                            guildPlayer.loadTrack("http://www.nicovideo.jp/watch/${selected.contentId}", TrackType.UserRequest, object: PlayerLoadResultHandler {
                                override fun onLoadTrack(track: AudioTrack) {
                                    track.data.nicoNicoSearch = selected

                                    event.reply {
                                        embed {
                                            author("トラックが見つかりました")
                                            title("\"${track.info.effectiveTitle}\" (${track.info.length.toMilliSecondString()}) を再生キューに追加します")
                                            description {
                                                if ((guildPlayer.controls.isEmptyQueue && !guildPlayer.controls.isPlaying) || guildPlayer.controls.currentTrack == track) {
                                                    "まもなく再生されます。"
                                                } else {
                                                    "再生まであと${guildPlayer.controls.queue.size + 1}曲 (およそ${guildPlayer.controls.totalDuration.toMilliSecondString()})"
                                                }
                                            }
                                            color(HexColor.Good)
                                            timestamp()
                                        }
                                    }.launchAndDelete(30, TimeUnit.SECONDS)

                                    guildPlayer.controls += track

                                    logger.info { "${event.member.fullName}が `${track.info.effectiveTitle}` を再生キューに追加しました. (${track.sourceManager.sourceName})" }
                                }

                                override fun onEmptyResult() {
                                    event.reply {
                                        embed {
                                            author("エラー")
                                            title("`$word` の結果は見つかりませんでした。")
                                            color(HexColor.Bad)
                                            timestamp()
                                        }
                                    }.launchAndDelete(60, TimeUnit.SECONDS)
                                }

                                override fun onFailed(exception: FriendlyException) {
                                    event.reply {
                                        embed {
                                            author("エラー")
                                            title("`$word` の読み込みに失敗しました。")
                                            description { exception.localizedMessage }
                                            color(HexColor.Bad)
                                            timestamp()
                                        }
                                    }.launchAndDelete(30, TimeUnit.SECONDS)
                                }
                            })
                        }
                    }
                }

                override fun onFoundYouTubeResult(result: List<com.google.api.services.youtube.model.SearchResult>) {
                    event.textChannel?.prompt(event.member!!) {
                        list(
                                result, result.first(), { it.snippet.title }, { "${it.snippet.description.orEmpty().take(20)}..." },
                                author = "YouTubeで${result.size}件の動画が見つかりました",
                                title = "\"$word\" の検索結果です",
                                color = HexColor.YouTube,
                                timeoutSec = 30
                        ) { selected, _, _ ->
                            guildPlayer.loadTrack("https://www.youtube.com/watch?v=${selected.id.videoId}", TrackType.UserRequest, object: PlayerLoadResultHandler {
                                override fun onLoadTrack(track: AudioTrack) {
                                    track.data.youTubeSearch = selected

                                    event.reply {
                                        embed {
                                            author("トラックが見つかりました")
                                            title("\"${track.info.effectiveTitle}\" (${track.info.length.toMilliSecondString()}) を再生キューに追加します")
                                            description {
                                                if ((guildPlayer.controls.isEmptyQueue && !guildPlayer.controls.isPlaying) || guildPlayer.controls.currentTrack == track) {
                                                    "まもなく再生されます。"
                                                } else {
                                                    "再生まであと${guildPlayer.controls.queue.size + 1}曲 (およそ${guildPlayer.controls.totalDuration.toMilliSecondString()})"
                                                }
                                            }
                                            color(HexColor.Good)
                                            timestamp()
                                        }
                                    }.launchAndDelete(30, TimeUnit.SECONDS)

                                    guildPlayer.controls += track

                                    logger.info { "${event.member.fullName}が `${track.info.effectiveTitle}` を再生キューに追加しました. (${track.sourceManager.sourceName})" }
                                }

                                override fun onEmptyResult() {
                                    event.reply {
                                        embed {
                                            author("エラー")
                                            title("`$word` の結果は見つかりませんでした。")
                                            color(HexColor.Bad)
                                            timestamp()
                                        }
                                    }.launchAndDelete(60, TimeUnit.SECONDS)
                                }

                                override fun onFailed(exception: FriendlyException) {
                                    event.reply {
                                        embed {
                                            author("エラー")
                                            title("`$word` の読み込みに失敗しました。")
                                            description { exception.localizedMessage }
                                            color(HexColor.Bad)
                                            timestamp()
                                        }
                                    }.launchAndDelete(30, TimeUnit.SECONDS)
                                }
                            })
                        }
                    }
                }

                override fun onEmptyResult() {
                    event.reply {
                        embed {
                            author("エラー")
                            title("`$word` の検索結果は見つかりませんでした。")
                            color(HexColor.Bad)
                            timestamp()
                        }
                    }.launchAndDelete(60, TimeUnit.SECONDS)
                }
            })
        }
    }

    @Command(channelType = Command.TargetChannelType.TextChannel, description = "ニコニコ動画のランキングを再生します。", category = "Music Bot")
    fun nico(event: Command.Event) {
        respondNico(event.textChannel ?: return, event.member ?: return, event.guild?.player ?: return)
    }

    private fun respondNico(channel: TextChannel, member: Member, guildPlayer: GuildPlayer) {
        channel.prompt(member) {
            emoji<RankingType, RankingType>(
                    title = "ニコニコ動画のランキングを再生します",
                    description = "ランキングの種類を選択してください。",
                    color = HexColor.Niconico,
                    timeoutSec = 30
            ) { rankingType, _, _ ->
                emoji<RankingPeriod, RankingPeriod>(
                        title = "ニコニコ動画のランキングを再生します",
                        description = "再生するニコニコ動画 ${rankingType.friendlyName}ランキングの集計期間を選択してください。",
                        color = HexColor.Niconico,
                        timeoutSec = 30
                ) { rankingPeriod, _, _ ->
                    enum(
                            RankingCategory.All,
                            title = "ニコニコ動画のランキングを再生します",
                            description = "再生するニコニコ動画 ${rankingPeriod.friendlyName}${rankingType.friendlyName}ランキングのカテゴリを選択してください。",
                            color = HexColor.Niconico,
                            timeoutSec = 60
                    ) { rankingCategory, _, _ ->
                        channel.reply(member) {
                            embed {
                                title("\"ニコニコ動画のランキングを再生します\"")
                                descriptionBuilder {
                                    appendln("${rankingCategory.friendlyName}カテゴリの${rankingPeriod.friendlyName}${rankingType.friendlyName}ランキングを再生します。")
                                    append("まもなく再生されます。")
                                }
                                color(HexColor.SoundCloud)
                            }
                        }.launchAndDelete(30, TimeUnit.SECONDS)

                        niconico.play(guildPlayer, rankingType, rankingPeriod, rankingCategory)

                        logger.info { "サーバ ${guildPlayer.guild.name} でニコニコ動画 ${rankingCategory.friendlyName}カテゴリの${rankingPeriod.friendlyName}${rankingType.name}ランキングをキューに追加します。" }
                    }
                }
            }
        }
    }

    @Command(aliases = ["sc"], channelType = Command.TargetChannelType.TextChannel, description = "SoundCloudのチャートを再生します。", category = "Music Bot")
    fun soundcloud(event: Command.Event) {
        respondSoundCloud(event.textChannel ?: return, event.member ?: return, event.guild?.player ?: return)
    }

    private fun respondSoundCloud(channel: TextChannel, member: Member, guildPlayer: GuildPlayer) {
        channel.prompt(member) {
            emoji<ChartType, ChartType>(
                    title = "SoundCloudのチャート TOP50を再生します",
                    description = "再生するチャートの種類を選択してください。",
                    color = HexColor.SoundCloud,
                    timeoutSec = 30
            ) { chartType, _, _ ->
                enum(
                        Genre.AllMusic,
                        title = "SoundCloudのチャート TOP50を再生します",
                        description = "再生するSoundCloud ${chartType.name}チャートのジャンルを選択してください。",
                        color = HexColor.SoundCloud,
                        timeoutSec = 60
                ) { genre, _, _ ->
                    channel.reply(member) {
                        embed {
                            title("SoundCloudのチャート TOP50を再生します")
                            descriptionBuilder {
                                appendln("${chartType.name}チャートの${genre.friendlyName}ジャンルのTOP50を再生します。")
                                append("まもなく再生されます。")
                            }
                            color(HexColor.SoundCloud)
                        }
                    }.launchAndDelete(30, TimeUnit.SECONDS)

                    GlobalScope.launch(dispatcher) {
                        soundCloud.play(guildPlayer, chartType, genre)
                    }

                    logger.info { "サーバ ${guildPlayer.guild.name} でSoundCloud ${genre.friendlyName}ジャンルの${chartType.friendlyName}チャートをキューに追加します。" }
                }
            }
        }
    }

    @Command(channelType = Command.TargetChannelType.TextChannel, description = "現在の再生キューを取得します。", category = "Music Bot")
    fun queue(event: Command.Event) {
        respondQueue(event.textChannel ?: return, event.member ?: return, event.guild?.player ?: return).launchAndDelete(30, TimeUnit.SECONDS)
    }

    private fun respondQueue(channel: TextChannel, member: Member, guildPlayer: GuildPlayer): MessageAction {
        return channel.reply(member) {
            embed {
                author("♪ 再生中のトラックとキュー")
                blankField()

                val playing = guildPlayer.controls.currentTrack
                val queue = guildPlayer.controls.queue
                var ms = playing?.remainingMillis ?: 0

                if (playing != null) {
                    title(playing.info.effectiveTitle, playing.info.uri)
                    descriptionBuilder {
                        if (playing.data.type != TrackType.UserRequest) {
                            appendln("このトラックは自動再生です。ユーザがリクエストしたトラックではありません。")
                            appendln("曲をリクエストすると このトラックの再生は中断されます。")
                        }
                        appendln("by ${playing.info.author}")
                        append("再生位置: ${playing.position.toMilliSecondString()} / ${playing.duration.toMilliSecondString()}")
                    }

                    if (playing.data.youTubeDL.info?.thumbnailUrl != null) {
                        thumbnail(playing.data.youTubeDL.info?.thumbnailUrl!!)
                    }
                } else {
                    title("再生中のトラックはありません。")
                }
                field("キューに入っているトラック") {
                    "総トラック数: ${queue.size}, 総再生時間: ${queue.sumBy { it.duration }.toMilliSecondString()}"
                }
                queue.take(20).forEachIndexed { i, it ->
                    field("#${(i + 1).toString().padEnd(queue.size.characterLength)}: ${it.info.effectiveTitle}") {
                        "長さ: ${it.duration.toMilliSecondString()} / 再生までおよそ ${ms.toMilliSecondString()}"
                    }
                    ms += it.duration
                }
                if (queue.size > 20) {
                    field("...") { "#20以降のトラックは省略されました。" }
                }
                color(HexColor.Good)
                timestamp()
            }
        }
    }

    @Command(channelType = Command.TargetChannelType.TextChannel, description = "現在接続中のボイスチャンネルにGLaDOSを呼びます。", category = "Music Bot")
    fun summon(event: Command.Event) {
        val guildPlayer = event.guild?.player ?: return

        reject(event.member!!.voiceState?.channel == guildPlayer.currentVoiceChannel) {
            event.embedError {
                "既に同じボイスチャンネルに参加しています。"
            }
        }

        guildPlayer.joinVoiceChannel(event.member.voiceState.channel)
    }

    override suspend fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        val guildPlayer = event.guild.player ?: return
        if (!guildPlayer.controls.isPlaying && !event.channelJoined.isNoOneExceptSelf) {
            guildPlayer.controls.resume()
        }
    }

    override suspend fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        val guildPlayer = event.guild.player ?: return
        if (guildPlayer.controls.isPlaying && event.channelLeft.isNoOneExceptSelf) {
            guildPlayer.controls.pause()
        }
    }

    override suspend fun onGuildMessageReactionAdd(event: GuildMessageReactionAddEvent) {
        if (event.user.isBotOrSelfUser) {
            return
        }

        val message = MessageCollector.latest(event.messageIdLong) ?: return
        if (!message.author.isSelfUser) {
            return
        }

        val emoji = PlayerEmoji.fromEmoji(event.reactionEmote.name) ?: return
        if (event.guild.selfMember.hasPermission(Permission.MESSAGE_MANAGE)) {
            event.reaction.removeReaction(event.user).launch()
        }

        val guildPlayer = event.guild.player ?: return
        if (!event.member.voiceState.inVoiceChannel() || event.member.voiceState.channel != guildPlayer.currentVoiceChannel) {
            event.channel.reply(event.member) {
                embed {
                    title("GLaDOSが再生しているボイスチャンネルに参加していないのでコマンドは実行できません。")
                    color(HexColor.Bad)
                }
            }.launchAndDelete(30, TimeUnit.SECONDS)
            return
        }

        when (emoji) {
            PlayerEmoji.Info -> {
                respondQueue(event.channel, event.member, guildPlayer)
            }
            PlayerEmoji.TogglePlayState -> {
                event.channel.reply(event.member) {
                    embed {
                        if (guildPlayer.controls.isPlaying) {
                            guildPlayer.controls.pause()
                            title("プレイヤーを一時停止しました。")
                        } else {
                            guildPlayer.controls.resume()
                            title("プレイヤーを再開しました。")
                        }
                        color(HexColor.Good)
                    }
                }
            }
            PlayerEmoji.SkipBack -> {
                if (guildPlayer.controls.isPlaying) {
                    guildPlayer.controls.skipBack()
                    event.channel.reply(event.member) {
                        embed {
                            title("現在の曲を最初に巻き戻しました。")
                            color(HexColor.Good)
                        }
                    }
                } else {
                    event.channel.reply(event.member) {
                        embed {
                            title("現在再生中の曲はありません。")
                            color(HexColor.Bad)
                        }
                    }
                }
            }
            PlayerEmoji.SeekBack -> {
                if (guildPlayer.controls.isPlaying) {
                    guildPlayer.controls.seekBack(15)
                    event.channel.reply(event.member) {
                        embed {
                            title("15秒巻き戻しました。")
                            description { "現在の再生位置は ${guildPlayer.controls.position.toMilliSecondString()} です。" }
                            color(HexColor.Good)
                        }
                    }
                } else {
                    event.channel.reply(event.member) {
                        embed {
                            title("現在再生中の曲はありません。")
                            color(HexColor.Bad)
                        }
                    }
                }
            }
            PlayerEmoji.SeekForward -> {
                if (guildPlayer.controls.isPlaying) {
                    guildPlayer.controls.seekForward(15)
                    event.channel.reply(event.member) {
                        embed {
                            title("15秒早送りしました。")
                            description { "現在の再生位置は ${guildPlayer.controls.position.toMilliSecondString()} です。" }
                            color(HexColor.Good)
                        }
                    }
                } else {
                    event.channel.reply(event.member) {
                        embed {
                            title("現在再生中の曲はありません。")
                            color(HexColor.Bad)
                        }
                    }
                }
            }
            PlayerEmoji.SkipForward -> {
                if (guildPlayer.controls.isPlaying) {
                    if (event.member.isAdmin()) {
                        guildPlayer.controls.skipForward()
                        skipForwardVote = 0
                        event.channel.reply(event.member) {
                            embed {
                                title("管理者特権で現在の曲をスキップしました。")
                                color(HexColor.Good)
                            }
                        }
                    } else {
                        val requiredRate = 0.4
                        val voteCount = ++skipForwardVote
                        val vcMembersCount = guildPlayer.currentVoiceChannel.members.count { !it.user.isBotOrSelfUser }
                        val rate = if (vcMembersCount != 0) {
                            1.0 * voteCount / vcMembersCount
                        } else {
                            1.0
                        }

                        if (rate > requiredRate) {
                            guildPlayer.controls.skipForward()
                            skipForwardVote = 0
                            event.channel.reply(event.member) {
                                embed {
                                    title("スキップ投票が可決したので 現在の曲をスキップしました。")
                                    descriptionBuilder {
                                        appendln("${voteCount}票(${rate.times(100).round(1)}%)が集まりました。")
                                        append("(Botは除外しています)")
                                    }
                                    color(HexColor.Good)
                                }
                            }
                        } else {
                            event.channel.reply(event.member) {
                                embed {
                                    title("スキップに投票しました。")
                                    descriptionBuilder {
                                        val needCount = (vcMembersCount * requiredRate).roundToInt() - voteCount

                                        appendln("現在 ${voteCount}票で スキップにはあと${needCount}票必要です。")
                                        append("(Botは除外しています)")
                                    }
                                    color(HexColor.Neutral)
                                }
                            }
                        }
                    }
                } else {
                    event.channel.reply(event.member) {
                        embed {
                            title("現在再生中の曲はありません。")
                            color(HexColor.Bad)
                        }
                    }
                }
            }
            PlayerEmoji.Shuffle -> {
                if (!guildPlayer.controls.isEmptyQueue) {
                    guildPlayer.controls.shuffle()
                    event.channel.reply(event.member) {
                        embed {
                            title("曲順をシャッフルしました。")
                            description { "次の曲から反映されます。" }
                            color(HexColor.Good)
                        }
                    }
                } else {
                    event.channel.reply(event.member) {
                        embed {
                            title("現在キューは空です。")
                            color(HexColor.Bad)
                        }
                    }
                }
            }
            PlayerEmoji.RepeatTrack -> {
                event.channel.reply(event.member) {
                    embed {
                        if (!guildPlayer.controls.isRepeatTrackEnabled) {
                            guildPlayer.controls.enableRepeatTrack()
                            title("リピートを有効にしました。")
                            description { "スキップかリピート解除しない限り同じ曲が再生されます。" }
                            color(HexColor.Good)
                        } else {
                            guildPlayer.controls.disableRepeatTrack()
                            title("リピートを無効にしました。")
                            color(HexColor.Bad)
                        }
                    }
                }
            }
            PlayerEmoji.RepeatPlaylist -> {
                event.channel.reply(event.member) {
                    embed {
                        if (!guildPlayer.controls.isRepeatPlaylistEnabled) {
                            guildPlayer.controls.enableRepeatPlaylist()
                            title("プレイリストリピートを有効にしました。")
                            description { "次の曲から一度再生された曲はキューから削除されません。" }
                            color(HexColor.Good)
                        } else {
                            guildPlayer.controls.disableRepeatPlaylist()
                            title("プレイリストリピートを無効にしました。")
                            description { "無効にする前に再生していたトラックはキューに残っています。" }
                            color(HexColor.Bad)
                        }
                    }
                }
            }
            PlayerEmoji.Mute -> {
                event.channel.reply(event.member) {
                    embed {
                        if (guildPlayer.controls.isMuted) {
                            guildPlayer.controls.unmute()
                            title("ミュート解除しました。")
                            description { "現在のボリュームは ${guildPlayer.controls.volume}%です。" }
                            color(HexColor.Good)
                        } else {
                            guildPlayer.controls.mute()
                            title("ミュートしました。")
                            description { "引き続き再生は継続されています。" }
                            color(HexColor.Bad)
                        }
                    }
                }
            }
            PlayerEmoji.VolumeDown -> {
                guildPlayer.controls.volumeDown(2)
                event.channel.reply(event.member) {
                    embed {
                        title("ボリュームを下げました。(-2%)")
                        description { "ボリュームを ${guildPlayer.controls.volume}% に変更しました。" }
                        color(HexColor.Good)
                    }
                }
            }
            PlayerEmoji.VolumeUp -> {
                guildPlayer.controls.volumeUp(2)
                event.channel.reply(event.member) {
                    embed {
                        title("ボリュームを上げました。(+2%)")
                        description { "ボリュームを ${guildPlayer.controls.volume}% に変更しました。" }
                        color(HexColor.Good)
                    }
                }
            }
            PlayerEmoji.Clear -> {
                if (event.member.isAdmin()) {
                    guildPlayer.controls.clear()
                    clearVote = 0
                    event.channel.reply(event.member) {
                        embed {
                            title("管理者特権で再生キューをクリアしました。")
                            color(HexColor.Good)
                        }
                    }
                } else {
                    val requiredRate = 0.4
                    val voteCount = ++clearVote
                    val vcMembersCount = guildPlayer.currentVoiceChannel.members.count { !it.user.isBotOrSelfUser }
                    val rate = if (vcMembersCount != 0) {
                        1.0 * voteCount / vcMembersCount
                    } else {
                        1.0
                    }

                    if (rate > requiredRate) {
                        guildPlayer.controls.clear()
                        clearVote = 0
                        event.channel.reply(event.member) {
                            embed {
                                title("再生キューのクリア投票が可決したので 再生キューをクリアしました。")
                                descriptionBuilder {
                                    appendln("${voteCount}票(${rate.times(100).round(1)}%)が集まりました。")
                                    append("(Botは除外しています)")
                                }
                                color(HexColor.Good)
                            }
                        }
                    } else {
                        event.channel.reply(event.member) {
                            embed {
                                title("再生キューのクリアに投票しました。")
                                descriptionBuilder {
                                    val needCount = (vcMembersCount * requiredRate).roundToInt() - voteCount

                                    appendln("現在 ${voteCount}票で スキップにはあと${needCount}票必要です。")
                                    append("(Botは除外しています)")
                                }
                                color(HexColor.Neutral)
                            }
                        }
                    }
                }
            }
            PlayerEmoji.SoundCloud -> {
                return respondSoundCloud(event.channel, event.member, guildPlayer)
            }
            PlayerEmoji.NicoRanking -> {
                return respondNico(event.channel, event.member, guildPlayer)
            }
        }.launchAndDelete(30, TimeUnit.SECONDS)
    }

    private val playCommandSyntax = "^!(?:!)?play".toRegex()
    override suspend fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        val guildConfig = config.forGuild(event.guild) ?: return
        if (guildConfig.boolOption("enable_find_video_url").isFalseOrNull() || playCommandSyntax.containsMatchIn(event.message.contentDisplay)) {
            return
        }

        PlayableURL.values().forEach eachUrl@{ url ->
            url.regexes.forEach {
                val result = it.find(event.message.contentDisplay) ?: return@eachUrl

                event.channel.prompt(event.member) {
                    emoji<YesNoEmoji, YesNoEmoji>(
                            title = "動画URLを検出しました",
                            description = "プレイヤーのキューに追加しますか？",
                            timeoutSec = 30
                    ) { emoji, _, event ->
                        when (emoji) {
                            YesNoEmoji.Yes -> {
                                val guildPlayer = event.guild.player
                                guildPlayer?.loadTrack(result.value, TrackType.UserRequest, object: PlayerLoadResultHandler {
                                    override fun onLoadTrack(track: AudioTrack) {
                                        event.channel.reply(event.member) {
                                            embed {
                                                author("動画URLを検出しました")
                                                title("\"${track.info.effectiveTitle}\" (${track.info.length.toMilliSecondString()}) を再生キューに追加します")
                                                description {
                                                    if ((guildPlayer.controls.isEmptyQueue && !guildPlayer.controls.isPlaying) || guildPlayer.controls.currentTrack == track) {
                                                        "まもなく再生されます。"
                                                    } else {
                                                        "再生まであと${guildPlayer.controls.queue.size + 1}曲 (およそ${guildPlayer.controls.totalDuration.toMilliSecondString()})"
                                                    }
                                                }
                                                color(HexColor.Good)
                                                timestamp()
                                            }
                                        }.launchAndDelete(30, TimeUnit.SECONDS)

                                        guildPlayer.controls += track
                                    }

                                    override fun onLoadPlaylist(playlist: AudioPlaylist) {
                                        event.channel.reply(event.member) {
                                            embed {
                                                author("プレイリストURLが見つかりました")
                                                title("プレイリスト \"${playlist.name}\" (${playlist.tracks.size}曲, ${playlist.tracks.totalDurationMillis.toMilliSecondString()}) を再生キューに追加します")

                                                description {
                                                    if ((guildPlayer.controls.isEmptyQueue && !guildPlayer.controls.isPlaying) || guildPlayer.controls.currentTrack == playlist.tracks.firstOrNull()) {
                                                        "まもなく再生されます。"
                                                    } else {
                                                        "再生まであと${guildPlayer.controls.queue.size + 1}曲 (およそ${guildPlayer.controls.totalDuration.toMilliSecondString()})"
                                                    }
                                                }

                                                playlist.tracks.forEachIndexed { i, audioTrack ->
                                                    field("\n#${(i + 1).toString().padEnd(playlist.tracks.size.characterLength)}") { audioTrack.info.effectiveTitle }
                                                }
                                                color(HexColor.Good)
                                                timestamp()
                                            }
                                        }.launchAndDelete(30, TimeUnit.SECONDS)

                                        guildPlayer.controls.addAll(playlist.tracks)
                                    }

                                    override fun onEmptyResult() {
                                        event.channel.reply(event.member) {
                                            embed {
                                                author("エラー")
                                                title("`${result.value}` の結果は見つかりませんでした。")
                                                color(HexColor.Bad)
                                                timestamp()
                                            }
                                        }.launchAndDelete(60, TimeUnit.SECONDS)
                                    }

                                    override fun onFailed(exception: FriendlyException) {
                                        event.channel.reply(event.member) {
                                            embed {
                                                author("エラー")
                                                title("`${result.value}` の読み込みに失敗しました。")
                                                description { exception.localizedMessage }
                                                color(HexColor.Bad)
                                                timestamp()
                                            }
                                        }.launchAndDelete(30, TimeUnit.SECONDS)
                                    }
                                })
                            }
                            else -> {
                                logger.info { "\"${result.value}\" の再生を提案しましたが ${event.member.fullNameWithoutGuild}は拒否しました。" }
                            }
                        }
                    }
                }

                return logger.info { "動画形式(${url.friendlyName})の文字列: ${result.value} を検出しました. [#${event.channel.name}] ${event.member.fullName}" }
            }
        }
    }
}
