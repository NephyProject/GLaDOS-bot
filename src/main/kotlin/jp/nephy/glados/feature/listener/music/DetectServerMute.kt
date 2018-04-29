package jp.nephy.glados.feature.listener.music

import jp.nephy.glados.GLaDOS
import jp.nephy.glados.component.helper.Color
import jp.nephy.glados.component.helper.deleteQueue
import jp.nephy.glados.component.helper.embedMessage
import jp.nephy.glados.component.helper.isSelf
import jp.nephy.glados.feature.ListenerFeature
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceGuildMuteEvent
import java.util.concurrent.TimeUnit

class DetectServerMute: ListenerFeature() {
    override fun onGuildVoiceGuildMute(event: GuildVoiceGuildMuteEvent) {
        if (! bot.config.getGuildConfig(event.guild).option.disableServerMute) {
            return
        }

        if (event.member.user.isSelf && event.isGuildMuted) {
            val guildPlayer = bot.playerManager.getGuildPlayer(event.guild)
            if (guildPlayer.controls.isPlaying) {
                if (guildPlayer.config.textChannel.bot != null) {
                    event.jda.getTextChannelById(guildPlayer.config.textChannel.bot).embedMessage {
                        title("エラー")
                        description { "GLaDOSのミュートは推奨されません。代わりにプレイヤーをミュートします。" }
                        color(Color.Bad)
                    }.deleteQueue(30, TimeUnit.SECONDS)
                }

                guildPlayer.controls.mute()
            }

            if (event.member.hasPermission(Permission.VOICE_MUTE_OTHERS)) {
                event.guild.controller.setMute(event.member, false).queue()
            }
        }
    }
}
