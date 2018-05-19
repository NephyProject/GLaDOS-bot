package jp.nephy.glados.feature.listener.kaigen

import jp.nephy.glados.component.helper.addRole
import jp.nephy.glados.component.helper.removeRole
import jp.nephy.glados.feature.ListenerFeature
import jp.nephy.glados.logger
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceSelfMuteEvent
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


class Inkya: ListenerFeature() {
    private val enabledGuilds = mutableMapOf<Guild, Role>()

    override fun onReady(event: ReadyEvent) {
        event.jda.guilds.forEach {
            val config = bot.config.getGuildConfig(it)
            if (config.role.inkya == null) {
                return@forEach
            }

            enabledGuilds[it] = it.getRoleById(config.role.inkya) ?: return@forEach
        }

        // 10秒おきに陰キャロールが適切かを調べるスレッド
        thread {
            while (true) {
                try {
                    enabledGuilds.keys.forEach { guild ->
                        guild.members.forEach {
                            if (it.voiceState.inVoiceChannel() && it.voiceState.channel != guild.afkChannel && it.voiceState.isSelfMuted) {
                                it.addInkyaRole()
                            } else {
                                it.removeInkyaRole()
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "陰キャロールの整理に失敗しました." }
                }

                TimeUnit.SECONDS.sleep(10)
            }
        }
    }

    private fun Member.addInkyaRole() {
        addRole(enabledGuilds[guild] ?: return)
    }

    private fun Member.removeInkyaRole() {
        removeRole(enabledGuilds[guild] ?: return)
    }

    // 条件を満たしていないのにロールが外れた場合のチェック
    override fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {
        val config = bot.config.getGuildConfig(event.guild)
        if (config.role.inkya == null) {
            return
        }

        event.roles.filter { it.idLong == config.role.inkya }.forEach {
            if (event.member.voiceState.inVoiceChannel() && event.member.voiceState.channel != event.guild.afkChannel && event.member.voiceState.isSelfMuted) {
                event.member.addInkyaRole()
            }
        }
    }

    // VCから切断した場合
    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        event.member.removeInkyaRole()
    }

    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        if (event.channelJoined == event.guild.afkChannel) {
            // AFKチャンネルに移動した場合
            event.member.removeInkyaRole()
        } else if (event.channelLeft == event.guild.afkChannel && event.member.voiceState.isSelfMuted) {
            // マイクミュート状態でAFKチャンネルから移動した場合
            event.member.addInkyaRole()
        }
    }

    override fun onGuildVoiceSelfMute(event: GuildVoiceSelfMuteEvent) {
        if (! event.isSelfMuted) {
            // マイクミュートを解除した場合
            event.member.removeInkyaRole()
        } else if (event.isSelfMuted && event.member.voiceState.channel != event.guild.afkChannel) {
            // マイクミュートした場合 (AFKチャンネル以外)
            event.member.addInkyaRole()
        }
    }

    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        // マイクミュート状態でVCに参加した場合 (AFKチャンネル以外)
        if (event.member.voiceState.isSelfMuted && event.member.voiceState.channel != event.guild.afkChannel) {
            event.member.addInkyaRole()
        }
    }
}
