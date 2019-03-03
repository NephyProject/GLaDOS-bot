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
import jp.nephy.glados.clients.discord.extensions.launchAndDelete
import jp.nephy.glados.clients.discord.extensions.messages.HexColor
import jp.nephy.glados.clients.discord.extensions.messages.edit
import jp.nephy.glados.clients.discord.extensions.messages.reply
import jp.nephy.glados.clients.logger.of
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.concurrent.TimeUnit

data class ListPrompt<T>(
    val items: List<T>, val itemTitle: (T) -> String, val itemDescription: (T) -> String?, val defaultItem: T?, val title: String?, val authorName: String?, val authorUrl: String?, val authorImageUrl: String?, val description: String?, val color: HexColor?, val timeoutSecs: Long?
) {
    data class PromptResult<T>(val item: T, val event: MessageReceivedEvent)

    class Builder<T>(private val items: List<T>) {
        private var itemTitle: (T) -> String = {
            if (it is PromptEnum) {
                it.promptTitle
            } else {
                it.toString()
            }
        }

        fun itemTitle(converter: (T) -> String) {
            itemTitle = converter
        }

        private var itemDescription: (T) -> String? = {
            if (it is PromptEnum) {
                it.promptDescription
            } else {
                it.toString()
            }
        }

        fun itemDescription(converter: (T) -> String?) {
            itemDescription = converter
        }

        private var defaultItem: T? = null
        fun defaultItem(item: T) {
            defaultItem = item
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

        internal fun build(): ListPrompt<T> {
            check(items.isNotEmpty())

            return ListPrompt(items, itemTitle, itemDescription, defaultItem, title, authorName, authorUrl, authorImageUrl, description, color, timeoutSecs)
        }
    }

    companion object {
        private val logger = Logger.of("GLaDOS.Discord.Message.ListPrompt")
        private val digitRegex = "^#?(\\d+)$".toRegex()

        @Suppress("ResultIsResult")
        suspend fun <T> create(channel: MessageChannel, target: User, items: List<T>, builder: Builder<T>.() -> Unit): Result<PromptResult<T>> {
            val prompt = Builder(items).apply(builder).build()

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
                        appendln("番号で回答してください。")
                        if (prompt.timeoutSecs != null) {
                            appendln("応答がない場合 ${prompt.timeoutSecs}秒後に自動でプロンプトを終了します。")
                        }
                    }
                    blankField()

                    if (items.size <= 20) {
                        for ((i, t) in items.withIndex()) {
                            field("#$i ${prompt.itemTitle(t)}", false) { prompt.itemDescription(t) ?: prompt.itemTitle(t) }
                        }
                    } else {
                        for ((i, list) in items.chunked(4).withIndex()) {
                            val first = i * 4
                            val last = first + 3
                            field("#$first~#$last", false) {
                                list.mapIndexed { j, t ->
                                    "#${first + j}: ${prompt.itemDescription(t)}"
                                }.joinToString(" / ")
                            }
                        }
                    }

                    if (prompt.color != null) {
                        color(prompt.color)
                    }

                    timestamp()
                }
            }.await()

            return runCatching {
                val event = DiscordEventWaiter.waitCatching<MessageReceivedEvent>(prompt.timeoutSecs, TimeUnit.SECONDS) {
                    it.member.user.idLong == target.idLong && digitRegex.containsMatchIn(it.message.contentDisplay)
                }.getOrThrow()
                val number = digitRegex.find(event.message.contentDisplay)!!.value.toInt()
                val selected = prompt.items.getOrElse(number) { prompt.defaultItem } ?: throw IllegalArgumentException("Item index $number is out of items.")

                PromptResult(selected, event)
            }.onSuccess { result ->
                promptMessage.edit {
                    embed {
                        if (prompt.title != null) {
                            title(prompt.title)
                        }

                        if (prompt.authorName != null) {
                            author(prompt.authorName, prompt.authorUrl, prompt.authorImageUrl)
                        }

                        descriptionBuilder {
                            appendln(prompt.itemTitle(result.item))
                            appendln(prompt.itemDescription(result.item))
                            append("が選択されました。")
                        }

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
