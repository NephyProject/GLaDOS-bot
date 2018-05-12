package jp.nephy.glados.component.helper.prompt

import jp.nephy.glados.GLaDOS
import jp.nephy.glados.component.helper.Color
import jp.nephy.glados.component.helper.deleteQueue
import jp.nephy.glados.component.helper.embedMention
import jp.nephy.glados.component.helper.wait
import jp.nephy.utils.joinToStringIndexed
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import java.util.concurrent.TimeUnit


class PromptBuilder private constructor(val textChannel: TextChannel, val target: Member) {
    companion object {
        fun build(textChannel: TextChannel, member: Member, operation: PromptBuilder.() -> Unit) {
            val builder = PromptBuilder(textChannel, member)
            operation(builder)
        }
    }

    inline fun <T: IPromptEmoji, reified R: Enum<T>> emojiPrompt(
            author: String? = null,
            title: String? = null,
            description: String? = null,
            color: Color = Color.Plain,
            timeoutSec: Int? = null,
            crossinline then: (selected: R, message: Message, event: GuildMessageReactionAddEvent) -> Unit
    ) {
        @Suppress("UNCHECKED_CAST")
        val enumConstants = R::class.java.enumConstants.map { it as T }
        textChannel.embedMention(target) {
            if (author != null) {
                author(author)
            }
            if (title != null) {
                title(title)
            }
            descriptionBuilder {
                if (description != null) {
                    appendln(description)
                }
                append(
                        enumConstants.joinToString(" / ") { it.friendlyName }
                )
                appendln("が利用可能です。絵文字で選択してください。")
                if (timeoutSec != null) {
                    appendln("応答がない場合 ${timeoutSec}秒後に自動でプロンプトを終了します。")
                }
                append(
                        enumConstants.joinToString(" / ") { "${it.emoji}: ${it.friendlyName}" }
                )
            }
            color(color)
            timestamp()
        }.queue { m ->
            enumConstants.forEach {
                m.addReaction(it.emoji).queue({}, {})
            }

            GLaDOS.instance.eventWaiter.wait<GuildMessageReactionAddEvent>({
                user.idLong == target.user.idLong
                        && messageIdLong == m.idLong
                        && ! enumConstants.none { it.emoji == reactionEmote.name }
            }, timeoutSec?.times(1000L), {
                m.delete().queue({}, {})
            }) {
                val selected = enumConstants.find { it.emoji == reactionEmote.name }!!

                textChannel.embedMention(target) {
                    if (author != null) {
                        author(author)
                    }
                    if (title != null) {
                        title(title)
                    }
                    description { "${selected.friendlyName} が選択されました。" }
                    color(color)
                    timestamp()
                }.deleteQueue(30, TimeUnit.SECONDS) {
                    m.delete().queue({}, {})
                    @Suppress("UNCHECKED_CAST")
                    then(selected as R, it, this)
                }
            }
        }
    }

