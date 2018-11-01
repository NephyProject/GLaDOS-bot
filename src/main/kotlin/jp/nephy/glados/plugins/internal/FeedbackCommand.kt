package jp.nephy.glados.plugins.internal

import jp.nephy.glados.core.extensions.*
import jp.nephy.glados.core.plugins.Plugin

object FeedbackCommand: Plugin() {
    private val channels by textChannelsLazy("feedback")

    @Command(aliases = ["report"], description = "GLaDOSの開発者にフィードバックやバグ報告が行なえます。", args = ["内容"], checkArgsCount = false, category = "システム")
    fun feedback(event: Command.Event) {
        reject(event.args.isBlank()) {
            event.embedError {
                "フィードバック内容が空です。"
            }
        }

        for (channel in channels) {
            channel.message {
                embed {
                    title("新しいフィードバックが送信されました")
                    author(event.authorName, iconUrl = event.author.effectiveAvatarUrl)
                    description { event.args }
                    timestamp()
                }
            }.launch()
        }
    }
}
