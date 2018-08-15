package jp.nephy.glados.features

import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.message
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.displayName
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Command
import jp.nephy.glados.core.feature.subscription.CommandChannelType
import jp.nephy.glados.core.feature.subscription.CommandEvent
import jp.nephy.glados.core.feature.subscription.Listener
import jp.nephy.glados.core.isBotOrSelfUser
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveAllEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class VoteCommand: BotFeature() {
    private val timeRegex = "^(\\d+d)?(\\d+h)?(\\d+m)?(\\d+s)?$".toRegex()
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    @Command(channelType = CommandChannelType.TextChannel, args = "<タイトル> <期限(XdXhXmXs)> <絵文字1> <選択肢1> <絵文字2> <選択肢2> ...")
    fun vote(event: CommandEvent) {
        val args = event.argList
        if (args.size < 6 || args.size % 2 != 0) {
            return event.error()
        }

        var duration = 0
        val (d, h, m, s) = timeRegex.matchEntire(args[1])?.destructured ?: return event.error()
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
        val calendar = Calendar.getInstance().also {
            it.add(Calendar.SECOND, duration)
        }

        val title = args.first()
        val choices = args.drop(2).chunked(2)

        event.textChannel!!.message {
            embed {
                title("「$title」の投票")
                author(event.user.displayName, iconUrl = event.user.effectiveAvatarUrl)
                descriptionBuilder {
                    for (choice in choices) {
                        val (emoji, text) = choice
                        appendln("$emoji: $text")
                    }

                    appendln("${timeFormat.format(calendar.time)} まで (${args[1].replace("d", "日").replace("h", "時間").replace("m", "分").replace("s", "秒")})")
                }
                color(Color.Neutral)
                timestamp()
            }
        }.queue { message ->
            for (choice in choices) {
                message.addReaction(choice.first()).complete()
            }
            votes[message.idLong] = mutableMapOf()

            thread {
                TimeUnit.SECONDS.sleep(duration.toLong())

                event.textChannel.message {
                    embed {
                        title("「$title」の投票結果")
                        author(event.user.displayName, iconUrl = event.user.effectiveAvatarUrl)
                        description { "${timeFormat.format(calendar.time)} 終了" }

                        val voteResult = votes.remove(message.idLong)
                        choices.forEach {
                            val users = voteResult?.get(it.first()).orEmpty().filterNot { it.user.isBotOrSelfUser }
                            field("${it.first()} ${it.last()}: ${users.size}票") {
                                users.joinToString(" ") { it.asMention }
                            }
                        }

                        color(Color.Good)
                        timestamp()
                    }
                }.queue {
                    message.delete().queue()
                }
            }
        }
    }

    private fun CommandEvent.error() {
        reply {
            embed {
                title("コマンドエラー: vote")
                description { "与えられた引数: `$args`は不正です。`help vote`を参照してください。" }
                color(Color.Bad)
                timestamp()
            }
        }.queue()
    }

    private val votes = mutableMapOf<Long, MutableMap<String, MutableList<Member>>>()

    @Listener
    override fun onGuildMessageReactionAdd(event: GuildMessageReactionAddEvent) {
        if (event.messageIdLong in votes) {
            if (event.reactionEmote.name !in votes[event.messageIdLong]!!) {
                votes[event.messageIdLong]!![event.reactionEmote.name] = mutableListOf()
            }

            votes[event.messageIdLong]!![event.reactionEmote.name]!!.add(event.member)
        }
    }

    @Listener
    override fun onGuildMessageReactionRemove(event: GuildMessageReactionRemoveEvent) {
        if (event.messageIdLong in votes) {
            if (event.reactionEmote.name in votes[event.messageIdLong]!!) {
                votes[event.messageIdLong]!![event.reactionEmote.name] = mutableListOf()
            }

            votes[event.messageIdLong]!![event.reactionEmote.name]!!.remove(event.member)
        }
    }

    @Listener
    override fun onGuildMessageReactionRemoveAll(event: GuildMessageReactionRemoveAllEvent) {
        if (event.messageIdLong in votes) {
            votes[event.messageIdLong] = mutableMapOf()
        }
    }
}
