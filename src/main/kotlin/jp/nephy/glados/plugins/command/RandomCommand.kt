package jp.nephy.glados.plugins.command

import jp.nephy.glados.core.extensions.*
import jp.nephy.glados.core.plugins.Plugin
import jp.nephy.utils.randomChoice
import java.util.concurrent.TimeUnit

object RandomCommand: Plugin() {
    @Command(description = "引数からランダムに選びます。", args = ["A", "B", "..."], checkArgsCount = false)
    fun random(event: Command.Event) {
        val elements = event.argList.toSet()

        reject(elements.size < 2) {
            event.embedError {
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
