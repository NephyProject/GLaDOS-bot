@file:Suppress("OVERRIDE_BY_INLINE")

package jp.nephy.glados.core.plugins.extensions.jda.messages

import jp.nephy.glados.core.plugins.extensions.jda.launch
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.IMentionable
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.requests.restaction.MessageAction

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
