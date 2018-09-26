package jp.nephy.glados.features

import jp.nephy.glados.config
import jp.nephy.glados.core.addRole
import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.deleteQueue
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.*
import jp.nephy.glados.core.feature.subscription.*
import jp.nephy.glados.core.fullName
import jp.nephy.glados.core.isBotOrSelfUser
import jp.nephy.glados.core.removeRole
import jp.nephy.utils.IntLinkedSingleCache
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMuteEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class VoiceChannelWatcher: BotFeature() {
    private val inkyaRoles by rolesLazy("inkya")
    private val noMuteVoiceChannels by voiceChannelsLazy("no_mute")
    private val muteLimitVoiceChannels by voiceChannelsLazy("mute_limit")
    private val generalVCTextChannels by textChannelsLazy("general_vc")
    private val invisibleVoiceChannels by voiceChannelsLazy("invisible")
    private val invisibleTextChannels by textChannelsLazy("invisible")

    private val noMuteMovedWarningCooldowns = ConcurrentHashMap<Member, Long>()
    private val muteLimitCountdowns = ConcurrentHashMap<Member, Int>()
    private var maxMuteSeconds by IntLinkedSingleCache { 5 * 60 }

    /*
        AFK以外のボイスチャンネルに参加した場合       -> GeneralVC権限付与
                       ミュートしていない場合       -> Invisible権限付与
                       ミュートしている場合         -> Inkyaロール付与
        [No Mute]にミュートした状態で参加した場合    -> AFKチャンネルへ移動
        [Mute Limit]にミュートした状態で参加した場合 -> カウントダウン開始

        AFKチャンネルから移動してきた場合            -> GeneralVC権限付与
                       ミュートしていない場合       -> Invisible権限付与
                       ミュートしている場合         -> Inkyaロール付与
        [No Mute]にミュートした状態で移動した場合    -> AFKチャンネルへ移動
        [Mute Limit]にミュートした状態で移動した場合 -> カウントダウン開始

        [Invisible]以外でミュートした場合           -> Invisible権限削除
        AFKチャンネル以外でミュートした場合          -> Inkyaロール付与
                         ミュート解除した場合      -> Inkyaロール削除
        [No Mute]でミュートした場合                -> AFKチャンネルへ移動

        ボイスチャンネルから退出した場合             -> 権限&ロール&カウントダウン削除
        AFKチャンネルに移動した場合                 -> 権限&ロール&カウントダウン削除
     */
    @Loop(5, TimeUnit.SECONDS)
    fun check() {
        val guilds = (inkyaRoles.map { it.guild } + generalVCTextChannels.map { it.guild } + invisibleVoiceChannels.map { it.guild } + invisibleTextChannels.map { it.guild }).toSet()
        for (guild in guilds) {
            for (member in guild.members) {
                if (member.voiceState.inVoiceChannel() && member.voiceState.channel != guild.afkChannel) {
                    member.addGeneralVCPermission()
                    if (!member.voiceState.isMuted) {
                        member.addInvisiblePermissions()
                    } else {
                        member.addInkyaRole()
                    }
                } else {
                    member.removeGeneralVCPermission()
                    member.removeInvisiblePermissions()
                    member.removeInkyaRole()
                }
            }
        }

        for ((member, elapsedSeconds) in muteLimitCountdowns.toMap()) {
            if (maxMuteSeconds <= elapsedSeconds) {
                if (member.voiceState.channel !in muteLimitVoiceChannels) {
                    member.stopMuting()
                    continue
                }

                if (member.guild.afkChannel == null) {
                    logger.warn { "AFKチャンネルが未定義のため, メンバーを移動できませんでした. (${member.guild.name})" }
                    continue
                }

                member.guild.controller.moveVoiceMember(member, member.guild.afkChannel).queue()
                member.stopMuting()
                continue
            }

            muteLimitCountdowns.replace(member, elapsedSeconds + 5)
            logger.debug { "${member.fullName} は ${elapsedSeconds + 5}秒間ミュートしています." }
        }
    }

    private val maxMuteMininumSeconds = 10
    @Command(permission = CommandPermission.AdminOnly, description = "[Mute Limit]チャンネルで適用する最大のミュート時間を指定します。", args = "<秒>")
    fun maxmute(event: CommandEvent) {
        val n = event.args.toIntOrNull()
        if (n == null || n < maxMuteMininumSeconds) {
            return event.reply {
                embed {
                    title("maxmute")
                    description { "不正な値です。maxmuteは${maxMuteMininumSeconds}以上の整数を指定できます。" }
                    color(Color.Bad)
                    timestamp()
                }
            }.queue()
        }

        maxMuteSeconds = n

        event.reply {
            embed {
                title("maxmute")
                description { "最大ミュート可能時間を `${event.args}秒` に変更しました。" }
                color(Color.Good)
                timestamp()
            }
        }.queue()
    }

    @Listener
    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        if (event.voiceState.channel != event.guild.afkChannel) {
            event.member.addGeneralVCPermission()
            if (!event.voiceState.isMuted) {
                event.member.addInvisiblePermissions()
            } else {
                event.member.addInkyaRole()
                if (event.voiceState.channel in noMuteVoiceChannels) {
                    event.member.moveToAFKChannel()
                } else if (event.voiceState.channel in muteLimitVoiceChannels) {
                    event.member.startMuting()
                }
            }
        }
    }

    @Listener
    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        event.member.removeGeneralVCPermission()
        event.member.removeInvisiblePermissions()
        event.member.removeInkyaRole()
        event.member.stopMuting()
    }

    @Listener
    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        if (event.channelLeft == event.guild.afkChannel) {
            event.member.addGeneralVCPermission()
            if (!event.voiceState.isMuted) {
                event.member.addInvisiblePermissions()
            } else {
                event.member.addInkyaRole()
            }
        } else if (event.channelJoined == event.guild.afkChannel) {
            event.member.removeGeneralVCPermission()
            event.member.removeInvisiblePermissions()
            event.member.removeInkyaRole()
            event.member.stopMuting()
        }

        if (event.channelJoined in noMuteVoiceChannels && event.voiceState.isMuted) {
            event.member.moveToAFKChannel()
        }
        if (event.channelJoined in muteLimitVoiceChannels && event.voiceState.isMuted) {
            event.member.startMuting()
        }
    }

    @Listener
    override fun onGuildVoiceMute(event: GuildVoiceMuteEvent) {
        if (event.voiceState.channel !in invisibleVoiceChannels && event.isMuted) {
            event.member.removeInvisiblePermissions()
        }

        if (!event.isMuted) {
            event.member.removeInkyaRole()
        } else if (event.voiceState.channel != event.guild.afkChannel) {
            event.member.addInkyaRole()
        }

        if (event.voiceState.channel in noMuteVoiceChannels && event.isMuted) {
            event.member.moveToAFKChannel()
        }
        if (event.voiceState.channel in muteLimitVoiceChannels) {
            if (event.isMuted) {
                event.member.startMuting()
            } else {
                event.member.stopMuting()
            }
        }
    }

    private fun Member.addInkyaRole() {
        if (user.isBotOrSelfUser) {
            return
        }

        inkyaRoles.roleOf(guild) {
            addRole(it)
        }
    }

    private fun Member.removeInkyaRole() {
        if (user.isBotOrSelfUser) {
            return
        }

        inkyaRoles.roleOf(guild) {
            removeRole(it)
        }
    }

    private fun Member.moveToAFKChannel() {
        if (user.isBotOrSelfUser) {
            return
        }

        if (guild.afkChannel == null) {
            logger.warn { "AFKチャンネルが未定義のため, メンバーの移動ができませんでした. (${guild.name})" }
            return
        }

        noMuteVoiceChannels.voiceChannelOf(guild) {
            val botChannel = config.forGuild(guild)?.textChannel("bot")
            if (botChannel != null) {
                if (checkNoMuteMovedWarningCooldown()) {
                    botChannel.reply(this) {
                        embed {
                            title("⚠️ チャンネル規制")
                            descriptionBuilder {
                                appendln("ボイスチャンネル `${it.name}` ではミュートが禁止されています。")
                                append("ご迷惑をおかけしますが AFKチャンネルに移動しました。")
                            }
                            color(Color.Bad)
                            timestamp()
                        }
                    }.deleteQueue(30)
                }
                updateNoMuteMovedWarningCooldown()
            }

            guild.controller.moveVoiceMember(this, guild.afkChannel).queue()
        }
    }

    private fun Member.updateNoMuteMovedWarningCooldown() {
        noMuteMovedWarningCooldowns[this] = Date().time
    }

    private val noMuteMovedWarningThresholdSeconds = 10
    private fun Member.checkNoMuteMovedWarningCooldown(): Boolean {
        val time = noMuteMovedWarningCooldowns[this] ?: return true
        return Date().time - time > 1000 * noMuteMovedWarningThresholdSeconds
    }

    private fun Member.startMuting() {
        if (user.isBotOrSelfUser) {
            return
        }

        muteLimitCountdowns.putIfAbsent(this, 0)
    }

    private fun Member.stopMuting() {
        if (user.isBotOrSelfUser) {
            return
        }

        muteLimitCountdowns.remove(this)
    }

    private fun Member.addGeneralVCPermission() {
        if (user.isBotOrSelfUser) {
            return
        }

        generalVCTextChannels.textChannelOf(guild) {
            if (it.getPermissionOverride(this) == null) {
                it.createPermissionOverride(this)
                        .setAllow(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_TTS, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_HISTORY, Permission.MESSAGE_MENTION_EVERYONE, Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_ADD_REACTION)
                        .setDeny(Permission.CREATE_INSTANT_INVITE, Permission.MANAGE_CHANNEL, Permission.MANAGE_ROLES, Permission.MANAGE_WEBHOOKS, Permission.MESSAGE_MANAGE)
                        .queue()
            }
        }
    }

    private fun Member.removeGeneralVCPermission() {
        if (user.isBotOrSelfUser) {
            return
        }

        generalVCTextChannels.textChannelOf(guild) {
            it.getPermissionOverride(this)?.delete()?.queue()
        }
    }

    private fun Member.addInvisiblePermissions() {
        if (user.isBotOrSelfUser) {
            return
        }

        invisibleTextChannels.textChannelOf(guild) {
            if (it.getPermissionOverride(this) == null) {
                it.createPermissionOverride(this)
                        .setAllow(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_TTS, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_MENTION_EVERYONE, Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_ADD_REACTION)
                        .setDeny(Permission.CREATE_INSTANT_INVITE, Permission.MANAGE_CHANNEL, Permission.MANAGE_ROLES, Permission.MANAGE_WEBHOOKS, Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY)
                        .queue()
            }
        }
        invisibleVoiceChannels.voiceChannelOf(guild) {
            if (it.getPermissionOverride(this) == null) {
                it.createPermissionOverride(this)
                        .setAllow(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.VOICE_USE_VAD)
                        .setDeny(Permission.CREATE_INSTANT_INVITE, Permission.MANAGE_CHANNEL, Permission.MANAGE_ROLES, Permission.MANAGE_WEBHOOKS, Permission.VOICE_MUTE_OTHERS, Permission.VOICE_DEAF_OTHERS, Permission.VOICE_MOVE_OTHERS, Permission.PRIORITY_SPEAKER)
                        .queue()
            }
        }
    }

    private fun Member.removeInvisiblePermissions() {
        if (user.isBotOrSelfUser) {
            return
        }

        invisibleTextChannels.textChannelOf(guild) {
            it.getPermissionOverride(this)?.delete()?.queue()
        }
        invisibleVoiceChannels.voiceChannelOf(guild) {
            it.getPermissionOverride(this)?.delete()?.queue()
        }
    }
}
