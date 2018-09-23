package jp.nephy.glados.core.builder

import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import jp.nephy.glados.core.builder.prompt.PromptBuilder
import jp.nephy.glados.core.feature.subscription.CommandEvent
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.requests.restaction.MessageAction
import java.util.concurrent.TimeUnit

fun CommandEvent.reply(operation: SendMessageWrapper.() -> Unit): MessageAction {
    return SendMessageWrapper(channel, user).apply(operation).build()
}

fun Message.reply(operation: SendMessageWrapper.() -> Unit): MessageAction {
    return SendMessageWrapper(channel, author).apply(operation).build()
}

fun Message.message(operation: SendMessageWrapper.() -> Unit): MessageAction {
    return SendMessageWrapper(channel).apply(operation).build()
}

fun Message.edit(operation: EditMessageWrapper.() -> Unit): MessageAction {
    return EditMessageWrapper(this).apply(operation).build()
}

fun MessageChannel.reply(to: IMentionable, operation: SendMessageWrapper.() -> Unit): MessageAction {
    return SendMessageWrapper(this, to).apply(operation).build()
}

fun MessageChannel.message(operation: SendMessageWrapper.() -> Unit): MessageAction {
    return SendMessageWrapper(this).apply(operation).build()
}

fun Message.prompt(operation: PromptBuilder.() -> Unit) {
    PromptBuilder(textChannel, member).apply(operation)
}

fun TextChannel.prompt(to: Member, operation: PromptBuilder.() -> Unit) {
    PromptBuilder(this, to).apply(operation)
}

fun MessageAction.deleteQueue(delay: Long? = null, unit: TimeUnit = TimeUnit.SECONDS, then: (Message) -> Unit = { }) {
    queue {
        if (delay != null) {
            launch {
                kotlinx.coroutines.experimental.delay(delay, unit)
                it.delete().queue({}, {})
            }
        } else {
            it.delete().queue({}, {})
        }
        then(it)
    }
}

inline fun <reified T: Event> EventWaiter.wait(noinline condition: T.() -> Boolean = { true }, timeout: Long? = null, noinline whenTimeout: () -> Unit = { }, noinline operation: T.() -> Unit) {
    var stop = false
    if (timeout != null) {
        launch {
            delay(timeout, TimeUnit.MILLISECONDS)
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
