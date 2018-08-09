package jp.nephy.glados.features

import jp.nephy.glados.config
import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.deleteQueue
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Listener
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceSelfMuteEvent
import java.util.*

class NoMuteChannel: BotFeature() {
    private val cooldowns = mutableMapOf<Long, Long>()

    @Listener
    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        if (event.member.voiceState.isMuted) {
            move(event.member, event.channelJoined, event.guild)
        }
    }

    @Listener
    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        if (event.member.voiceState.isMuted) {
            move(event.member, event.channelJoined, event.guild)
        }
    }

    @Listener
    override fun onGuildVoiceSelfMute(event: GuildVoiceSelfMuteEvent) {
        if (event.isSelfMuted) {
            move(event.member, event.voiceState.channel, event.guild)
        }
    }

    private fun move(member: Member, voiceChannel: VoiceChannel, guild: Guild) {
        val guildConfig = config.forGuild(guild) ?: return
        val noMuteChannel = guildConfig.voiceChannel("no_mute")
        if (noMuteChannel == null || guild.afkChannel == null || voiceChannel.idLong != noMuteChannel.idLong) {
            return
        }
        val botChannel = guildConfig.textChannel("bot")

        if (botChannel != null) {
            if (member.checkCooldown()) {
                botChannel.reply(member) {
                    embed {
                        title("⚠️ チャンネル規制")
                        descriptionBuilder {
                            appendln("ボイスチャンネル `${voiceChannel.name}` ではミュートが禁止されています。")
                            append("ご迷惑をおかけしますが AFKチャンネルに移動しました。")
                        }
                        color(Color.Bad)
                        timestamp()
                    }
                }.deleteQueue(30)
            }
        }

        guild.controller.moveVoiceMember(member, guild.afkChannel).queue()
        member.updateCooldown()
    }

    private fun Member.updateCooldown() {
        cooldowns[user.idLong] = Date().time
    }

    private fun Member.checkCooldown(): Boolean {
        val time = cooldowns[user.idLong] ?: return true
        return Date().time - time > 1000 * 10
    }
}
