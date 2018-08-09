package jp.nephy.glados.features

import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Command
import jp.nephy.glados.core.feature.subscription.CommandEvent
import jp.nephy.glados.core.feature.subscription.CommandPermission

class ShutdownCommand: BotFeature() {
    @Command(permission = CommandPermission.OwnerOnly, description = "GLaDOSを安全にシャットダウンします。")
    fun shutdown(event: CommandEvent) {
        event.reply {
            message {
                append("終了します...")
            }
        }.queue {
            event.jda.shutdown()
        }
    }
}
