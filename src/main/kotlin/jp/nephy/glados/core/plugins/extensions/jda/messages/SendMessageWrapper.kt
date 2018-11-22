package jp.nephy.glados.core.plugins.extensions.jda.messages

import jp.nephy.glados.core.plugins.extensions.jda.launch
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.IMentionable
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.requests.restaction.MessageAction

class SendMessageWrapper(private val channel: MessageChannel, private val mention: IMentionable? = null): MessageWrapper {
    private var message: Message? = null
    override fun text(operation: MessageBuilder.() -> Unit) {
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
                channel.sendTyping().launch()
                channel.sendMessage(message)
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
