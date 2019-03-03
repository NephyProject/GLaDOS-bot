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

@file:Suppress("UNUSED")

package jp.nephy.glados.clients.discord.extensions.messages

import jp.nephy.glados.clients.discord.command.DiscordCommandEvent
import jp.nephy.glados.clients.discord.extensions.messages.prompt.EmojiEnum
import jp.nephy.glados.clients.discord.extensions.messages.prompt.EmojiPrompt
import jp.nephy.glados.clients.discord.extensions.messages.prompt.ListPrompt
import jp.nephy.glados.clients.discord.extensions.messages.prompt.PromptEnum
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.requests.restaction.MessageAction

/* Reply */

inline fun DiscordCommandEvent.reply(operation: SendMessageWrapper.() -> Unit): MessageAction {
    return SendMessageWrapper(channel, author).apply(operation).build()
}

inline fun MessageReceivedEvent.reply(operation: SendMessageWrapper.() -> Unit): MessageAction {
    return SendMessageWrapper(channel, author).apply(operation).build()
}

inline fun MessageUpdateEvent.reply(operation: SendMessageWrapper.() -> Unit): MessageAction {
    return SendMessageWrapper(channel, author).apply(operation).build()
}

inline fun Message.reply(operation: SendMessageWrapper.() -> Unit): MessageAction {
    return SendMessageWrapper(channel, author).apply(operation).build()
}

// TODO
inline fun MessageChannel.reply(to: IMentionable, operation: SendMessageWrapper.() -> Unit): MessageAction {
    return SendMessageWrapper(this, to).apply(operation).build()
}

/* Message */

inline fun Message.message(operation: SendMessageWrapper.() -> Unit): MessageAction {
    return SendMessageWrapper(channel).apply(operation).build()
}

inline fun MessageChannel.message(operation: SendMessageWrapper.() -> Unit): MessageAction {
    return SendMessageWrapper(this).apply(operation).build()
}

/* Edit */

inline fun Message.edit(operation: EditMessageWrapper.() -> Unit): MessageAction {
    return EditMessageWrapper(this).apply(operation).build()
}

/* Prompt */
@Suppress("ResultIsResult")
suspend inline fun Message.emojiPrompt(noinline operation: EmojiPrompt.Builder<EmojiPrompt.Emoji>.() -> Unit): Result<EmojiPrompt.PromptResult<EmojiPrompt.Emoji>> {
    return textChannel.emojiPrompt(author, operation)
}

@Suppress("ResultIsResult")
suspend inline fun TextChannel.emojiPrompt(to: Member, noinline operation: EmojiPrompt.Builder<EmojiPrompt.Emoji>.() -> Unit): Result<EmojiPrompt.PromptResult<EmojiPrompt.Emoji>> {
    return emojiPrompt(to.user, operation)
}

@Suppress("ResultIsResult")
suspend inline fun TextChannel.emojiPrompt(to: User, noinline operation: EmojiPrompt.Builder<EmojiPrompt.Emoji>.() -> Unit): Result<EmojiPrompt.PromptResult<EmojiPrompt.Emoji>> {
    return EmojiPrompt.create(this, to, builder = operation)
}

@Suppress("ResultIsResult")
suspend inline fun <reified E: Enum<out EmojiEnum>> Message.emojiEnumPrompt(noinline operation: EmojiPrompt.Builder<E>.() -> Unit): Result<EmojiPrompt.PromptResult<E>> {
    return textChannel.emojiEnumPrompt(author, operation)
}

@Suppress("ResultIsResult")
suspend inline fun <reified E: Enum<out EmojiEnum>> TextChannel.emojiEnumPrompt(to: Member, noinline operation: EmojiPrompt.Builder<E>.() -> Unit): Result<EmojiPrompt.PromptResult<E>> {
    return emojiEnumPrompt(to.user, operation)
}

@Suppress("ResultIsResult")
suspend inline fun <reified E: Enum<out EmojiEnum>> TextChannel.emojiEnumPrompt(to: User, noinline operation: EmojiPrompt.Builder<E>.() -> Unit): Result<EmojiPrompt.PromptResult<E>> {
    return EmojiPrompt.create(this, to, E::class.java.enumConstants.toList(), operation)
}

@Suppress("ResultIsResult")
suspend inline fun <T> Message.itemPrompt(items: List<T>, noinline operation: ListPrompt.Builder<T>.() -> Unit): Result<ListPrompt.PromptResult<T>> {
    return textChannel.itemPrompt(author, items, operation)
}

@Suppress("ResultIsResult")
suspend inline fun <T> TextChannel.itemPrompt(to: Member, items: List<T>, noinline operation: ListPrompt.Builder<T>.() -> Unit): Result<ListPrompt.PromptResult<T>> {
    return itemPrompt(to.user, items, operation)
}

@Suppress("ResultIsResult")
suspend inline fun <T> TextChannel.itemPrompt(to: User, items: List<T>, noinline operation: ListPrompt.Builder<T>.() -> Unit): Result<ListPrompt.PromptResult<T>> {
    return ListPrompt.create(this, to, items, operation)
}

@Suppress("ResultIsResult")
suspend inline fun <reified E: Enum<out PromptEnum>> Message.enumPrompt(noinline operation: ListPrompt.Builder<E>.() -> Unit): Result<ListPrompt.PromptResult<E>> {
    return textChannel.itemPrompt(author, E::class.java.enumConstants.toList(), operation)
}

@Suppress("ResultIsResult")
suspend inline fun <reified E: Enum<out PromptEnum>> TextChannel.enumPrompt(to: Member, noinline operation: ListPrompt.Builder<E>.() -> Unit): Result<ListPrompt.PromptResult<E>> {
    return itemPrompt(to.user, E::class.java.enumConstants.toList(), operation)
}

@Suppress("ResultIsResult")
suspend inline fun <reified E: Enum<out PromptEnum>> TextChannel.enumPrompt(to: User, noinline operation: ListPrompt.Builder<E>.() -> Unit): Result<ListPrompt.PromptResult<E>> {
    return ListPrompt.create(this, to, E::class.java.enumConstants.toList(), operation)
}
