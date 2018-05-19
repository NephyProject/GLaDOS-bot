package jp.nephy.glados.feature.listener.kaigen

import jp.nephy.glados.component.helper.addRole
import jp.nephy.glados.component.helper.putIfNotNull
import jp.nephy.glados.component.helper.removeRole
import jp.nephy.glados.feature.ListenerFeature
import jp.nephy.glados.logger
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceSelfMuteEvent
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


class GeneralVC: ListenerFeature() {
    private val inVoiceChannelroleEnabledGuilds = mutableMapOf<Guild, Role>()
    private val isNotMutingroleEnabledGuilds = mutableMapOf<Guild, Role>()

    override fun onReady(event: ReadyEvent) {
        event.jda.guilds.forEach {
            val config = bot.config.getGuildConfig(it)

            if (config.role.inVoiceChannel != null) {
                inVoiceChannelroleEnabledGuilds.putIfNotNull(it, it.getRoleById(config.role.inVoiceChannel))
            }
            if (config.role.isNotMuting != null) {
                isNotMutingroleEnabledGuilds.putIfNotNull(it, it.getRoleById(config.role.isNotMuting))
            }
        }

        // 10秒おきにロールが適切かを調べるスレッド
        thread {
            while (true) {
                try {
                    inVoiceChannelroleEnabledGuilds.keys.forEach { guild ->
                        for (member in guild.members) {
                            if (! member.user.isBot && member.voiceState.inVoiceChannel() && member.voiceState.channel != guild.afkChannel) {
                                member.addInVoiceChannelRole()
                            } else {
                                member.removeInVoiceChannelRole()
                            }
                        }
                    }
                    isNotMutingroleEnabledGuilds.keys.forEach { guild ->
                        for (member in guild.members) {
                            if (! member.user.isBot && member.voiceState.inVoiceChannel() && member.voiceState.channel != guild.afkChannel && ! member.voiceState.isSelfMuted) {
                                member.addIsNotMutingRole()
                            } else {
                                member.removeIsNotMutingRole()
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "ロールの整理に失敗しました." }
                }

                TimeUnit.SECONDS.sleep(10)
            }
        }
    }

    private fun Member.addInVoiceChannelRole() {
        if (user.isBot) {
            return
        }
        addRole(inVoiceChannelroleEnabledGuilds[guild] ?: return)
    }
    private fun Member.removeInVoiceChannelRole() {
        if (user.isBot) {
            return
        }
        removeRole(inVoiceChannelroleEnabledGuilds[guild] ?: return)
    }
    private fun Member.addIsNotMutingRole() {
        if (user.isBot) {
            return
        }
        addRole(isNotMutingroleEnabledGuilds[guild] ?: return)
    }
    private fun Member.removeIsNotMutingRole() {
        if (user.isBot) {
            return
        }
        removeRole(isNotMutingroleEnabledGuilds[guild] ?: return)
    }

    // ボイスチャンネルに参加した場合
    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        if (event.member.voiceState.channel != event.guild.afkChannel) {
            event.member.addInVoiceChannelRole()
            if (! event.member.voiceState.isSelfMuted) {
                event.member.addIsNotMutingRole()
            }
        }
    }

    // ボイスチャンネルから退出した場合
    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        event.member.removeInVoiceChannelRole()
        event.member.removeIsNotMutingRole()
    }

    override fun onGuildVoiceSelfMute(event: GuildVoiceSelfMuteEvent) {
        if (! event.isSelfMuted) {
            // マイクミュートを解除した場合
            event.member.addIsNotMutingRole()
        } else if (event.isSelfMuted && event.member.voiceState.channel != event.guild.afkChannel) {
            // マイクミュートした場合 (AFKチャンネル以外)
            event.member.removeIsNotMutingRole()
        }
    }

    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        if (event.channelLeft == event.guild.afkChannel) {
            // AFKチャンネルから移動してきた場合
            event.member.addInVoiceChannelRole()
            if (! event.member.voiceState.isSelfMuted) {
                event.member.addIsNotMutingRole()
            }
        } else if (event.channelJoined == event.guild.afkChannel) {
            // AFKチャンネルに移動した場合
            event.member.removeInVoiceChannelRole()
            event.member.removeIsNotMutingRole()
        }
    }
}
