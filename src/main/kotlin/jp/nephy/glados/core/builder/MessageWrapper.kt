package jp.nephy.glados.core.builder

import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.requests.restaction.MessageAction

interface MessageWrapper {
    fun message(operation: MessageBuilder.() -> Unit)

    fun embed(operation: EmbedBuilder.() -> Unit)

    fun build(): MessageAction
}
