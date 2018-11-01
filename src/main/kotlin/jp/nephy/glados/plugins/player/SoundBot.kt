package jp.nephy.glados.plugins.player

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import io.ktor.util.extension
import jp.nephy.glados.config
import jp.nephy.glados.core.audio.player.*
import jp.nephy.glados.core.extensions.*
import jp.nephy.glados.core.extensions.messages.HexColor
import jp.nephy.glados.core.plugins.Plugin
import jp.nephy.glados.dispatcher
import jp.nephy.glados.mongodb
import jp.nephy.utils.randomChoice
import jp.nephy.utils.toString
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

object SoundBot: Plugin() {
    private const val prefix = "."
    private const val soundsDirectory = "sounds"
    private val extensions = arrayOf("mp3", "wav", "ogg")

    fun listSounds(guild: Long? = null): List<Path> {
        return if (guild != null) {
            val path = Paths.get(soundsDirectory, "$guild")
            if (!Files.isDirectory(path)) {
                Files.createDirectories(path)
            }

            Files.list(path).filter { Files.isRegularFile(it) && it.extension in extensions }
        } else {
            Files.list(Paths.get(soundsDirectory)).filter { Files.isDirectory(it) }.flatMap { Files.list(it) }
        }.sorted().toList()
    }

    private val botChannels by textChannelsLazy("bot")

