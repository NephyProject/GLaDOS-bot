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

@file:Suppress("OVERRIDE_BY_INLINE")

package jp.nephy.glados.clients.discord.extensions.messages

import jp.nephy.glados.clients.discord.extensions.launch
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.requests.restaction.MessageAction

class SendMessageWrapper(val channel: MessageChannel, val mention: IMentionable? = null): MessageWrapper {
    var text: Message? = null
    override inline fun text(operation: MessageBuilder.() -> Unit) {
        text = MessageBuilder().apply(operation).apply {
            if (mention != null) {
                append(mention)
            }
        }.build()
    }

    var embed: MessageEmbed? = null
    override inline fun embed(operation: EmbedBuilder.() -> Unit) {
        embed = EmbedBuilder().apply(operation).apply {
            if (mention != null) {
                asMention(mention)
            }
        }.build()
    }

    override fun build(): MessageAction {
        return when {
            text != null -> {
                channel.sendTyping().launch()
                channel.sendMessage(text)
            }
            embed != null -> {
                channel.sendTyping().launch()
                channel.sendMessage(embed)
            }
            else -> {
                throw IllegalStateException()
            }
        }
    }
}
