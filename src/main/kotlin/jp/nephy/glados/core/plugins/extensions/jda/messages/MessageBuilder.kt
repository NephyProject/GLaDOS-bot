package jp.nephy.glados.core.plugins.extensions.jda.messages

import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import jp.nephy.glados.GLaDOS.dispatcher
import jp.nephy.glados.core.plugins.Plugin
import jp.nephy.glados.core.plugins.extensions.jda.messages.prompt.PromptBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.MessageUpdateEvent
import net.dv8tion.jda.core.requests.restaction.MessageAction

/* Reply */

inline fun Plugin.Command.Event.reply(operation: SendMessageWrapper.() -> Unit): MessageAction {
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

// TODO
inline fun Message.prompt(operation: PromptBuilder.() -> Unit) {
    PromptBuilder(textChannel, member).apply(operation)
}

inline fun TextChannel.prompt(to: Member, operation: PromptBuilder.() -> Unit) {
    PromptBuilder(this, to).apply(operation)
}

inline fun <reified T: Event> EventWaiter.wait(noinline condition: T.() -> Boolean = { true }, timeout: Long? = null, noinline whenTimeout: () -> Unit = { }, noinline operation: T.() -> Unit) {
    var stop = false
    if (timeout != null) {
        GlobalScope.launch(dispatcher) {
            delay(timeout)
            stop = true
            whenTimeout()
        }
    }

    waitForEvent(T::class.java, {
        !stop && condition(it)
    }, {
                     if (stop) {
                         return@waitForEvent
                     }
                     operation(it)
                 })
}
