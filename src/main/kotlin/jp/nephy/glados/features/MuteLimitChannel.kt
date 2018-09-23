package jp.nephy.glados.features

import jp.nephy.glados.config
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Listener
import jp.nephy.glados.core.feature.subscription.Loop
import jp.nephy.glados.core.fullName
import jp.nephy.glados.core.isBotOrSelfUser
import jp.nephy.utils.IntLinkedSingleCache
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceSelfMuteEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class MuteLimitChannel: BotFeature() {
    companion object {
        var maxMuteSeconds by IntLinkedSingleCache { 5 * 60 }
    }

    private val enabledGuilds = ConcurrentHashMap<Guild, VoiceChannel>()

    @Listener
    override fun onReady(event: ReadyEvent) {
        event.jda.guilds.forEach {
            val muteLimitChannel = config.forGuild(it)?.voiceChannel("mute_limit") ?: return@forEach
            enabledGuilds.putIfAbsent(it, muteLimitChannel)
        }
    }

    @Loop(5, TimeUnit.SECONDS)
    fun checkInkya() {
        try {
            muteMembers.toMap().forEach {
                muteMembers[it.key] = (muteMembers[it.key] ?: return@forEach) + 5

                if (muteMembers[it.key]!! >= maxMuteSeconds) {
                    it.key.guild.controller.moveVoiceMember(it.key, it.key.guild.afkChannel).queue { _ ->
                        it.key.stopMuting()
                    }
                }

                logger.debug { "${it.key.fullName} は ${muteMembers[it.key]}秒ミュートしています." }
            }
        } catch (e: Exception) {
            logger.error(e) { "ミュートメンバーの整理に失敗しました." }
        }
    }

    private val muteMembers = ConcurrentHashMap<Member, Int>()
    private fun Member.startMuting() {
        if (user.isBotOrSelfUser) {
            return
        }

        muteMembers[this] = 0
    }

    private fun Member.stopMuting() {
        if (user.isBotOrSelfUser) {
            return
        }

        muteMembers.remove(this)
    }

    // ボイスチャンネルに参加した場合
    @Listener
    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        if (event.guild !in enabledGuilds) {
            return
        }

        if (enabledGuilds[event.guild] == event.channelJoined && event.voiceState.isSelfMuted) {
            event.member.startMuting()
        }
    }

    @Listener
    override fun onGuildVoiceSelfMute(event: GuildVoiceSelfMuteEvent) {
        if (enabledGuilds[event.guild] != event.voiceState.channel) {
            return
        }

        if (!event.isSelfMuted) {
            // マイクミュートを解除した場合
            event.member.stopMuting()
        } else if (event.isSelfMuted) {
            // マイクミュートした場合
            event.member.startMuting()
        }
    }

    @Listener
    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        if (event.guild !in enabledGuilds) {
            return
        }

        if (enabledGuilds[event.guild] == event.channelJoined && event.voiceState.isSelfMuted) {
            event.member.startMuting()
        }
    }

    @Listener
    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        event.member.stopMuting()
    }
}
