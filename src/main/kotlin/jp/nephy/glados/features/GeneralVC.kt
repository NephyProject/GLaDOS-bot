package jp.nephy.glados.features

import jp.nephy.glados.config
import jp.nephy.glados.core.addRole
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Listener
import jp.nephy.glados.core.feature.subscription.Pool
import jp.nephy.glados.core.isBotOrSelfUser
import jp.nephy.glados.core.removeRole
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import java.util.concurrent.TimeUnit

class GeneralVC: BotFeature() {
    private val roleEnabledGuilds = mutableMapOf<Guild, Role>()

    @Listener
    override fun onReady(event: ReadyEvent) {
        event.jda.guilds.forEach {
            val inVoiceChannelRole = config.forGuild(it)?.role("in_voice_channel") ?: return@forEach
            roleEnabledGuilds.putIfAbsent(it, inVoiceChannelRole)
        }
    }

    @Pool(10, TimeUnit.SECONDS)
    fun checkRole() {
        // 10秒おきにロールが適切かを調べる
        try {
            roleEnabledGuilds.keys.forEach { guild ->
                for (member in guild.members) {
                    if (!member.user.isBotOrSelfUser && member.voiceState.inVoiceChannel() && member.voiceState.channel != guild.afkChannel) {
                        member.addInVoiceChannelRole()
                    } else {
                        member.removeInVoiceChannelRole()
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "ロールの整理に失敗しました." }
        }
    }

    private fun Member.addInVoiceChannelRole() {
        if (user.isBotOrSelfUser) {
            return
        }

        addRole(roleEnabledGuilds[guild] ?: return)
    }

    private fun Member.removeInVoiceChannelRole() {
        if (user.isBotOrSelfUser) {
            return
        }

        removeRole(roleEnabledGuilds[guild] ?: return)
    }

    // ボイスチャンネルに参加した場合
    @Listener
    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        if (event.member.voiceState.channel != event.guild.afkChannel) {
            event.member.addInVoiceChannelRole()
        }
    }

    // ボイスチャンネルから退出した場合
    @Listener
    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        event.member.removeInVoiceChannelRole()
    }

    @Listener
    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        if (event.channelLeft == event.guild.afkChannel) {
            // AFKチャンネルから移動してきた場合
            event.member.addInVoiceChannelRole()
        } else if (event.channelJoined == event.guild.afkChannel) {
            // AFKチャンネルに移動した場合
            event.member.removeInVoiceChannelRole()
        }
    }
}
