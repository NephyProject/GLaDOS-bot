package jp.nephy.glados.features.player

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import io.ktor.util.extension
import jp.nephy.glados.config
import jp.nephy.glados.core.audio.player.*
import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.message
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.*
import jp.nephy.glados.core.feature.subscription.Event
import jp.nephy.glados.core.feature.subscription.spaceRegex
import jp.nephy.glados.core.isAdmin
import jp.nephy.glados.core.isBotOrSelfUser
import jp.nephy.glados.core.launch
import jp.nephy.glados.dispatcher
import jp.nephy.glados.mongodb
import jp.nephy.utils.randomChoice
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent
import org.apache.lucene.search.spell.LevensteinDistance
import org.litote.kmongo.getCollection
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.streams.toList

class SoundBot: BotFeature() {
    companion object {
        private const val prefix = "."
        private const val soundsDirectory = "sounds"
        private val extensions = arrayOf("mp3", "wav", "ogg")

        fun listSounds(guild: Long? = null): List<Path> {
            return if (guild != null) {
                Files.list(Paths.get(soundsDirectory, "$guild")).filter { Files.isRegularFile(it) && it.extension in extensions }.toList()
            } else {
                Files.list(Paths.get(soundsDirectory)).filter { Files.isDirectory(it) }.flatMap { Files.list(it) }.toList()
            }.sorted()
        }
    }

    private val db = mongodb.getCollection<PlayHistory>("GLaDOSSoundBotPlayHistory")
    private val botChannels by textChannelsLazy("bot")

