package jp.nephy.glados.features

import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.deleteQueue
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Command
import jp.nephy.glados.core.feature.subscription.CommandChannelType
import jp.nephy.glados.core.feature.subscription.CommandEvent
import jp.nephy.glados.core.feature.subscription.CommandPermission
import jp.nephy.glados.player

class SummonCommand: BotFeature() {
    @Command(permission = CommandPermission.AdminOnly, channelType = CommandChannelType.TextChannel, description = "現在接続中のボイスチャンネルにGLaDOSを呼びます。")
    fun summon(event: CommandEvent) {
        if (event.member == null) {
            return
        }
        val guildPlayer = event.guild?.player ?: return

        if (event.member.voiceState?.channel == guildPlayer.currentVoiceChannel) {
            return event.reply {
                embed {
                    title("コマンドエラー: !summon")
                    description { "既に同じボイスチャンネルに参加しています。" }
                    color(Color.Bad)
                    timestamp()
                }
            }.deleteQueue(30)
        }

        guildPlayer.joinVoiceChannel(event.member.voiceState.channel)
    }
}
