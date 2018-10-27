package jp.nephy.glados.features.command

import jp.nephy.glados.core.awaitAndDelete
import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Command
import jp.nephy.glados.core.feature.subscription.CommandEvent
import jp.nephy.glados.core.feature.subscription.CommandSubscription
import jp.nephy.glados.featureManager
import java.util.concurrent.TimeUnit

class HelpCommand: BotFeature() {
    @Command(description = "GLaDOSで利用可能なコマンド一覧を表示します。", category = "システム")
    suspend fun help(event: CommandEvent) {
        event.reply {
            embed {
                title("!help")
                description { "利用可能なGLaDOSコマンド一覧です。" }
                color(Color.Plain)
                timestamp()

                val categorizedSubscriptions = featureManager.commandClient.subscriptions.map {
                    it as CommandSubscription
                }.sortedBy {
                    it.name
                }.groupBy {
                    it.category
                }

                for ((category, subscriptions) in categorizedSubscriptions) {
                    field(category ?: "未分類") {
                        buildString {
                            for (subscription in subscriptions) {
                                appendln("`${subscription.primaryCommandSyntax} ${subscription.annotation.args.joinToString(" ") { "[$it]" }}`")
                                if (subscription.annotation.description.isNotBlank()) {
                                    appendln("  ${subscription.annotation.description}")
                                }
                            }
                        }
                    }
                }
            }
        }.awaitAndDelete(5, TimeUnit.MINUTES)
    }
}
