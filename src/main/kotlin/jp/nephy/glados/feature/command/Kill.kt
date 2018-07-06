package jp.nephy.glados.feature.command

import com.jagrosh.jdautilities.command.CommandEvent
import jp.nephy.glados.feature.CommandFeature

class Kill: CommandFeature() {
    init {
        name = "kill"
        help = "陰キャを殺します。"
        guildOnly = true
    }

    override fun executeCommand(event: CommandEvent) {
        event.reply("Searching...") {
            event.guild.voiceChannels.filter { it.members.isNotEmpty() }.forEach {
                it.members.filter { it.voiceState.isSelfMuted }.forEach {
                    event.guild.controller.moveVoiceMember(it, event.guild.afkChannel).queue()
                }
            }

            it.editMessage("Done.").queue()
        }
    }
}
