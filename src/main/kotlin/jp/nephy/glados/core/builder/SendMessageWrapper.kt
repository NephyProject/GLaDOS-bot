package jp.nephy.glados.core.builder

import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.IMentionable
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.requests.restaction.MessageAction

class SendMessageWrapper(private val channel: MessageChannel, private val mention: IMentionable? = null): MessageWrapper {
    private var message: Message? = null
    override fun message(operation: MessageBuilder.() -> Unit) {
        message = MessageBuilder().apply(operation).apply {
            if (mention != null) {
                append(mention)
            }
        }.build()
    }

    private var embed: MessageEmbed? = null
    override fun embed(operation: EmbedBuilder.() -> Unit) {
        embed = EmbedBuilder().apply(operation).apply {
            if (mention != null) {
                asMention(mention)
            }
        }.build()
    }

    override fun build(): MessageAction {
        return when {
            message != null -> {
                channel.sendTyping().queue()
                channel.sendMessage(message)
            }
            embed != null -> {
                channel.sendTyping().queue()
                channel.sendMessage(embed)
            }
            else -> {
                throw IllegalStateException()
            }
        }
    }
}
