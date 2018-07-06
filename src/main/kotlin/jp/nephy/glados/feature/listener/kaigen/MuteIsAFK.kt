package jp.nephy.glados.feature.listener.kaigen

import jp.nephy.glados.component.helper.fullName
import jp.nephy.glados.component.helper.putIfNotNull
import jp.nephy.glados.feature.ListenerFeature
import jp.nephy.glados.logger
import jp.nephy.utils.IntLinkedSingleCache
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceSelfMuteEvent
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


class MuteIsAFK: ListenerFeature() {
    companion object {
        var maxMuteSeconds by IntLinkedSingleCache { 5 * 60 }
    }

    private val enabledGuilds = mutableMapOf<Guild, VoiceChannel>()

    override fun onReady(event: ReadyEvent) {
        event.jda.guilds.forEach {
            val config = bot.config.getGuildConfig(it)

            if (config.voiceChannel.muteIsAFK != null) {
                enabledGuilds.putIfNotNull(it, it.getVoiceChannelById(config.voiceChannel.muteIsAFK))

                it.voiceChannels.filter { it.members.isNotEmpty() }.forEach {
                    it.members.filter { it.voiceState.isSelfMuted }.forEach {
                        it.startMuting()
                    }
                }
            }
        }

        thread {
            while (true) {
                try {
                    muteMembers.toMap().forEach {
                        muteMembers[it.key] = (muteMembers[it.key] ?: return@forEach) + 5

                        if (muteMembers[it.key]!! >= maxMuteSeconds) {
                            it.key.guild.controller.moveVoiceMember(it.key, it.key.guild.afkChannel).queue()
                        }

                        logger.debug { "${it.key.fullName} は ${muteMembers[it.key]}秒ミュートしています." }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "ミュートメンバーの整理に失敗しました." }
                }

                TimeUnit.SECONDS.sleep(5)
            }
        }
    }

    private val muteMembers = mutableMapOf<Member, Int>()
    private fun Member.startMuting() {
        if (user.isBot) {
            return
        }
        muteMembers[this] = 0
    }
    private fun Member.stopMuting() {
        if (user.isBot) {
            return
        }
        muteMembers.remove(this)
    }

    // ボイスチャンネルに参加した場合
    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        if (event.guild !in enabledGuilds) {
            return
        }

        // AFKチャンネルに参加した場合
        if (event.channelJoined == event.guild.afkChannel) {
            return event.member.stopMuting()
        }

        if (enabledGuilds[event.guild] == event.channelJoined && event.voiceState.isSelfMuted) {
            event.member.startMuting()
        }
    }

    override fun onGuildVoiceSelfMute(event: GuildVoiceSelfMuteEvent) {
        if (enabledGuilds[event.guild] != event.voiceState.channel) {
            return
        }

        if (! event.isSelfMuted) {
            // マイクミュートを解除した場合
            event.member.stopMuting()
        } else if (event.isSelfMuted) {
            // マイクミュートした場合 (AFKチャンネル以外)
            event.member.startMuting()
        }
    }

    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        if (event.guild !in enabledGuilds) {
            return
        }

        // AFKチャンネルに移動した場合
        if (event.channelJoined == event.guild.afkChannel) {
            return event.member.stopMuting()
        }

        if (enabledGuilds[event.guild] == event.channelJoined && event.voiceState.isSelfMuted) {
            event.member.startMuting()
        }
    }
}
