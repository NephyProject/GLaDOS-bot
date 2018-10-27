package jp.nephy.glados.features.command

import jp.nephy.glados.core.awaitAndDelete
import jp.nephy.glados.core.builder.edit
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.embedError
import jp.nephy.glados.core.feature.reject
import jp.nephy.glados.core.feature.subscription.Command
import jp.nephy.glados.core.feature.subscription.CommandEvent
import jp.nephy.glados.core.launch
import jp.nephy.utils.randomChoice
import java.util.concurrent.TimeUnit

class RandomCommand: BotFeature() {
    @Command(description = "引数からランダムに選びます。", args = ["A", "B", "..."], checkArgsCount = false)
    fun random(event: CommandEvent) {
        val elements = event.argList.toSet()

        reject(elements.size < 2) {
            event.embedError("!random") {
                "引数は2個以上必要です。"
            }
        }

        event.reply {
            message {
                append("...")
            }
        }.launch {
            it.edit {
                message {
                    append(elements.randomChoice())
                }
            }.awaitAndDelete(1, TimeUnit.MINUTES)
        }
    }
}
