package jp.nephy.glados.features

import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.edit
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Command
import jp.nephy.glados.core.feature.subscription.CommandEvent
import jp.nephy.utils.randomChoice

class RandomCommand: BotFeature() {
    @Command(description = "引数からランダムに選びます。", args = ["A", "B", "..."], checkArgsCount = false)
    fun random(event: CommandEvent) {
        val elements = event.argList.toSet()
        if (elements.size < 2) {
            return event.reply {
                embed {
                    title("コマンドエラー: !random")
                    description { "引数は2個以上必要です。" }
                    timestamp()
                    color(Color.Bad)
                }
            }.queue()
        }

        event.reply {
            message {
                append("...")
            }
        }.queue {
            it.edit {
                message {
                    append(elements.randomChoice())
                }
            }.queue()
        }
    }
}
