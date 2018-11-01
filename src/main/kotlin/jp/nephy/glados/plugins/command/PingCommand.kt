package jp.nephy.glados.plugins.command

import jp.nephy.glados.core.extensions.await
import jp.nephy.glados.core.extensions.edit
import jp.nephy.glados.core.extensions.launch
import jp.nephy.glados.core.extensions.reply
import jp.nephy.glados.core.plugins.Plugin
import java.time.temporal.ChronoUnit

object PingCommand: Plugin() {
    @Command(description = "GLaDOSの遅延をチェックします。", category = "システム")
    fun ping(event: Command.Event) {
        event.reply {
            message {
                append("Ping...")
            }
        }.launch {
            val ping = event.message.creationTime.until(it.creationTime, ChronoUnit.MILLIS)
            it.edit {
                message {
                    append("Ping: " + ping + "ms | WebSocket: " + event.jda.ping + "ms")
                }
            }.await()
        }
    }
}
