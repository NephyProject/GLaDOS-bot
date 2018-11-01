package jp.nephy.glados.plugins.command

import jp.nephy.glados.core.extensions.*
import jp.nephy.glados.core.extensions.messages.HexColor
import jp.nephy.glados.core.plugins.Plugin
import jp.nephy.glados.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveAllEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

object VoteCommand: Plugin() {
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    @Command(channelType = Command.TargetChannelType.TextChannel, args = ["タイトル", "期限(XdXhXmXs)", "絵文字1", "選択肢1", "絵文字2", "選択肢2", "..."], checkArgsCount = false)
    fun vote(event: Command.Event) {
        val args = event.argList
        reject(args.size < 6 || args.size % 2 != 0) {
            event.embedError {
                "与えられた引数: `$args`は不正です。`!help`を参照してください。"
            }
        }

        val duration = calculateDurationSeconds(args[1])
        rejectNull(duration) {
            event.embedError {
                "与えられた引数: `$args`は不正です。`!help`を参照してください。"
            }
        }

        val calendar = Calendar.getInstance().also {
            it.add(Calendar.SECOND, duration)
        }

        val title = args.first()
        val choices = args.asSequence().drop(2).chunked(2).toList()

        event.textChannel!!.message {
            embed {
                title("「$title」の投票")
                author(event.author.displayName, iconUrl = event.author.effectiveAvatarUrl)
                descriptionBuilder {
                    for (choice in choices) {
                        val (emoji, text) = choice
                        appendln("$emoji: $text")
                    }

                    appendln("${timeFormat.format(calendar.time)} まで (${args[1].replace("d", "日").replace("h", "時間").replace("m", "分").replace("s", "秒")})")
                }
                color(HexColor.Neutral)
                timestamp()
            }
        }.launch { message ->
            for (choice in choices) {
                message.addReaction(choice.first()).launch()
            }
            votes[message.idLong] = ConcurrentHashMap()

            GlobalScope.launch(dispatcher) {
                delay(TimeUnit.SECONDS.toMillis(duration.toLong()))

                event.textChannel.message {
                    embed {
                        title("「$title」の投票結果")
                        author(event.author.displayName, iconUrl = event.author.effectiveAvatarUrl)
                        description { "${timeFormat.format(calendar.time)} 終了" }

                        val voteResult = votes.remove(message.idLong)
                        choices.forEach {
                            val users = voteResult?.get(it.first()).orEmpty().filterNot { it.user.isBotOrSelfUser }
                            field("${it.first()} ${it.last()}: ${users.size}票") {
                                users.joinToString(" ") { it.asMention }
                            }
                        }

                        color(HexColor.Good)
                        timestamp()
                    }
                }.launch {
                    message.delete().launch()
                }
            }
        }
    }

    // TODO
    private val votes = ConcurrentHashMap<Long, ConcurrentHashMap<String, CopyOnWriteArrayList<Member>>>()

    override suspend fun onGuildMessageReactionAdd(event: GuildMessageReactionAddEvent) {
        if (event.messageIdLong in votes) {
            if (event.reactionEmote.name !in votes[event.messageIdLong]!!) {
                votes[event.messageIdLong]!![event.reactionEmote.name] = CopyOnWriteArrayList()
            }

            votes[event.messageIdLong]!![event.reactionEmote.name]!! += event.member
        }
    }

    override suspend fun onGuildMessageReactionRemove(event: GuildMessageReactionRemoveEvent) {
        if (event.messageIdLong in votes) {
            if (event.reactionEmote.name in votes[event.messageIdLong]!!) {
                votes[event.messageIdLong]!![event.reactionEmote.name] = CopyOnWriteArrayList()
            }

            votes[event.messageIdLong]!![event.reactionEmote.name]!! -= event.member
        }
    }

    override suspend fun onGuildMessageReactionRemoveAll(event: GuildMessageReactionRemoveAllEvent) {
        if (event.messageIdLong in votes) {
            votes[event.messageIdLong] = ConcurrentHashMap()
        }
    }
}

fun calculateDurationSeconds(text: String): Int? {
    var duration = 0

    val timeRegex = "^(\\d+d)?(\\d+h)?(\\d+m)?(\\d+s)?$".toRegex()
    val (d, h, m, s) = timeRegex.matchEntire(text)?.destructured ?: return null
    if (d.isNotEmpty()) {
        duration += d.dropLast(1).toInt() * 24 * 60 * 60
    }
    if (h.isNotEmpty()) {
        duration += h.dropLast(1).toInt() * 60 * 60
    }
    if (m.isNotEmpty()) {
        duration += m.dropLast(1).toInt() * 60
    }
    if (s.isNotEmpty()) {
        duration += s.dropLast(1).toInt()
    }
    return duration
}
