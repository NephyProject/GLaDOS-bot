package jp.nephy.glados.core.plugins

import jp.nephy.glados.core.plugins.extensions.jda.launchAndDelete
import jp.nephy.glados.core.plugins.extensions.jda.messages.message
import jp.nephy.glados.core.plugins.extensions.jda.messages.HexColor
import jp.nephy.glados.core.plugins.extensions.jda.messages.reply
import net.dv8tion.jda.core.entities.Message
import java.util.concurrent.TimeUnit

abstract class CommandError(val jdaMessage: Message, val commandName: String, val description: String): Throwable() {
    override val message: String
        get() = "コマンド: $commandName\n$description"

    init {
        @Suppress("LeakingThis") sendErrorReport()
    }

    abstract fun sendErrorReport()

    class Embed(jdaMessage: Message, commandName: String, description: String): CommandError(jdaMessage, commandName, description) {
        override fun sendErrorReport() {
            jdaMessage.reply {
                embed {
                    title("コマンドエラー: $commandName")
                    description { description }
                    timestamp()
                    color(HexColor.Bad)
                }
            }.launchAndDelete(30, TimeUnit.SECONDS)
        }
    }

    class Simple(jdaMessage: Message, commandName: String, description: String): CommandError(jdaMessage, commandName, description) {
        override fun sendErrorReport() {
            jdaMessage.message {
                text {
                    append("${jdaMessage.author.asMention} $description")
                }
            }.launchAndDelete(30, TimeUnit.SECONDS)
        }
    }
}
