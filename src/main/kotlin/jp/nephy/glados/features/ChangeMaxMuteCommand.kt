package jp.nephy.glados.features

import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Command
import jp.nephy.glados.core.feature.subscription.CommandEvent
import jp.nephy.glados.core.feature.subscription.CommandPermission

class ChangeMaxMuteCommand: BotFeature() {
    private val maxMuteMininum = 5

    @Command(permission = CommandPermission.AdminOnly, description = "[Mute Limit]チャンネルで適用する最大のミュート時間を指定します。", args = "<秒>")
    fun maxmute(event: CommandEvent) {
        val n = event.args.toIntOrNull()
        if (n == null || n < maxMuteMininum) {
            return event.reply {
                embed {
                    title("maxmute")
                    description { "不正な値です。maxmuteは${maxMuteMininum}以上の整数を指定できます。" }
                    color(Color.Bad)
                    timestamp()
                }
            }.queue()
        }

        MuteLimitChannel.maxMuteSeconds = n

        event.reply {
            embed {
                title("maxmute")
                description { "最大ミュート可能時間を `${event.args}秒` に変更しました。" }
                color(Color.Good)
                timestamp()
            }
        }.queue()
    }
}
