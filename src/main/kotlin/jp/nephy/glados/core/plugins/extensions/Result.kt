@file:Suppress("UNUSED")

package jp.nephy.glados.core.plugins.extensions

import jp.nephy.glados.core.plugins.CommandError
import jp.nephy.glados.core.plugins.Plugin
import jp.nephy.glados.core.plugins.extensions.jda.messages.HexColor
import jp.nephy.glados.core.plugins.extensions.jda.messages.message
import jp.nephy.glados.core.plugins.extensions.jda.messages.reply
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.requests.restaction.MessageAction
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@UseExperimental(ExperimentalContracts::class)
inline fun Plugin.reject(value: Boolean, fallback: () -> Nothing) {
    contract {
        returns() implies !value
    }

    if (value) {
        fallback()
    }
}

@UseExperimental(ExperimentalContracts::class)
inline fun <T> Plugin.rejectNull(value: T?, fallback: () -> Nothing) {
    contract {
        returns() implies (value != null)
    }

    if (value == null) {
        fallback()
    }
}

inline fun Message.embedError(commandName: String, description: () -> String): Nothing = throw CommandError.Embed(this, commandName, description.invoke())

inline fun Message.simpleError(commandName: String, description: StringBuilder.() -> Unit): Nothing = throw CommandError.Simple(this, commandName, buildString(description))

inline fun Plugin.Command.Event.embedError(description: () -> String): Nothing = throw CommandError.Embed(message, command.primaryCommandSyntax, description.invoke())

inline fun Plugin.Command.Event.simpleError(description: StringBuilder.() -> Unit): Nothing = throw CommandError.Simple(message, command.primaryCommandSyntax, buildString(description))

fun Message.embedResult(commandName: String, description: () -> String): MessageAction {
    return reply {
        embed {
            title(commandName)
            description(description)
            timestamp()
            color(HexColor.Good)
        }
    }
}

fun Message.simpleResult(description: () -> String): MessageAction {
    return message {
        text {
            append("${author.asMention} ${description.invoke()}")
        }
    }
}

fun Plugin.Command.Event.embedResult(description: () -> String) = message.embedResult(command.primaryCommandSyntax, description)

fun Plugin.Command.Event.simpleResult(description: () -> String) = message.simpleResult(description)
