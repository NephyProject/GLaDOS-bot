package jp.nephy.glados.features

import jp.nephy.glados.core.builder.edit
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Command
import jp.nephy.glados.core.feature.subscription.CommandChannelType
import jp.nephy.glados.core.feature.subscription.CommandEvent
import jp.nephy.glados.core.feature.subscription.CommandPermission

class KillCommand: BotFeature() {
    @Command(permission = CommandPermission.AdminOnly, channelType = CommandChannelType.TextChannel, description = "陰キャを殺します。")
    fun kill(event: CommandEvent) {
        event.reply {
            message {
                append("Searching...")
            }
        }.queue {
            event.guild!!.voiceChannels.filter { it.members.isNotEmpty() }.forEach {
                it.members.filter { it.voiceState.isSelfMuted }.forEach {
                    event.guild.controller.moveVoiceMember(it, event.guild.afkChannel).queue()
                }
            }

            it.edit {
                message {
                    append("Done.")
                }
            }.queue()
        }
    }
}