    override suspend fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        event.message.handleMessage()
    }

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
            ".fav", ".favorite" -> {
                favorite()
            }
            ".addfav" -> {
                addfav()
            }
            ".removefav" -> {
                removefav()
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
            ".ignore", ".mute" -> {
                ignore()
            }
            ".stop", ".skip" -> {
                stop(player)
            }
            ".kill", ".clear" -> {
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
                color(HexColor.Good)
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
                color(HexColor.Good)
            }
        }.launch()
    }

    private val favorites = mongodb.getCollection<SoundFavorite>("GLaDOSSoundBotFavorite")
    private fun Message.favorite() {
        val favoritesEntry = favorites.find(Filters.and(Filters.eq("guild", guild.idLong), Filters.eq("user", author.idLong))).toList()

        reject(favoritesEntry.isEmpty()) {
            embedError(".favorite") {
                "お気に入り登録されたサウンドはありません。"
            }
        }

        reply {
            embed {
                title("お気に入り登録したサウンド (${guild.name})")

                for (entry in favoritesEntry) {
                    field("`${entry.filename}`") {
                        "追加日: ${Date(entry.date).toString("yyyy/MM/dd HH:mm:ss")}"
                    }
                }

                timestamp()
                color(HexColor.Good)
            }
        }.launch()
    }

    private fun Message.addfav() {
        val filename = contentDisplay.split(spaceRegex).getOrNull(1)?.removePrefix(prefix)
        rejectNull(filename) {
            embedError(".addfav") {
                "コマンドの引数が不正です。"
            }
        }

        val path = extensions.map { Paths.get(soundsDirectory, guild.id, "$filename.$it") }.find { Files.exists(it) }
        rejectNull(path) {
            embedError(".addfav") {
                "`$filename` は存在しません。"
            }
        }

        reject(favorites.contains(Filters.and(Filters.eq("guild", guild.idLong), Filters.eq("user", author.idLong), Filters.eq("filename", filename)))) {
            embedError(".addfav") {
                "`$filename` はすでにお気に入り登録されています。"
            }
        }

        favorites.insertOne(SoundFavorite(guild.idLong, author.idLong, filename, Date().time))

        reply {
            embed {
                title("サウンドお気に入り追加")
                description { "サウンド `$filename` をお気に入り登録しました。(${guild.name})" }
                timestamp()
                color(HexColor.Good)
            }
        }.launch()
    }

    private fun Message.removefav() {
        val filename = contentDisplay.split(spaceRegex).getOrNull(1)?.removePrefix(prefix)
        rejectNull(filename) {
            embedError(".removefav") {
                "コマンドの引数が不正です。"
            }
        }

        reject(!favorites.contains(Filters.and(Filters.eq("guild", guild.idLong), Filters.eq("user", author.idLong), Filters.eq("filename", filename)))) {
            embedError(".removefav") {
                "`$filename` はお気に入り登録されていません。"
            }
        }

        favorites.deleteOne(Filters.and(Filters.eq("guild", guild.idLong), Filters.eq("user", author.idLong), Filters.eq("filename", filename)))

        reply {
            embed {
                title("サウンドお気に入り削除")
                description { "サウンド `$filename` をお気に入り削除しました。(${guild.name})" }
                timestamp()
                color(HexColor.Good)
            }
        }.launch()
    }

    private val histories = mongodb.getCollection<PlayHistory>("GLaDOSSoundBotPlayHistory")
    private fun Message.ranking() {
        reply {
            embed {
                title("${guild.name} で再生されたサウンド Top10")

                runBlocking { histories.find().filter(Filters.eq("guild", guild.idLong)).sort(Sorts.descending("count")).limit(10).toList() }.forEachIndexed { i, sound ->
                    field("#${i + 1}") {
                        "`${sound.command}`: ${sound.count}"
                    }
                }

                timestamp()
                color(HexColor.Good)
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
                color(HexColor.Good)
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
                        color(HexColor.Good)
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
                color(HexColor.Bad)
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
                color(HexColor.Good)
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
                color(HexColor.Good)
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
                color(HexColor.Good)
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
                field("`.random`, `.r`") {
                    "ランダムにサウンドを再生します。"
                }
                field("`.add <添付ファイル>`") {
                    "添付された${extensions.joinToString(", ") { "`$it`" }}ファイルを登録します。"
                }
                field("`.remove <サウンド名>`, `.delete <サウンド名>`") {
                    "指定されたサウンドを削除します。(要Adminロール)"
                }
                field("`.favorite`, `.fav`") {
                    "お気に入り登録したサウンドを表示します。"
                }
                field("`.addfav <サウンド名>`") {
                    "指定されたサウンドをお気に入り登録します。"
                }
                field("`.removefav <サウンド名>`") {
                    "指定されたサウンドをお気に入り解除します。"
                }
                field("`.ranking`, `.mostplayed`") {
                    "${guild.name} で再生されたTop10を表示します。"
                }
                field("`.recent`, `.new`, `.lastadded`") {
                    "${guild.name} に最近追加されたサウンドを直近10件表示します。"
                }
                field("`.search <クエリ>`, `.find <クエリ>`") {
                    "サウンドを検索します。正規表現も使用できます。"
                }
                field("`.list`, `.sounds`") {
                    "${guild.name} で使えるサウンド一覧をDMで送信します。"
                }
                field("`.download <サウンド名>`, `.dl <サウンド名>`") {
                    "指定されたサウンドをダウンロードできるようにします。"
                }
                field("`.stop`, `.skip`") {
                    "現在再生中のサウンドを停止します。"
                }
                field("`.kill`, `.clear`") {
                    "サウンドキューをクリアします。"
                }
                field("`.ignore <@メンション>`, `.mute <@メンション>`") {
                    "指定ユーザが10分間サウンドを再生できないようにします。(要Adminロール)"
                }
                timestamp()
                color(HexColor.Good)
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
            command == "random" || command == "r" -> listSounds(guild.idLong).randomChoice()
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
                if ((commandReplaced == "random" || commandReplaced == "r") || postResult) {
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
                    if (histories.countDocuments(Filters.and(Filters.eq("guild", guild.idLong), Filters.eq("command", filename))) > 0L) {
                        histories.updateOne(
                                Filters.and(Filters.eq("guild", guild.idLong), Filters.eq("command", filename)),
                                Updates.inc("count", 1)
                        )
                    } else {
                        histories.insertOne(PlayHistory(guild.idLong, filename, 1))
                    }
                }
            }
        })
    }

    data class SoundFavorite(val guild: Long, val user: Long, val filename: String, val date: Long)

    data class PlayHistory(val guild: Long, val command: String, val count: Int)
}
