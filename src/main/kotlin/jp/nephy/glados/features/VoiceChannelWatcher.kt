package jp.nephy.glados.features

import jp.nephy.glados.config
import jp.nephy.glados.core.*
import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.*
import jp.nephy.glados.core.feature.subscription.Command
import jp.nephy.glados.core.feature.subscription.CommandEvent
import jp.nephy.glados.core.feature.subscription.Event
import jp.nephy.glados.core.feature.subscription.Loop
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMuteEvent
import net.dv8tion.jda.core.events.user.update.UserUpdateGameEvent
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
    private val vrcVoiceChannels by voiceChannelsLazy("vrchat")

    private val muteLimitCountdowns = ConcurrentHashMap<Member, Long>()
    private val noMuteMovedWarningCooldowns by MongoUserConfigStore("GLaDOSNoMuteMovedWarningCooldown", false) { 0L }

    /*
        AFK以外のボイスチャンネルに参加した場合       -> GeneralVC権限付与
                       ミュートしていない場合       -> Invisible権限付与
                       ミュートしている場合         -> Inkyaロール付与
        [No Mute]にミュートした状態で参加した場合    -> AFKチャンネルへ移動
        [Mute Limit]にミュートした状態で参加した場合 -> カウントダウン開始
        [Invisible]に参加した場合                  -> テキストチャンネルの権限を付与

        AFKチャンネルから移動してきた場合            -> GeneralVC権限付与
                       ミュートしていない場合       -> Invisible権限付与
                       ミュートしている場合         -> Inkyaロール付与
        [No Mute]にミュートした状態で移動した場合    -> AFKチャンネルへ移動
        [Mute Limit]にミュートした状態で移動した場合 -> カウントダウン開始
        [Invisible]に移動した場合                  -> テキストチャンネルの権限を付与

        [Invisible]以外でミュートした場合           -> Invisible権限削除
        AFKチャンネル以外でミュートした場合          -> Inkyaロール付与
                         ミュート解除した場合      -> Inkyaロール削除
        [No Mute]でミュートした場合                -> AFKチャンネルへ移動

        ボイスチャンネルから退出した場合             -> 権限&ロール&カウントダウン削除
        AFKチャンネルに移動した場合                 -> 権限&ロール&カウントダウン削除
        [Invisible]から退出した場合                -> テキストチャンネルの権限を削除

        [Invisible]が空になった場合                -> ロックを解除
     */
    @Loop(5, TimeUnit.SECONDS)
    fun check() {
        val guilds = (inkyaRoles.map { it.guild } + generalVCTextChannels.map { it.guild } + invisibleVoiceChannels.map { it.guild } + invisibleTextChannels.map { it.guild }).toSet()
        for (guild in guilds) {
            for (member in guild.members) {
                if (member.voiceState.inVoiceChannel() && member.voiceState.channel != guild.afkChannel) {
                    member.addGeneralVCPermission()
                    if (!member.voiceState.isMuted) {
                        member.addInvisibleVoiceChannelPermission()
                        if (member.voiceState.channel in invisibleVoiceChannels) {
                            member.addInvisibleTextChannelPermission()
                        }
                    } else {
                        member.addInkyaRole()
                    }
                } else {
                    member.removeGeneralVCPermission()
                    member.removeInvisibleTextChannelPermission()
                    member.removeInvisibleVoiceChannelPermission()
                    member.removeInkyaRole()
                }

                if (member.game?.name == "VRChat") {
                    member.addVRCVoiceChannelPermission()
                } else {
                    member.removeVRCVoiceChannelPermission()
                }
            }
        }

        for ((member, elapsedSeconds) in muteLimitCountdowns) {
            if (maxMuteSeconds[member.guild] < elapsedSeconds) {
                if (member.voiceState.channel !in muteLimitVoiceChannels) {
                    member.stopMuting()
                    continue
                }

                if (member.guild.afkChannel == null) {
                    logger.warn { "AFKチャンネルが未定義のため, メンバーを移動できませんでした. (${member.guild.name})" }
                    continue
                }

                member.guild.controller.moveVoiceMember(member, member.guild.afkChannel).launch()
                member.stopMuting()
                continue
            }

            muteLimitCountdowns[member] = elapsedSeconds + 5
            logger.debug { "${member.fullName} は ${elapsedSeconds + 5}秒間ミュートしています。" }
        }
    }

    private val maxMuteSeconds by MongoGuildConfigStore("GLaDOSMaxMuteSecond") { 5 * 60 }
    @Command(permission = Command.Permission.AdminOnly, description = "[Mute Limit]チャンネルで適用する最大のミュート時間を指定します。", args = ["秒"], category = "VC Watcher", channelType = Command.ChannelType.TextChannel)
    fun maxmute(event: CommandEvent) {
        val n = event.args.toIntOrNull()
        reject(n == null || n < 10) {
            event.embedError("!maxmute") {
                "不正な値です。maxmuteは10以上の整数を指定できます。"
            }
        }

        maxMuteSeconds[event.guild!!] = n

        event.reply {
            embed {
                title("maxmute")
                description { "最大ミュート可能時間を `${event.args}秒` に変更しました。" }
                color(Color.Good)
                timestamp()
            }
        }.launch()
    }

    private val invisibleChannelLocked by MongoGuildConfigStore("GLaDOSInvisibleChannelState") { false }
    @Command(description = "[Invisible]チャンネルをロックします。", category = "VC Watcher", channelType = Command.ChannelType.TextChannel)
    fun lock(event: CommandEvent) {
        if (event.textChannel !in invisibleTextChannels) {
            return
        }

        if (invisibleChannelLocked[event.guild!!]) {
            event.reply {
                embed {
                    title("lock")
                    descriptionBuilder {
                        appendln("既に ${event.textChannel!!.asMention} はロックされています。")
                        appendln("ロック中は[Invisible] と ${event.textChannel.asMention} は他のメンバーに表示されなくなるため安全に利用できます。")
                        appendln("VCから切断すると参加できなくなるためご注意ください。")
                        append("[Invisible]接続メンバーが0になるか, `!unlock` コマンドでロックを解除できます。")
                    }
                    timestamp()
                }
            }.launchAndDelete(30, TimeUnit.SECONDS)
        } else {
            invisibleChannelLocked[event.guild] = true
            event.reply {
                embed {
                    title("lock")
                    descriptionBuilder {
                        appendln("${event.textChannel!!.asMention} はロックしました。")
                        appendln("ロック中は[Invisible] と ${event.textChannel.asMention} は他のメンバーに表示されなくなるため安全に利用できます。")
                        appendln("VCから切断すると参加できなくなるためご注意ください。")
                        append("[Invisible]接続メンバーが0になるか, `!unlock` コマンドでロックを解除できます。")
                    }
                    timestamp()
                }
            }.launchAndDelete(30, TimeUnit.SECONDS)
        }
    }

    @Command(description = "[Invisible]チャンネルのロックを解除します。", category = "VC Watcher")
    fun unlock(event: CommandEvent) {
        if (event.textChannel !in invisibleTextChannels) {
            return
        }

        if (!invisibleChannelLocked[event.guild!!]) {
            event.reply {
                embed {
                    title("unlock")
                    descriptionBuilder {
                        appendln("既に ${event.textChannel!!.asMention} はロック解除されています。")
                        append("${event.textChannel.asMention} の書き込み内容は接続中に投稿されたメッセージ以外は読むことができないため安全に利用できます。")
                    }
                    timestamp()
                }
            }.launchAndDelete(30, TimeUnit.SECONDS)
        } else {
            invisibleChannelLocked[event.guild] = false
            event.reply {
                embed {
                    title("unlock")
                    descriptionBuilder {
                        appendln("${event.textChannel!!.asMention} はロック解除しました。")
                        append("${event.textChannel.asMention} の書き込み内容は接続中に投稿されたメッセージ以外は読むことができないため安全に利用できます。")
                    }
                    timestamp()
                }
            }.launchAndDelete(30, TimeUnit.SECONDS)
        }
    }

    @Event
    override suspend fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        if (event.voiceState.channel != event.guild.afkChannel) {
            event.member.addGeneralVCPermission()
            if (!event.voiceState.isMuted) {
                event.member.addInvisibleVoiceChannelPermission()
                if (event.member.voiceState.channel in invisibleVoiceChannels) {
                    event.member.addInvisibleTextChannelPermission()
                }
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

    @Event
    override suspend fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        event.member.removeGeneralVCPermission()
        event.member.removeInvisibleTextChannelPermission()
        event.member.removeInvisibleVoiceChannelPermission()
        event.member.removeInkyaRole()
        event.member.stopMuting()

        if (event.channelLeft in invisibleVoiceChannels && event.channelLeft.members.isEmpty()) {
            invisibleChannelLocked[event.guild] = false
        }
    }

    @Event
    override suspend fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        if (event.channelLeft == event.guild.afkChannel) {
            event.member.addGeneralVCPermission()
            if (!event.voiceState.isMuted) {
                event.member.addInvisibleVoiceChannelPermission()
                if (event.member.voiceState.channel in invisibleVoiceChannels) {
                    event.member.addInvisibleTextChannelPermission()
                }
            } else {
                event.member.addInkyaRole()
            }
        } else if (event.channelJoined == event.guild.afkChannel) {
            event.member.removeGeneralVCPermission()
            event.member.removeInvisibleTextChannelPermission()
            event.member.removeInvisibleVoiceChannelPermission()
            event.member.removeInkyaRole()
            event.member.stopMuting()
        }

        if (event.channelJoined in noMuteVoiceChannels && event.voiceState.isMuted) {
            event.member.moveToAFKChannel()
        }
        if (event.channelJoined in muteLimitVoiceChannels && event.voiceState.isMuted) {
            event.member.startMuting()
        }

        if (event.channelLeft in invisibleVoiceChannels && event.channelLeft.members.isEmpty()) {
            invisibleChannelLocked[event.guild] = false
        }

        if (event.channelJoined in invisibleVoiceChannels) {
            invisibleTextChannels.textChannelOf(event.guild) {
                TimeUnit.SECONDS.sleep(3)

                it.reply(event.member) {
                    embed {
                        title("General #0 へようこそ！")
                        descriptionBuilder {
                            appendln("General #0 と ${it.asMention} はVC接続中にのみ表示される隠しチャンネルです。")
                            appendln("`!lock` コマンドでチャンネルをロックし, 新たな人に表示されなくできます。")
                            append("また ${it.asMention} は接続中に投稿されたメッセージしか読むことはできず, 安全に利用できます。")
                        }
                        timestamp()
                    }
                }.launchAndDelete(30, TimeUnit.SECONDS)
            }
        }
    }

    @Event
    override suspend fun onGuildVoiceMute(event: GuildVoiceMuteEvent) {
        if (event.voiceState.channel !in invisibleVoiceChannels && event.isMuted) {
            event.member.removeInvisibleTextChannelPermission()
            event.member.removeInvisibleVoiceChannelPermission()
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

    @Event
    override suspend fun onUserUpdateGame(event: UserUpdateGameEvent) {
        when {
            event.oldGame?.name == event.newGame?.name -> {
                return
            }
            event.newGame?.name == "VRChat" -> {
                event.member.addVRCVoiceChannelPermission()
            }
            event.oldGame?.name == "VRChat" -> {
                event.member.removeVRCVoiceChannelPermission()
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
                    }.launchAndDelete(30, TimeUnit.SECONDS)
                }
                updateNoMuteMovedWarningCooldown()
            }

            guild.controller.moveVoiceMember(this, guild.afkChannel).launch()
        }
    }

    private fun Member.updateNoMuteMovedWarningCooldown() {
        noMuteMovedWarningCooldowns[this] = Date().time
    }

    private val noMuteMovedWarningThresholdSeconds = 10
    private fun Member.checkNoMuteMovedWarningCooldown(): Boolean {
        val time = noMuteMovedWarningCooldowns[this]
        if (time == 0L) {
            return true
        }

        return Date().time - time > 1000 * noMuteMovedWarningThresholdSeconds
    }

    private fun Member.startMuting() {
        if (user.isBotOrSelfUser) {
            return
        }

        muteLimitCountdowns[this] = 0
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
                        .launch()
            }
        }
    }

    private fun Member.removeGeneralVCPermission() {
        if (user.isBotOrSelfUser) {
            return
        }

        generalVCTextChannels.textChannelOf(guild) {
            it.getPermissionOverride(this)?.delete()?.launch()
        }
    }

    private fun Member.addInvisibleVoiceChannelPermission() {
        if (user.isBotOrSelfUser || invisibleChannelLocked[guild]) {
            return
        }

        invisibleVoiceChannels.voiceChannelOf(guild) {
            if (it.getPermissionOverride(this) == null) {
                it.createPermissionOverride(this)
                        .setAllow(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.VOICE_USE_VAD)
                        .setDeny(Permission.CREATE_INSTANT_INVITE, Permission.MANAGE_CHANNEL, Permission.MANAGE_ROLES, Permission.MANAGE_WEBHOOKS, Permission.VOICE_MUTE_OTHERS, Permission.VOICE_DEAF_OTHERS, Permission.VOICE_MOVE_OTHERS, Permission.PRIORITY_SPEAKER)
                        .launch()
            }
        }
    }

    private fun Member.addInvisibleTextChannelPermission() {
        if (user.isBotOrSelfUser || invisibleChannelLocked[guild]) {
            return
        }

        invisibleTextChannels.textChannelOf(guild) {
            if (it.getPermissionOverride(this) == null) {
                it.createPermissionOverride(this)
                        .setAllow(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_TTS, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_MENTION_EVERYONE, Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_ADD_REACTION)
                        .setDeny(Permission.CREATE_INSTANT_INVITE, Permission.MANAGE_CHANNEL, Permission.MANAGE_ROLES, Permission.MANAGE_WEBHOOKS, Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY)
                        .launch()
            }
        }
    }

    private fun Member.removeInvisibleVoiceChannelPermission() {
        if (user.isBotOrSelfUser) {
            return
        }

        invisibleVoiceChannels.voiceChannelOf(guild) {
            it.getPermissionOverride(this)?.delete()?.launch()
        }
    }

    private fun Member.removeInvisibleTextChannelPermission() {
        if (user.isBotOrSelfUser) {
            return
        }

        invisibleTextChannels.textChannelOf(guild) {
            it.getPermissionOverride(this)?.delete()?.launch()
        }
    }

    private fun Member.addVRCVoiceChannelPermission() {
        if (user.isBotOrSelfUser) {
            return
        }

        vrcVoiceChannels.voiceChannelOf(guild) {
            if (it.getPermissionOverride(this) == null) {
                it.createPermissionOverride(this)
                        .setAllow(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.VOICE_USE_VAD)
                        .setDeny(Permission.CREATE_INSTANT_INVITE, Permission.MANAGE_CHANNEL, Permission.MANAGE_ROLES, Permission.MANAGE_WEBHOOKS, Permission.VOICE_MUTE_OTHERS, Permission.VOICE_DEAF_OTHERS, Permission.VOICE_MOVE_OTHERS, Permission.PRIORITY_SPEAKER)
                        .launch()
            }
        }
    }

    private fun Member.removeVRCVoiceChannelPermission() {
        if (user.isBotOrSelfUser) {
            return
        }

        vrcVoiceChannels.voiceChannelOf(guild) {
            it.getPermissionOverride(this)?.delete()?.launch()
        }
    }
}
