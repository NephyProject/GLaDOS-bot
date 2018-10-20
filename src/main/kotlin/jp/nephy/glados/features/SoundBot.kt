package jp.nephy.glados.features

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import com.mongodb.client.result.UpdateResult
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import io.ktor.util.extension
import jp.nephy.glados.config
import jp.nephy.glados.core.audio.music.GuildPlayer
import jp.nephy.glados.core.audio.music.PlayerLoadResultHandler
import jp.nephy.glados.core.audio.music.TrackType
import jp.nephy.glados.core.audio.music.player
import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.message
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Listener
import jp.nephy.glados.core.feature.textChannelsLazy
import jp.nephy.glados.secret
import jp.nephy.jsonkt.database
import jp.nephy.jsonkt.mongodb
import jp.nephy.utils.randomChoice
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import org.litote.kmongo.coroutine.getCollectionOfName
import org.litote.kmongo.coroutine.insertOne
import org.litote.kmongo.coroutine.singleResult
import org.litote.kmongo.coroutine.toList
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
        val config = config.forGuild(event.guild) ?: return
        val player = event.guild.player ?: return

        // enable_soundbotが有効でなければ無視
        if (!config.boolOption("enable_soundbot", false)) {
            return
        }

        // ignore_soundbot_from_non_bot_channelフラグが有効ならBotチャンネル以外のメッセージを無視
        if (config.boolOption("ignore_soundbot_from_non_bot_channel", false) && event.channel !in botChannels) {
            return
        }

        // GLaDOSが接続しているVCに接続していなければ無視
        if (player.currentVoiceChannel != event.member.voiceState.channel) {
            return
        }

        when (event.message.contentDisplay) {
            ".add" -> {
                event.message.add()
            }
            ".ranking", ".mostplayed" -> {
                event.message.ranking()
            }
            ".recent", ".new" -> {
                event.message.recent()
            }
            ".list", ".sounds" -> {
                event.message.list()
            }
            else -> {
                event.message.contentDisplay.lines().filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) }.forEach { command ->
                    player.play(command)
                }
            }
        }
    }

    private fun Message.add() {
        val attachment = attachments.firstOrNull() ?: return
        if (attachment.fileName.split(".").last() !in extensions) {
            return
        }

        val newSound = Paths.get(soundsDirectory, guild.id, attachment.fileName)
        if (Files.exists(newSound)) {
            return
        }
        attachment.download(newSound.toFile())

        reply {
            embed {
                title("新しいサウンド: ${attachment.fileName.split(".").first()} を追加しました")
                timestamp()
                color(Color.Good)
            }
        }.queue()
    }

    private fun Message.ranking() {
        reply {
            embed {
                title("このサーバで再生されたサウンド Top10")

                runBlocking { db.find().filter(Filters.eq("guild", guild.idLong)).sort(Sorts.descending("count")).limit(10).toList() }.forEachIndexed { i, sound ->
                    field("#${i + 1}") {
                        "${sound.command}: ${sound.count}"
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
                title("このサーバに最近追加されたサウンド (直近10件)")

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
            listSounds(guild.idLong).chunked(100).forEachIndexed { i, list ->
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

    private fun GuildPlayer.play(command: String) {
        val path = if (command == "random") {
            listSounds(guild.idLong).randomChoice()
        } else {
            extensions.map { Paths.get(soundsDirectory, guild.id, "$command.$it") }.find { Files.exists(it) }
                    ?: return logger.warn { "不明なサウンドファイル: $command" }
        }

        loadTrack(path.toAbsolutePath().toString(), TrackType.Sound, object: PlayerLoadResultHandler {
            override fun onLoadTrack(track: AudioTrack) {
                controls += track
                logger.info { "サウンド: $path をロードしました." }

                launch {
                    val commandExact = path.fileName.toString().split(".").first()
                    val count = singleResult<Long> { db.countDocuments(Filters.and(Filters.eq("guild", guild.idLong), Filters.eq("command", commandExact)), it) } ?: 0L
                    if (count > 0) {
                        singleResult<UpdateResult> {
                            db.updateOne(
                                    Filters.and(Filters.eq("guild", guild.idLong), Filters.eq("command", commandExact)),
                                    Updates.inc("count", 1),
                                    it
                            )
                        }
                    } else {
                        db.insertOne(PlayHistory(guild.idLong, commandExact, 1))
                    }
                }
            }
        })
    }

    data class PlayHistory(val guild: Long, val command: String, val count: Int)
}
