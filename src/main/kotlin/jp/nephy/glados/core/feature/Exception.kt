package jp.nephy.glados.core.feature

import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.message
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.subscription.CommandEvent
import jp.nephy.glados.core.launchAndDelete
import net.dv8tion.jda.core.entities.Message
import java.util.concurrent.TimeUnit

abstract class CommandError(val jdaMessage: Message, val commandName: String, val description: String): Exception() {
    override val message: String
        get() = "コマンド: $commandName\n$description"

    init {
        sendErrorReport()
    }

    abstract fun sendErrorReport()
}

class EmbedCommandError(jdaMessage: Message, commandName: String, description: String): CommandError(jdaMessage, commandName, description) {
    override fun sendErrorReport() {
        jdaMessage.reply {
            embed {
                title("コマンドエラー: $commandName")
                description { description }
                timestamp()
                color(Color.Bad)
            }
        }.launchAndDelete(30, TimeUnit.SECONDS)
    }
}

class SimpleCommandError(jdaMessage: Message, commandName: String, description: String): CommandError(jdaMessage, commandName, description) {
    override fun sendErrorReport() {
        jdaMessage.message {
            message {
                append("${jdaMessage.author.asMention} $description")
            }
        }.launchAndDelete(30, TimeUnit.SECONDS)
    }
}

inline fun Message.embedError(commandName: String, description: () -> String): Nothing = throw EmbedCommandError(this, commandName, description.invoke())

inline fun Message.simpleError(commandName: String, description: StringBuilder.() -> Unit): Nothing = throw SimpleCommandError(this, commandName, buildString(description))

inline fun CommandEvent.embedError(commandName: String, description: () -> String): Nothing = throw EmbedCommandError(message, commandName, description.invoke())

inline fun CommandEvent.simpleError(commandName: String, description: StringBuilder.() -> Unit): Nothing = throw SimpleCommandError(message, commandName, buildString(description))
