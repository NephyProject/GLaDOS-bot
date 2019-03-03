/*
 * The MIT License (MIT)
 *
 *     Copyright (c) 2017-2019 Nephy Project Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package jp.nephy.glados.clients.discord.extensions.messages.prompt

import jp.nephy.glados.api.Logger
import jp.nephy.glados.clients.discord.extensions.DiscordEventWaiter
import jp.nephy.glados.clients.discord.extensions.await
import jp.nephy.glados.clients.discord.extensions.launch
import jp.nephy.glados.clients.discord.extensions.launchAndDelete
import jp.nephy.glados.clients.discord.extensions.messages.HexColor
import jp.nephy.glados.clients.discord.extensions.messages.edit
import jp.nephy.glados.clients.discord.extensions.messages.reply
import jp.nephy.glados.clients.logger.of
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import java.util.concurrent.TimeUnit

data class EmojiPrompt<E>(
    val emojis: List<E>, val emojiSymbol: (E) -> String, val emojiTitle: (E) -> String, val emojiDescription: (E) -> String?, val title: String?, val authorName: String?, val authorUrl: String?, val authorImageUrl: String?, val description: String?, val color: HexColor?, val timeoutSecs: Long?
) {
    data class Emoji(val symbol: String, val name: String, val description: String?)

    data class PromptResult<E>(val selected: E, val event: MessageReactionAddEvent)

    class Builder<E>(emojis: List<E> = emptyList()) {
        private val emojis = emojis.toMutableList()
        fun emoji(emoji: E) {
            emojis += emoji
        }

        private var emojiSymbol: (E) -> String = {
            when (it) {
                is EmojiEnum -> it.symbol
                is Emoji -> it.symbol
                else -> it.toString()
            }
        }

        fun emojiSymbol(converter: (E) -> String) {
            emojiSymbol = converter
        }

        private var emojiTitle: (E) -> String = {
            when (it) {
                is EmojiEnum -> it.promptTitle
                is Emoji -> it.name
                else -> it.toString()
            }
        }

        fun emojiTitle(converter: (E) -> String) {
            emojiTitle = converter
        }

        private var emojiDescription: (E) -> String? = {
            when (it) {
                is EmojiEnum -> it.promptDescription
                is Emoji -> it.description
                else -> it.toString()
            }
        }

        fun emojiDescription(converter: (E) -> String?) {
            emojiDescription = converter
        }

        private var title: String? = null
        fun title(value: String) {
            title = value
        }

        private var authorName: String? = null
        private var authorUrl: String? = null
        private var authorImageUrl: String? = null
        fun author(name: String, url: String? = null, imageUrl: String? = null) {
            authorName = name
            authorUrl = url
            authorImageUrl = imageUrl
        }

        private var description: String? = null
        fun description(value: () -> String) {
            description = value.invoke()
        }

        private var color: HexColor? = null
        fun color(hexColor: HexColor) {
            color = hexColor
        }

        private var timeoutSecs: Long? = null
        fun timeout(value: Long, unit: TimeUnit) {
            timeoutSecs = unit.toSeconds(value)
        }

        internal fun build(): EmojiPrompt<E> {
            check(emojis.isNotEmpty())

            return EmojiPrompt(emojis, emojiSymbol, emojiTitle, emojiDescription, title, authorName, authorUrl, authorImageUrl, description, color, timeoutSecs)
        }
    }

    companion object {
        private val logger = Logger.of("GLaDOS.Discord.Message.EmojiPrompt")

        @Suppress("ResultIsResult")
        suspend fun <E> create(channel: MessageChannel, target: User, emojis: List<E> = emptyList(), builder: Builder<E>.() -> Unit): Result<PromptResult<E>> {
            val prompt = Builder(emojis).apply(builder).build()

            val promptMessage = channel.reply(target) {
                embed {
                    if (prompt.title != null) {
                        title(prompt.title)
                    }

                    if (prompt.authorName != null) {
                        author(prompt.authorName, prompt.authorUrl, prompt.authorImageUrl)
                    }

                    descriptionBuilder {
                        if (prompt.description != null) {
                            appendln(prompt.description)
                        }

                        append(prompt.emojis.joinToString(" / ") { prompt.emojiTitle(it) })
                        appendln("が利用可能です。絵文字で選択してください。")
                        if (prompt.timeoutSecs != null) {
                            appendln("応答がない場合 ${prompt.timeoutSecs}秒後に自動でプロンプトを終了します。")
                        }
                        append(prompt.emojis.joinToString(" / ") { "${prompt.emojiSymbol(it)}: ${prompt.emojiDescription(it) ?: prompt.emojiTitle(it)}" })
                    }

                    if (prompt.color != null) {
                        color(prompt.color)
                    }

                    timestamp()
                }
            }.await()

            for (emoji in prompt.emojis) {
                promptMessage.addReaction(prompt.emojiSymbol(emoji)).launch()
            }

            return runCatching {
                val event = DiscordEventWaiter.waitCatching<MessageReactionAddEvent>(prompt.timeoutSecs, TimeUnit.SECONDS) {
                    it.user.idLong == target.idLong && it.messageIdLong == promptMessage.idLong && prompt.emojis.any { emoji -> prompt.emojiSymbol(emoji) == it.reactionEmote.name }
                }.getOrThrow()
                val selected = prompt.emojis.find { emoji -> prompt.emojiSymbol(emoji) == event.reactionEmote.name }!!

                PromptResult(selected, event)
            }.onSuccess { result ->
                val selected = prompt.emojis.find { emoji -> prompt.emojiSymbol(emoji) == result.event.reactionEmote.name }!!

                promptMessage.edit {
                    embed {
                        if (prompt.title != null) {
                            title(prompt.title)
                        }

                        if (prompt.authorName != null) {
                            author(prompt.authorName, prompt.authorUrl, prompt.authorImageUrl)
                        }

                        description { "${prompt.emojiSymbol(selected)} ${prompt.emojiTitle(selected)} (${prompt.emojiDescription(selected)}) が選択されました。" }

                        if (prompt.color != null) {
                            color(prompt.color)
                        }

                        timestamp()
                    }
                }.launchAndDelete(10, TimeUnit.SECONDS)
            }.onFailure {
                logger.error(it) { "プロンプト待機中に例外が発生しました。" }

                promptMessage.delete().await()
            }
        }
    }
}
