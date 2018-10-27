package jp.nephy.glados.core.builder

import jp.nephy.glados.core.builder.prompt.PromptBuilder
import jp.nephy.glados.core.builder.wrapper.EditMessageWrapper
import jp.nephy.glados.core.builder.wrapper.SendMessageWrapper
import jp.nephy.glados.core.feature.subscription.CommandEvent
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.MessageUpdateEvent
import net.dv8tion.jda.core.requests.restaction.MessageAction

/* Reply */

fun CommandEvent.reply(operation: SendMessageWrapper.() -> Unit): MessageAction {
    return SendMessageWrapper(channel, user).apply(operation).build()
}

fun MessageReceivedEvent.reply(operation: SendMessageWrapper.() -> Unit): MessageAction {
    return SendMessageWrapper(channel, author).apply(operation).build()
}

fun MessageUpdateEvent.reply(operation: SendMessageWrapper.() -> Unit): MessageAction {
    return SendMessageWrapper(channel, author).apply(operation).build()
}

fun Message.reply(operation: SendMessageWrapper.() -> Unit): MessageAction {
    return SendMessageWrapper(channel, author).apply(operation).build()
}

fun MessageChannel.reply(to: IMentionable, operation: SendMessageWrapper.() -> Unit): MessageAction {
    return SendMessageWrapper(this, to).apply(operation).build()
}

/* Message */

fun Message.message(operation: SendMessageWrapper.() -> Unit): MessageAction {
    return SendMessageWrapper(channel).apply(operation).build()
}

fun MessageChannel.message(operation: SendMessageWrapper.() -> Unit): MessageAction {
    return SendMessageWrapper(this).apply(operation).build()
}

/* Edit */

fun Message.edit(operation: EditMessageWrapper.() -> Unit): MessageAction {
    return EditMessageWrapper(this).apply(operation).build()
}

// TODO
fun Message.prompt(operation: PromptBuilder.() -> Unit) {
    PromptBuilder(textChannel, member).apply(operation)
}

fun TextChannel.prompt(to: Member, operation: PromptBuilder.() -> Unit) {
    PromptBuilder(this, to).apply(operation)
}
