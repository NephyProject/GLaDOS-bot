package jp.nephy.glados.plugins.command

import jp.nephy.glados.core.extensions.awaitAndDelete
import jp.nephy.glados.core.extensions.messages.HexColor
import jp.nephy.glados.core.extensions.reply
import jp.nephy.glados.core.plugins.Plugin
import jp.nephy.glados.core.plugins.SubscriptionClient
import java.util.concurrent.TimeUnit

object HelpCommand: Plugin() {
    @Command(description = "GLaDOSで利用可能なコマンド一覧を表示します。", category = "システム")
    suspend fun help(event: Command.Event) {
        event.reply {
            embed {
                title(event.command.primaryCommandSyntax)
                description { "利用可能なGLaDOSコマンド一覧です。" }
                color(HexColor.Plain)
                timestamp()

                val categorizedSubscriptions = SubscriptionClient.Command.subscriptions.sortedBy {
                    it.primaryCommandName
                }.groupBy {
                    it.category
                }

                for ((category, subscriptions) in categorizedSubscriptions) {
                    field(category ?: "未分類") {
                        buildString {
                            for (subscription in subscriptions) {
                                appendln("`${subscription.primaryCommandSyntax} ${subscription.arguments.joinToString(" ") { "[$it]" }}`")
                                if (subscription.description != null) {
                                    appendln("  ${subscription.description}")
                                }
                            }
                        }
                    }
                }
            }
        }.awaitAndDelete(5, TimeUnit.MINUTES)
    }
}
