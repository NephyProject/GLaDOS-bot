package jp.nephy.glados.features

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import com.mongodb.client.result.UpdateResult
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import io.ktor.util.extension
import jp.nephy.glados.config
import jp.nephy.glados.core.audio.music.*
import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.deleteQueue
import jp.nephy.glados.core.builder.message
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Listener
import jp.nephy.glados.core.feature.textChannelsLazy
import jp.nephy.glados.core.isAdmin
import jp.nephy.glados.core.isBotOrSelfUser
import jp.nephy.glados.secret
import jp.nephy.jsonkt.database
import jp.nephy.jsonkt.mongodb
import jp.nephy.utils.randomChoice
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent
import org.apache.lucene.search.spell.LevensteinDistance
import org.litote.kmongo.coroutine.getCollectionOfName
import org.litote.kmongo.coroutine.insertOne
import org.litote.kmongo.coroutine.singleResult
import org.litote.kmongo.coroutine.toList
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
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

    private val db = mongodb(secret.forKey("mongodb_host")).database("bot").getCollectionOfName<PlayHistory>("GLaDOSSoundBotPlayHistory")
    private val botChannels by textChannelsLazy("bot")

    @Listener
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        event.message.handleMessage()
    }

    @Listener
    override fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) {
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

        val command = contentDisplay.split("\\s+".toRegex()).first().removePrefix(prefix)
        when (command) {
            "add" -> {
                add()
            }
            "remove", "delete" -> {
                remove()
            }
            "ranking", "mostplayed" -> {
                ranking()
            }
            "recent", "new", "lastadded" -> {
                recent()
            }
            "list", "sounds" -> {
                list()
            }
            "mute", ".ignore" -> {
                mute()
            }
            "stop", "skip" -> {
                stop(player)
            }
            "download", "dl" -> {
                download()
            }
            "search", "find" -> {
                search()
            }
            "help" -> {
                help()
            }
            else -> {
                contentDisplay.lines().filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) }.forEach { it ->
                    play(it, player)
                }
            }
        }
    }

    private fun Message.add() {
        val attachment = attachments.firstOrNull() ?: return reply {
            embed {
                title("コマンドエラー: `.add`")
                description { "添付ファイルがありません。使用可能な拡張子は ${extensions.joinToString(", ") { "`$it`" }} です。" }
                timestamp()
                color(Color.Bad)
            }
        }.queue()

        val (filename, ext) = attachment.fileName.split(".", limit = 2)
        if (ext !in extensions) {
            return reply {
                embed {
                    title("コマンドエラー: `.add`")
                    description { "`.$ext` に対応していません。使用可能な拡張子は ${extensions.joinToString(", ") { "`$it`" }} です。" }
                    timestamp()
                    color(Color.Bad)
                }
            }.queue()
        }

        if (extensions.any { Files.exists(Paths.get(soundsDirectory, guild.id, "$filename.$it")) }) {
            return reply {
                embed {
                    title("コマンドエラー: `.add`")
                    description { "すでに `$filename` は登録済みであるため, 追加できませんでした。" }
                    timestamp()
                    color(Color.Bad)
                }
            }.queue()
        }

        val newSound = Paths.get(soundsDirectory, guild.id, attachment.fileName)
        return if (attachment.download(newSound.toFile())) {
            reply {
                embed {
                    title("サウンド追加")
                    description { "${guild.name} に新しいサウンド `${attachment.fileName.split(".").first()}` を追加しました。" }
                    timestamp()
                    color(Color.Good)
                }
            }.queue()
        } else {
            reply {
                embed {
                    title("コマンドエラー: `.add`")
                    description { "`${attachment.url}` のダウンロードに失敗しました。あとで再試行してください。" }
                    timestamp()
                    color(Color.Bad)
                }
            }.queue()
        }
    }

    private fun Message.remove() {
        if (!member.isAdmin()) {
            return reply {
                embed {
                    title("コマンドエラー: `.remove`")
                    description { "このコマンドは管理者のみ実行できます。" }
                    timestamp()
                    color(Color.Bad)
                }
            }.queue()
        }

        val filename = contentDisplay.split("\\s+".toRegex()).getOrNull(1)?.removePrefix(prefix) ?: return reply {
            embed {
                title("コマンドエラー: `.remove`")
                description { "コマンドの引数が不正です。" }
                timestamp()
                color(Color.Bad)
            }
        }.queue()

        val path = extensions.map { Paths.get(soundsDirectory, guild.id, "$filename.$it") }.find { Files.exists(it) } ?: return reply {
            embed {
                title("コマンドエラー: `.remove`")
                description { "`$filename` は登録されていません。" }
                timestamp()
                color(Color.Bad)
            }
        }.queue()

        Files.delete(path)

        return reply {
            embed {
                title("サウンド削除")
                description { "${guild.name} からサウンド `$filename` を削除しました。" }
                timestamp()
                color(Color.Good)
            }
        }.queue()
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
        }.queue()
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
        }.queue()
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
                }.queue()
            }
        }
    }

    private val cooldowns = ConcurrentHashMap<Member, Long>()
    private fun Message.mute() {
        if (!member.isAdmin()) {
            return reply {
                embed {
                    title("コマンドエラー: `.mute`")
                    description { "このコマンドは管理者のみ実行できます。" }
                    timestamp()
                    color(Color.Bad)
                }
            }.queue()
        }

        val target = mentionedMembers.firstOrNull() ?: return reply {
            embed {
                title("コマンドエラー: `.mute`")
                description { "対象が指定されていません。" }
                timestamp()
                color(Color.Bad)
            }
        }.queue()

        if (target.isAdmin()) {
            return reply {
                embed {
                    title("コマンドエラー: `.mute`")
                    description { "対象は管理者のため, クールダウンを設定できません。" }
                    timestamp()
                    color(Color.Bad)
                }
            }.queue()
        }

        cooldowns[member] = (cooldowns[member] ?: Date().time) + 10 * 60 * 1000

        reply {
            embed {
                title("クールダウン追加")
                description { "${target.asMention} に10分間のクールダウンを追加しました。" }
                timestamp()
                color(Color.Bad)
            }
        }.queue()
    }

    private fun Message.stop(player: GuildPlayer) {
        val track = player.controls.currentTrack
        when (track?.type) {
            null -> {
                reply {
                    embed {
                        title("コマンドエラー: `.stop`")
                        description { "現在何も再生されていないため, 停止できません。" }
                        timestamp()
                        color(Color.Bad)
                    }
                }.queue()
            }
            TrackType.Sound -> {
                player.controls.skipForward()

                reply {
                    embed {
                        title("サウンド停止")
                        description { "`${track.identifier.split("/").last().split(".").first()}` を停止しました。" }
                        timestamp()
                        color(Color.Good)
                    }
                }.queue()
            }
            else -> {
                reply {
                    embed {
                        title("コマンドエラー: `.stop`")
                        description { "現在サウンドが再生されていないため, 停止できません。" }
                        timestamp()
                        color(Color.Bad)
                    }
                }.queue()
            }
        }
    }

    private fun Message.download() {
        val filename = contentDisplay.split("\\s+".toRegex()).getOrNull(1)?.removePrefix(prefix) ?: return reply {
            embed {
                title("コマンドエラー: `.download`")
                description { "コマンドの引数が不正です。" }
                timestamp()
                color(Color.Bad)
            }
        }.queue()

        val path = extensions.map { Paths.get(soundsDirectory, guild.id, "$filename.$it") }.find { Files.exists(it) } ?: return reply {
            embed {
                title("コマンドエラー: `.download`")
                description { "`$filename` は登録されていません。" }
                timestamp()
                color(Color.Bad)
            }
        }.queue()

        channel.sendFile(path.toFile()).queue()
    }

    private fun Message.search() {
        val query = contentDisplay.split("\\s+".toRegex()).getOrNull(1) ?: return reply {
            embed {
                title("コマンドエラー: `.search`")
                description { "コマンドの引数が不正です。" }
                timestamp()
                color(Color.Bad)
            }
        }.queue()
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
        }.queue()
    }

    private fun Message.help() {
        reply {
            embed {
                title("SoundBot")
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
                field("`.list`") {
                    "${guild.name} で使えるサウンド一覧をDMで送信します。"
                }
                field("`.mute`") {
                    "指定ユーザが10分間サウンドを再生できないようにします。(要Adminロール)"
                }
                field("`.stop`") {
                    "現在再生中のサウンドを停止します。"
                }
                field("`.download`") {
                    "指定されたサウンドをダウンロードできるようにします。"
                }
                field("`.search`") {
                    "サウンドを正規表現で検索します。"
                }
                timestamp()
                color(Color.Good)
            }
        }.queue()
    }

    private fun Message.play(command: String, player: GuildPlayer) {
        val cooldown = cooldowns[member]
        if (cooldown != null && Date().time < cooldown) {
            return reply {
                embed {
                    title("コマンドエラー")
                    description { "現在クールダウンが有効なため, サウンドを再生できません。" }
                    timestamp()
                    color(Color.Bad)
                }
            }.queue()
        } else {
            cooldowns.remove(member)
        }

        val path = if (command == "random") {
            listSounds(guild.idLong).randomChoice()
        } else {
            extensions.map { Paths.get(soundsDirectory, guild.id, "$command.$it") }.find { Files.exists(it) } ?: return message {
                message {
                    appendln("`$command` は登録されていません。")
                    val candidates = listSounds(guild.idLong).map {
                        val filename = it.fileName.toString().split(".").first()
                        filename to LevensteinDistance().getDistance(command, filename)
                    }.sortedByDescending { it.second }.take(5)

                    append("もしかして: ${candidates.joinToString(", ") { "`${it.first}`" }}")
                }
            }.queue()
        }

        // GLaDOSが接続しているVCに接続していなければ無視
        if (player.currentVoiceChannel != member.voiceState.channel) {
            return reply {
                embed {
                    title("コマンドエラー")
                    description { "サウンドの再生はGLaDOSと同じVCに接続中にのみ行なえます。" }
                    timestamp()
                    color(Color.Bad)
                }
            }.deleteQueue(30, TimeUnit.SECONDS)
        }

        val filename = path.fileName.toString().split(".").first()

        player.loadTrack(path.toAbsolutePath().toString(), TrackType.Sound, object: PlayerLoadResultHandler {
            override fun onLoadTrack(track: AudioTrack) {
                player.controls += track
                player.controls.resume()

                logger.info { "サウンド: $filename をロードしました." }

                launch {
                    val count = singleResult<Long> { db.countDocuments(Filters.and(Filters.eq("guild", guild.idLong), Filters.eq("command", filename)), it) } ?: 0L
                    if (count > 0) {
                        singleResult<UpdateResult> {
                            db.updateOne(
                                    Filters.and(Filters.eq("guild", guild.idLong), Filters.eq("command", filename)),
                                    Updates.inc("count", 1),
                                    it
                            )
                        }
                    } else {
                        db.insertOne(PlayHistory(guild.idLong, filename, 1))
                    }
                }

                if (command == "random") {
                    launch {
                        delay(track.duration, TimeUnit.MILLISECONDS)

                        message {
                            message {
                                append("=> `$filename`")
                            }
                        }.queue()
                    }
                }
            }
        })
    }

    data class PlayHistory(val guild: Long, val command: String, val count: Int)
}
