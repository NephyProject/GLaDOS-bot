package jp.nephy.glados.features

import jp.nephy.glados.core.builder.edit
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Command
import jp.nephy.glados.core.feature.subscription.CommandEvent
import java.time.temporal.ChronoUnit

class PingCommand: BotFeature() {
    @Command(description = "GLaDOSの遅延をチェックします。", category = "システム")
    fun ping(event: CommandEvent) {
        event.reply {
            message {
                append("Ping...")
            }
        }.queue {
            val ping = event.message.creationTime.until(it.creationTime, ChronoUnit.MILLIS)
            it.edit {
                message {
                    append("Ping: " + ping + "ms | WebSocket: " + event.jda.ping + "ms")
                }
            }.queue()
        }
    }
}
