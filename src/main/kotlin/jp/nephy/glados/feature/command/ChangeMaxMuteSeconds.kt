package jp.nephy.glados.feature.command

import com.jagrosh.jdautilities.command.CommandEvent
import jp.nephy.glados.component.helper.Color
import jp.nephy.glados.component.helper.embedMention
import jp.nephy.glados.feature.CommandFeature
import jp.nephy.glados.feature.listener.kaigen.MuteIsAFK

class ChangeMaxMuteSeconds: CommandFeature() {
    init {
        name = "maxmute"
        help = "[Mute is AFK]チャンネルで適用する最大のミュート時間を指定します。"

        isAdminCommand = true
        arguments = "<秒>"
    }

    override fun executeCommand(event: CommandEvent) {
        MuteIsAFK.maxMuteSeconds = event.args.toIntOrNull() ?: return

        event.embedMention {
            title("maxmute")
            description { "最大ミュート可能時間を `${event.args}秒` に変更しました." }
            color(Color.Good)
            timestamp()
        }.queue()
    }
}
