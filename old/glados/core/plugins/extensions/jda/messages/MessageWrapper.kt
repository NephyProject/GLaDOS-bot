package jp.nephy.glados.core.plugins.extensions.jda.messages

import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.requests.restaction.MessageAction

interface MessageWrapper {
    fun text(operation: MessageBuilder.() -> Unit)

    fun embed(operation: EmbedBuilder.() -> Unit)

    fun build(): MessageAction
}