    inline fun <T: IPromptEnum, reified R: Enum<T>> enumPrompt(
            default: R,
            author: String? = null,
            title: String? = null,
            description: String? = null,
            color: Color = Color.Plain,
            timeoutSec: Int? = null,
            crossinline then: (selected: R, message: Message, event: GuildMessageReceivedEvent) -> Unit
    ) {
        @Suppress("UNCHECKED_CAST")
        val enumConstants = R::class.java.enumConstants.map { it as T }
        val digit = "^(\\d+)$".toRegex()

        textChannel.embedMention(target) {
            if (author != null) {
                author(author)
            }
            if (title != null) {
                title(title)
            }
            descriptionBuilder {
                if (description != null) {
                    appendln(description)
                }
                appendln("番号で回答してください。")
                if (timeoutSec != null) {
                    appendln("応答がない場合 ${timeoutSec}秒後に自動でプロンプトを終了します。")
                }
            }
            blankField()

            if (enumConstants.size <= 20) {
                enumConstants.forEachIndexed { i, t ->
                    field("#$i", true) { t.friendlyName }
                }
            } else {
                enumConstants.chunked(4).forEachIndexed { i, list ->
                    val first = i * 4
                    val last = first + 3
                    field("#$first~#$last", false) {
                        list.joinToStringIndexed(" / ") { j, t -> "#${first + j}: ${t.friendlyName}" }
                    }
                }
            }

            color(color)
            timestamp()
        }.queue { m ->
            GLaDOS.instance.eventWaiter.wait<GuildMessageReceivedEvent>({
                member.user.idLong == target.user.idLong
                        && digit.containsMatchIn(message.contentDisplay).apply {
                    println(message.contentDisplay)
                }
            }, timeoutSec?.times(1000L), {
                m.delete().queue({}, {})
            }) {
                val number = digit.find(message.contentDisplay)!!.value.toInt()
                @Suppress("UNCHECKED_CAST")
                val selected = enumConstants.getOrElse(number) { default as T }

                textChannel.embedMention(target) {
                    if (author != null) {
                        author(author)
                    }
                    if (title != null) {
                        title(title)
                    }
                    description { "${selected.friendlyName} が選択されました。" }
                    color(color)
                }.deleteQueue(30, TimeUnit.SECONDS) {
                    m.delete().queue({}, {})
                    @Suppress("UNCHECKED_CAST")
                    then(selected as R, it, this)
                }
            }
        }
    }

    fun <T> listPrompt(
            list: List<T>,
            default: T,
            itemTitle: (T) -> String = { toString() },
            itemDescription: (T) -> String = { toString() },
            author: String? = null,
            title: String? = null,
            description: String? = null,
            color: Color = Color.Plain,
            timeoutSec: Int? = null,
            then: (selected: T, message: Message, event: GuildMessageReceivedEvent) -> Unit
    ) {
        val digit = "^(\\d+)$".toRegex()

        textChannel.embedMention(target) {
            if (author != null) {
                author(author)
            }
            if (title != null) {
                title(title)
            }
            descriptionBuilder {
                if (description != null) {
                    appendln(description)
                }
                appendln("番号で回答してください。")
                if (timeoutSec != null) {
                    appendln("応答がない場合 ${timeoutSec}秒後に自動でプロンプトを終了します。")
                }
            }
            blankField()

            if (list.size <= 20) {
                list.forEachIndexed { i, t ->
                    field("#$i ${itemTitle(t)}", false) { itemDescription(t) }
                }
            } else {
                list.chunked(4).forEachIndexed { i, list ->
                    val first = i * 4
                    val last = first + 3
                    field("#$first~#$last", false) {
                        list.joinToStringIndexed(" / ") { j, t -> "#${first + j}: ${itemDescription(t)}" }
                    }
                }
            }

            color(color)
            timestamp()
        }.queue { m ->
            GLaDOS.instance.eventWaiter.wait<GuildMessageReceivedEvent>({
                member.user.idLong == target.user.idLong
                        && digit.containsMatchIn(message.contentDisplay).apply {
                    println(message.contentDisplay)
                }
            }, timeoutSec?.times(1000L), {
                m.delete().queue({}, {})
            }) {
                val number = digit.find(message.contentDisplay)!!.value.toInt()
                val selected = list.getOrElse(number) { default }

                textChannel.embedMention(target) {
                    if (author != null) {
                        author(author)
                    }
                    if (title != null) {
                        title(title)
                    }
                    descriptionBuilder {
                        appendln(itemTitle(selected))
                        appendln(itemDescription(selected))
                        append("が選択されました。")
                    }
                    color(color)
                }.deleteQueue(30, TimeUnit.SECONDS) {
                    m.delete().queue({}, {})
                    @Suppress("UNCHECKED_CAST")
                    then(selected, it, this)
                }
            }
        }
    }
}
