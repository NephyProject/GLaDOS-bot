package jp.nephy.glados.features

import jp.nephy.glados.config
import jp.nephy.glados.core.addRole
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Listener
import jp.nephy.glados.core.feature.subscription.Pool
import jp.nephy.glados.core.removeRole
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.core.events.guild.voice.*
import java.util.concurrent.TimeUnit

class InkyaHunter: BotFeature() {
    private val enabledGuilds = mutableMapOf<Guild, Role>()

    @Listener
    override fun onReady(event: ReadyEvent) {
        event.jda.guilds.forEach {
            enabledGuilds[it] = config.forGuild(it)?.role("inkya") ?: return@forEach
        }
    }

    @Pool(10, TimeUnit.SECONDS)
    fun checkRole() {
        // 10秒おきに陰キャロールが適切かを調べる
        try {
            enabledGuilds.keys.forEach { guild ->
                guild.members.forEach {
                    if (it.voiceState.inVoiceChannel() && it.voiceState.channel != guild.afkChannel && (it.voiceState.isSelfMuted || it.voiceState.isGuildMuted)) {
                        it.addInkyaRole()
                    } else {
                        it.removeInkyaRole()
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "陰キャロールの整理に失敗しました." }
        }
    }

    private fun Member.addInkyaRole() {
        addRole(enabledGuilds[guild] ?: return)
    }

    private fun Member.removeInkyaRole() {
        removeRole(enabledGuilds[guild] ?: return)
    }

    // 条件を満たしていないのにロールが外れた場合のチェック
    @Listener
    override fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {
        val inkyaRole = config.forGuild(event.guild)?.role("inkya") ?: return

        event.roles.filter { it.idLong == inkyaRole.idLong }.forEach {
            if (event.member.voiceState.inVoiceChannel() && event.member.voiceState.channel != event.guild.afkChannel && (event.member.voiceState.isSelfMuted || event.member.voiceState.isGuildMuted)) {
                event.member.addInkyaRole()
            }
        }
    }

    // VCから切断した場合
    @Listener
    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        event.member.removeInkyaRole()
    }

    @Listener
    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        if (event.channelJoined == event.guild.afkChannel) {
            // AFKチャンネルに移動した場合
            event.member.removeInkyaRole()
        } else if (event.channelLeft == event.guild.afkChannel && (event.member.voiceState.isSelfMuted || event.member.voiceState.isGuildMuted)) {
            // マイク/サーバミュート状態でAFKチャンネルから移動した場合
            event.member.addInkyaRole()
        }
    }

    @Listener
    override fun onGuildVoiceSelfMute(event: GuildVoiceSelfMuteEvent) {
        if (!event.isSelfMuted) {
            // マイクミュートを解除した場合
            event.member.removeInkyaRole()
        } else if (event.isSelfMuted && event.member.voiceState.channel != event.guild.afkChannel) {
            // マイクミュートした場合 (AFKチャンネル以外)
            event.member.addInkyaRole()
        }
    }

    @Listener
    override fun onGuildVoiceGuildMute(event: GuildVoiceGuildMuteEvent) {
        if (!event.isGuildMuted) {
            // サーバミュートを解除した場合
            event.member.removeInkyaRole()
        } else if (event.isGuildMuted && event.member.voiceState.channel != event.guild.afkChannel) {
            // サーバミュートした場合 (AFKチャンネル以外)
            event.member.addInkyaRole()
        }
    }

    @Listener
    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        // マイク/サーバミュート状態でVCに参加した場合 (AFKチャンネル以外)
        if ((event.member.voiceState.isSelfMuted || event.member.voiceState.isGuildMuted) && event.member.voiceState.channel != event.guild.afkChannel) {
            event.member.addInkyaRole()
        }
    }
}
