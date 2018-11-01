package jp.nephy.glados.core.extensions.messages

import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.requests.restaction.MessageAction

class EditMessageWrapper(private val target: Message): MessageWrapper {
    private var message: Message? = null
    override fun message(operation: MessageBuilder.() -> Unit) {
        message = MessageBuilder().apply(operation).build()
    }

    private var embed: MessageEmbed? = null
    override fun embed(operation: EmbedBuilder.() -> Unit) {
        embed = EmbedBuilder().apply(operation).build()
    }

    override fun build(): MessageAction {
        return when {
            message != null -> {
                target.editMessage(message)
            }
            embed != null -> {
                target.editMessage(embed)
            }
            else -> {
                throw IllegalStateException()
            }
        }
    }
}