    @Event
    override suspend fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        event.message.handleMessage()
    }

    @Event
    override suspend fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) {
        event.message.handleMessage()
    }

    private fun Message.handleMessage() {
        val config = config.forGuild(guild) ?: return
        val player = guild.player ?: return

        if (author.isBotOrSelfUser) {
            return
        }

        // enable_soundbotが有効でなければ無視
        if (!config.boolOption("enable_soundbot", false)) {
            return
        }

        // ignore_soundbot_from_non_bot_channelフラグが有効ならBotチャンネル以外のメッセージを無視
        if (config.boolOption("ignore_soundbot_from_non_bot_channel", false) && channel !in botChannels) {
            return
        }

        val command = contentDisplay.split(spaceRegex).first()
        when (command) {
            ".add" -> {
                add()
            }
            ".remove", ".delete" -> {
                remove()
            }
            ".ranking", ".mostplayed" -> {
                ranking()
            }
            ".recent", ".new", ".lastadded" -> {
                recent()
            }
            ".list", ".sounds" -> {
                list()
            }
            ".mute", ".ignore" -> {
                ignore()
            }
            ".stop", ".skip" -> {
                stop(player)
            }
            ".kill" -> {
                kill(player)
            }
            ".download", ".dl" -> {
                download()
            }
            ".search", ".find" -> {
                search()
            }
            ".help" -> {
                help()
            }
            else -> {
                contentDisplay.lines().filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) }.filter { it.isNotBlank() }.forEach { it ->
                    play(it.toLowerCase(), player)
                }
            }
        }
    }

    private fun Message.add() {
        val attachment = attachments.firstOrNull()
        rejectNull(attachment) {
            embedError(".add") {
                "添付ファイルがありません。使用可能な拡張子は ${extensions.joinToString(", ") { "`$it`" }} です。"
            }
        }

        val (filename, ext) = attachment.fileName.split(".", limit = 2)
        reject(ext !in extensions) {
            embedError(".add") {
                "`.$ext` に対応していません。使用可能な拡張子は ${extensions.joinToString(", ") { "`$it`" }} です。"
            }
        }

        reject(extensions.any { Files.exists(Paths.get(soundsDirectory, guild.id, "$filename.$it")) }) {
            embedError(".add") {
                "すでに `$filename` は登録済みであるため, 追加できませんでした。"
            }
        }

        val newSound = Paths.get(soundsDirectory, guild.id, attachment.fileName)
        reject(!attachment.download(newSound.toFile())) {
            embedError(".add") {
                "`${attachment.url}` のダウンロードに失敗しました。あとで再試行してください。"
            }
        }

        reply {
            embed {
                title("サウンド追加")
                description { "${guild.name} に新しいサウンド `${attachment.fileName.split(".").first()}` を追加しました。" }
                timestamp()
                color(Color.Good)
            }
        }.launch()
    }

    private fun Message.remove() {
        reject(!member.isAdmin()) {
            embedError(".remove") {
                "このコマンドは管理者のみ実行できます。"
            }
        }

        val filename = contentDisplay.split(spaceRegex).getOrNull(1)?.removePrefix(prefix)
        rejectNull(filename) {
            embedError(".remove") {
                "コマンドの引数が不正です。"
            }
        }

        val path = extensions.map { Paths.get(soundsDirectory, guild.id, "$filename.$it") }.find { Files.exists(it) }
        rejectNull(path) {
            embedError(".remove") {
                "`$filename` は登録されていません。"
            }
        }

        Files.delete(path)

        reply {
            embed {
                title("サウンド削除")
                description { "${guild.name} からサウンド `$filename` を削除しました。" }
                timestamp()
                color(Color.Good)
            }
        }.launch()
    }

    private fun Message.ranking() {
        reply {
            embed {
                title("${guild.name} で再生されたサウンド Top10")

                runBlocking { db.find().filter(Filters.eq("guild", guild.idLong)).sort(Sorts.descending("count")).limit(10).toList() }.forEachIndexed { i, sound ->
                    field("#${i + 1}") {
                        "`${sound.command}`: ${sound.count}"
                    }
                }

                timestamp()
                color(Color.Good)
            }
        }.launch()
    }

    private fun Message.recent() {
        reply {
            embed {
                title("${guild.name} に最近追加されたサウンド (直近10件)")

                listSounds(guild.idLong).map { it to Files.getLastModifiedTime(it) }.sortedByDescending { it.second }.take(10).forEach { (path, time) ->
                    field(path.fileName.toString().split(".").first()) {
                        "${DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(time.toInstant().atZone(ZoneId.of("Asia/Tokyo")))} 追加"
                    }
                }

                timestamp()
                color(Color.Good)
            }
        }.launch()
    }

    private fun Message.list() {
        author.openPrivateChannel().queue {
            listSounds(guild.idLong).chunked(200).forEachIndexed { i, list ->
                it.message {
                    embed {
                        title("${guild.name} に登録されているサウンド一覧 #${i + 1}")
                        descriptionBuilder {
                            appendln("```")
                            list.forEach { path ->
                                appendln(path.fileName.toString().split(".").first())
                            }
                            appendln("```")
                        }
                        timestamp()
                        color(Color.Good)
                    }
                }.launch()
            }
        }
    }

    private val cooldowns = ConcurrentHashMap<Member, Long>()
    private fun Message.ignore() {
        reject(!member.isAdmin()) {
            embedError(".ignore") {
                "このコマンドは管理者のみ実行できます。"
            }
        }

        val target = mentionedMembers.firstOrNull()
        rejectNull(target) {
            embedError(".ignore") {
                "対象が指定されていません。メンションで指定します。"
            }
        }

        reject(target.isAdmin()) {
            embedError(".ignore") {
                "対象は管理者のため, クールダウンを設定できません。"
            }
        }

        cooldowns[member] = (cooldowns[member] ?: Date().time) + 10 * 60 * 1000

        reply {
            embed {
                title("クールダウン追加")
                description { "${target.asMention} に10分間のクールダウンを追加しました。" }
                timestamp()
                color(Color.Bad)
            }
        }.launch()
    }

    private fun Message.stop(player: GuildPlayer) {
        val track = player.controls.currentTrack
        rejectNull(track) {
            embedError(".stop") {
                "現在何も再生されていないため, 停止できません。"
            }
        }

        reject(track.data.type != TrackType.Sound) {
            embedError(".stop") {
                "現在サウンドが再生されていないため, 停止できません。"
            }
        }

        player.controls.skipForward()

        reply {
            embed {
                title("サウンド停止")
                description { "`${track.identifier.split("/").last().split(".").first()}` を停止しました。" }
                timestamp()
                color(Color.Good)
            }
        }.launch()
    }

    private fun Message.kill(player: GuildPlayer) {
        val track = player.controls.currentTrack
        rejectNull(track) {
            embedError(".kill") {
                "現在何も再生されていないため, 停止できません。"
            }
        }

        player.controls.clear()
        player.controls.skipForward()

        reply {
            embed {
                title("サウンド一括クリア")
                description { "キューのサウンドをクリアしました。" }
                timestamp()
                color(Color.Good)
            }
        }.launch()
    }

    private fun Message.download() {
        val filename = contentDisplay.split(spaceRegex).getOrNull(1)?.removePrefix(prefix)
        rejectNull(filename) {
            embedError(".download") {
                "コマンドの引数が不正です。"
            }
        }

        val path = extensions.map { Paths.get(soundsDirectory, guild.id, "$filename.$it") }.find { Files.exists(it) }
        rejectNull(path) {
            embedError(".download") {
                "`$filename` は登録されていません。"
            }
        }

        channel.sendFile(path.toFile()).launch()
    }

    private fun Message.search() {
        val query = contentDisplay.split(spaceRegex).getOrNull(1)
        rejectNull(query) {
            embedError(".search") {
                "コマンドの引数が不正です。"
            }
        }

        val regex = query.toRegex()

        val result = listSounds(guild.idLong).map { it.fileName.toString().split(".").first() }.filter { regex.containsMatchIn(it) }.take(10)

        reply {
            embed {
                title("`$query`のサウンド検索結果")

                if (result.isNotEmpty()) {
                    result.forEachIndexed { i, it ->
                        field("#${i + 1}") { "`$it`" }
                    }
                } else {
                    val candidates = listSounds(guild.idLong).map {
                        val filename = it.fileName.toString().split(".").first()
                        filename to LevensteinDistance().getDistance(query, filename)
                    }.sortedByDescending { it.second }.take(5)

                    descriptionBuilder {
                        appendln("`$query` の検索結果はありませんでした。")
                        append("もしかして: ${candidates.joinToString(", ") { "`${it.first}`" }}")
                    }
                }

                timestamp()
                color(Color.Good)
            }
        }.launch()
    }

    private fun Message.help() {
        reply {
            embed {
                title("SoundBot")
                field("`.<サウンド名>`") {
                    "指定されたサウンドを再生します。"
                }
                field("`..<サウンド名>`") {
                    "あいまい検索の結果、最も関連度の高いサウンドを再生します。"
                }
                field("`.add`") {
                    "添付された${extensions.joinToString(", ") { "`$it`" }}ファイルを登録します。"
                }
                field("`.remove`") {
                    "指定されたサウンドを削除します。(要Adminロール)"
                }
                field("`.ranking`") {
                    "${guild.name} で再生されたTop10を表示します。"
                }
                field("`.recent`") {
                    "${guild.name} に最近追加されたサウンドを直近10件表示します。"
                }
                field("`.search`") {
                    "サウンドを正規表現で検索します。"
                }
                field("`.list`") {
                    "${guild.name} で使えるサウンド一覧をDMで送信します。"
                }
                field("`.download`") {
                    "指定されたサウンドをダウンロードできるようにします。"
                }
                field("`.stop`") {
                    "現在再生中のサウンドを停止します。"
                }
                field("`.kill`") {
                    "サウンドキューをクリアします。"
                }
                field("`.ignore`") {
                    "指定ユーザが10分間サウンドを再生できないようにします。(要Adminロール)"
                }
                timestamp()
                color(Color.Good)
            }
        }.launch()
    }

    private fun Message.play(command: String, player: GuildPlayer, postResult: Boolean = false) {
        val cooldown = cooldowns[member]
        reject(cooldown != null && Date().time < cooldown) {
            embedError(".play") {
                "現在クールダウンが有効なため, サウンドを再生できません。"
            }
        }

        cooldowns.remove(member)

        // GLaDOSが接続しているVCに接続していなければ無視
        reject(player.currentVoiceChannel != member.voiceState.channel) {
            embedError(".play") {
                "サウンドの再生はGLaDOSと同じVCに接続中にのみ行なえます。"
            }
        }

        val commandReplaced = command.replace(".", "")
        val path = when {
            commandReplaced.isBlank() -> return
            command == "random" -> listSounds(guild.idLong).randomChoice()
            command.startsWith(".") -> {
                val candidates = listSounds(guild.idLong).map {
                    val filename = it.fileName.toString().split(".").first()
                    filename to LevensteinDistance().getDistance(commandReplaced, filename)
                }
                val max = candidates.maxBy { it.second }!!.second
                val filename = candidates.filter { it.second >= max }.randomChoice().first

                return play(filename, player, true)
            }
            else -> extensions.map { Paths.get(soundsDirectory, guild.id, "$commandReplaced.$it") }.find { Files.exists(it) }
        }

        rejectNull(path) {
            simpleError(".play") {
                appendln("`$commandReplaced` は登録されていません。")
                val candidates = listSounds(guild.idLong).map {
                    val filename = it.fileName.toString().split(".").first()
                    filename to LevensteinDistance().getDistance(commandReplaced, filename)
                }.sortedByDescending { it.second }.take(5)

                append("もしかして: ${candidates.joinToString(", ") { "`${it.first}`" }}")
            }
        }

        val filename = path.fileName.toString().split(".").first()

        player.loadTrack(path.toAbsolutePath().toString(), TrackType.Sound, object: PlayerLoadResultHandler {
            override fun onLoadTrack(track: AudioTrack) {
                if (command == "random" || postResult) {
                    track.data.onTrackEnd = {
                        message {
                            message {
                                append("=> `$filename`")
                            }
                        }.launch()
                    }
                }

                player.controls += track
                player.controls.resume()

                logger.info { "サウンド: $filename をロードしました。" }

                GlobalScope.launch(dispatcher) {
                    if (db.countDocuments(Filters.and(Filters.eq("guild", guild.idLong), Filters.eq("command", filename))) > 0L) {
                        db.updateOne(
                                Filters.and(Filters.eq("guild", guild.idLong), Filters.eq("command", filename)),
                                Updates.inc("count", 1)
                        )
                    } else {
                        db.insertOne(PlayHistory(guild.idLong, filename, 1))
                    }
                }
            }
        })
    }

    data class PlayHistory(val guild: Long, val command: String, val count: Int)
}
