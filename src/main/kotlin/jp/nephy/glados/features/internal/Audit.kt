package jp.nephy.glados.features.internal

import jp.nephy.glados.config
import jp.nephy.glados.core.*
import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.message
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Event
import jp.nephy.glados.core.feature.subscription.Priority
import jp.nephy.glados.secret
import jp.nephy.jsonkt.longList
import mu.KotlinLogging
import net.dv8tion.jda.client.events.relationship.*
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.audio.hooks.ConnectionStatus
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.*
import net.dv8tion.jda.core.events.channel.category.CategoryCreateEvent
import net.dv8tion.jda.core.events.channel.category.CategoryDeleteEvent
import net.dv8tion.jda.core.events.channel.category.update.CategoryUpdateNameEvent
import net.dv8tion.jda.core.events.channel.category.update.CategoryUpdatePermissionsEvent
import net.dv8tion.jda.core.events.channel.priv.PrivateChannelCreateEvent
import net.dv8tion.jda.core.events.channel.priv.PrivateChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.text.update.*
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelCreateEvent
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.voice.update.*
import net.dv8tion.jda.core.events.emote.EmoteAddedEvent
import net.dv8tion.jda.core.events.emote.EmoteRemovedEvent
import net.dv8tion.jda.core.events.emote.update.EmoteUpdateNameEvent
import net.dv8tion.jda.core.events.guild.*
import net.dv8tion.jda.core.events.guild.member.*
import net.dv8tion.jda.core.events.guild.update.*
import net.dv8tion.jda.core.events.guild.voice.*
import net.dv8tion.jda.core.events.message.MessageBulkDeleteEvent
import net.dv8tion.jda.core.events.message.MessageDeleteEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.MessageUpdateEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveAllEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent
import net.dv8tion.jda.core.events.role.RoleDeleteEvent
import net.dv8tion.jda.core.events.role.update.*
import net.dv8tion.jda.core.events.user.UserTypingEvent
import net.dv8tion.jda.core.events.user.update.*
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException
import java.nio.file.Files
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class Audit: BotFeature() {
    @Event(priority = Priority.Lowest)
    override suspend fun onStatusChange(guild: Guild, status: ConnectionStatus) {
        logger.info { "接続ステータスが変更されました: ${status.name} (${guild.name})" }
    }

    private val silentLogger = KotlinLogging.logger("Feature.Audit")
    @Event(priority = Priority.Lowest)
    override suspend fun onUserSpeaking(guild: Guild, user: User, speaking: Boolean) {
        if (speaking) {
            silentLogger.debug { "${user.displayName} (${guild.name}) がオーディオを送信しています。" }
        } else {
            silentLogger.debug { "${user.displayName} (${guild.name}) がオーディオの送信を終了しました。" }
        }
    }

    private var lastPing: Long = 0
    @Event(priority = Priority.Lowest)
    override suspend fun onPing(guild: Guild, ping: Long) {
        if (ping - lastPing > 100) {
            logger.debug { "[${guild.name}] Pingが悪化しています。現在のPing: ${ping}ms" }
        }
        lastPing = ping
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onReady(event: ReadyEvent) {
        logger.info { "Discordへの接続が確立されました。" }

        for (guild in event.jda.guilds) {
            for (channel in guild.textChannels.filter { it.hasLatestMessage() }) {
                try {
                    channel.getHistory(300).forEach { m ->
                        MessageCollector.add(m)
                        m.attachments.forEach {
                            tmpFile("attachments", m.guild.id, m.channel.id, "${m.id}_${it.id}_${it.fileName}") {
                                if (!Files.exists(toPath())) {
                                    it.download(this)
                                }
                            }
                        }
                    }
                } catch (e: InsufficientPermissionException) {
                }
            }
        }

        logger.info { "テキストチャンネルのキャッシュが完了しました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onDisconnect(event: DisconnectEvent) {
        logger.warn { "Discordとの接続が切断されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onResume(event: ResumedEvent) {
        logger.info { "Discordとの接続が再開しました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onShutdown(event: ShutdownEvent) {
        logger.info { "Discordとの接続が終了しました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onException(event: ExceptionEvent) {
        logger.error(event.cause) { "Discord接続中に例外が発生しました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onCategoryCreate(event: CategoryCreateEvent) {
        messageLog(event.guild, "カテゴリー 作成", Color.Good) { "`${event.category.name}` が作成されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onCategoryDelete(event: CategoryDeleteEvent) {
        messageLog(event.guild, "カテゴリー 削除", Color.Bad) { "`${event.category.name}` が削除されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onCategoryUpdateName(event: CategoryUpdateNameEvent) {
        messageLog(event.guild, "カテゴリー 名前変更", Color.Change) { "`${event.oldName}` から `${event.category.name}` に名前が変更されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onCategoryUpdatePermissions(event: CategoryUpdatePermissionsEvent) {
        val detail = buildString {
            appendln("\n```")
            if (event.changedRoles.isNotEmpty()) {
                appendln("ロール:")
                for (role in event.changedRoles) {
                    appendln("    ${role.name}")
                }
            }
            if (event.changedMembers.isNotEmpty()) {
                appendln("メンバー:")
                for (member in event.changedMembers) {
                    appendln("    ${member.fullNameWithoutGuild}")
                }
            }
            append("```")
        }
        slackLog(null, null, event.guild, null) { "カテゴリー `${event.category.name}` の権限が変更されました.$detail" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onPrivateChannelCreate(event: PrivateChannelCreateEvent) {
        slackLog(event.user, null, null, null) { "${event.user.displayName} とのDMが開始されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onPrivateChannelDelete(event: PrivateChannelDeleteEvent) {
        slackLog(event.user, null, null, null) { "${event.user.displayName} とのDMが終了されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onTextChannelCreate(event: TextChannelCreateEvent) {
        messageLog(event.guild, "テキストチャンネル 作成", Color.Good) { "`${event.channel.name}` が作成されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onTextChannelDelete(event: TextChannelDeleteEvent) {
        messageLog(event.guild, "テキストチャンネル 削除", Color.Bad) { "`${event.channel.name}` が削除されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onTextChannelUpdateName(event: TextChannelUpdateNameEvent) {
        messageLog(event.guild, "テキストチャンネル 名前変更", Color.Neutral) { "`${event.oldName}` から `${event.channel.name}` に名前が変更されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onTextChannelUpdateNSFW(event: TextChannelUpdateNSFWEvent) {
        if (event.channel.isNSFW) {
            messageLog(event.guild, "テキストチャンネル NSFW有効化", Color.Bad) { "`${event.channel.name}` にNSFWフラグが設定されました。" }
        } else {
            messageLog(event.guild, "テキストチャンネル NSFW無効化", Color.Good) { "`${event.channel.name}` からNSFWフラグが解除されました。" }
        }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onTextChannelUpdateParent(event: TextChannelUpdateParentEvent) {
        messageLog(event.guild, "テキストチャンネル 所属カテゴリー変更", Color.Neutral) { "`${event.channel.name}` は カテゴリー `${event.oldParent?.name ?: "(カテゴリーなし)"}` から `${event.channel.parent?.name ?: "(カテゴリーなし)"}` に移動しました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onTextChannelUpdateTopic(event: TextChannelUpdateTopicEvent) {
        if (event.oldTopic.isNullOrEmpty() && event.channel.topic.isNullOrEmpty()) {
            return
        }

        messageLog(event.guild, "テキストチャンネル トピック変更", Color.Neutral) {
            if (event.oldTopic.isEmpty() && event.channel.topic.isNotEmpty()) {
                "`${event.channel.name}` のトピックが `${event.channel.topic}` に設定されました。"
            } else if (event.oldTopic.isNotEmpty() && event.channel.topic.isEmpty()) {
                "`${event.channel.name}` のトピック `${event.oldTopic}` が削除されました。"
            } else {
                "`${event.channel.name}` のトピックが `${event.oldTopic}` から `${event.channel.topic}` に変更されました。"
            }
        }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onTextChannelUpdatePermissions(event: TextChannelUpdatePermissionsEvent) {
        val detail = buildString {
            appendln("\n```")
            if (event.changedRoles.isNotEmpty()) {
                appendln("ロール:")
                for (role in event.changedRoles) {
                    appendln("    ${role.name}")
                }
            }
            if (event.changedMembers.isNotEmpty()) {
                appendln("メンバー:")
                for (member in event.changedMembers) {
                    appendln("    ${member.fullNameWithoutGuild}")
                }
            }
            append("```")
        }
        slackLog(null, null, event.guild, null) { "テキストチャンネル `${event.channel.name}` の権限が変更されました.$detail" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onVoiceChannelCreate(event: VoiceChannelCreateEvent) {
        messageLog(event.guild, "ボイスチャンネル 作成", Color.Good) { "`${event.channel.name}` が作成されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onVoiceChannelDelete(event: VoiceChannelDeleteEvent) {
        messageLog(event.guild, "ボイスチャンネル 削除", Color.Bad) { "`${event.channel.name}` が削除されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onVoiceChannelUpdateName(event: VoiceChannelUpdateNameEvent) {
        messageLog(event.guild, "ボイスチャンネル 名前変更", Color.Neutral) { "`${event.oldName}` から `${event.channel.name}` に名前が変更されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onVoiceChannelUpdateParent(event: VoiceChannelUpdateParentEvent) {
        messageLog(event.guild, "ボイスチャンネル 所属カテゴリー変更", Color.Neutral) { "`${event.channel.name}` は カテゴリー `${event.oldParent?.name ?: "(カテゴリーなし)"}` から `${event.channel.parent?.name ?: "(カテゴリーなし)"}` に移動しました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onVoiceChannelUpdateBitrate(event: VoiceChannelUpdateBitrateEvent) {
        messageLog(event.guild, "ボイスチャンネル ビットレート変更", Color.Neutral) { "`${event.channel.name}` のビットレートが `${event.oldBitrate} kbps` から `${event.channel.bitrate} kbps` に変更されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onVoiceChannelUpdatePermissions(event: VoiceChannelUpdatePermissionsEvent) {
        val detail = buildString {
            appendln("\n```")
            if (event.changedRoles.isNotEmpty()) {
                appendln("ロール:")
                for (role in event.changedRoles) {
                    appendln("    ${role.name}")
                }
            }
            if (event.changedMembers.isNotEmpty()) {
                appendln("メンバー:")
                for (member in event.changedMembers) {
                    appendln("    ${member.fullNameWithoutGuild}")
                }
            }
            append("```")
        }
        slackLog(null, null, event.guild, null) { "ボイスチャンネル `${event.channel.name}` の権限が変更されました.$detail" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onVoiceChannelUpdateUserLimit(event: VoiceChannelUpdateUserLimitEvent) {
        messageLog(event.guild, "ボイスチャンネル 人数制限変更", Color.Neutral) { "ボイスチャンネル `${event.channel.name}` の人数制限が `${event.oldUserLimit}` から `${event.channel.userLimit}` に変更されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildJoin(event: GuildJoinEvent) {
        slackLog(null, null, event.guild, null) { "サーバー `${event.guild.name}` に参加しました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildLeave(event: GuildLeaveEvent) {
        slackLog(null, null, event.guild, null) { "サーバー `${event.guild.name}` から退出しました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildBan(event: GuildBanEvent) {
        messageLog(event.guild, "BAN", Color.Bad) { "`${event.user.displayName}` がBANされました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildUnban(event: GuildUnbanEvent) {
        messageLog(event.guild, "BAN解除", Color.Good) { "`${event.user.displayName}` がBAN解除されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildAvailable(event: GuildAvailableEvent) {
        messageLog(event.guild, "障害復帰", Color.Good) { "このDiscordサーバーは障害から復旧しました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildUnavailable(event: GuildUnavailableEvent) {
        messageLog(event.guild, "障害発生", Color.Bad) { "このDiscordサーバーでは現在障害が発生しています。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildUpdateRegion(event: GuildUpdateRegionEvent) {
        messageLog(event.guild, "リージョン変更", Color.Good) { "リージョンが `${event.oldRegion.name} (${event.oldRegionRaw})` から `${event.newRegion.name} (${event.newRegionRaw})` に変更されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildUpdateName(event: GuildUpdateNameEvent) {
        messageLog(event.guild, "サーバー名変更", Color.Neutral) { "サーバー名が `${event.oldName}` から `${event.guild.name}` に変更されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildUpdateAfkChannel(event: GuildUpdateAfkChannelEvent) {
        messageLog(event.guild, "AFKチャンネル変更", Color.Neutral) { "AFKチャンネルが `${event.oldAfkChannel.name}` から `${event.guild.afkChannel.name}` に変更されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildUpdateSystemChannel(event: GuildUpdateSystemChannelEvent) {
        messageLog(event.guild, "システムチャンネル変更", Color.Neutral) { "システムチャンネルが `${event.oldSystemChannel.name}` から `${event.guild.systemChannel.name}` に変更されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildUpdateOwner(event: GuildUpdateOwnerEvent) {
        messageLog(event.guild, "オーナー変更", Color.Neutral) { "オーナーが `${event.oldOwner.fullNameWithoutGuild}` から `${event.guild.owner.fullNameWithoutGuild}` に譲渡されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildUpdateVerificationLevel(event: GuildUpdateVerificationLevelEvent) {
        messageLog(event.guild, "認証レベル変更", Color.Neutral) { "認証レベルが `${event.oldVerificationLevel.name}` から `${event.guild.verificationLevel.name}` に変更されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildUpdateNotificationLevel(event: GuildUpdateNotificationLevelEvent) {
        messageLog(event.guild, "標準の通知設定変更", Color.Neutral) { "標準の通知設定が `${event.oldNotificationLevel.name}` から `${event.guild.defaultNotificationLevel.name}` に変更されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildUpdateExplicitContentLevel(event: GuildUpdateExplicitContentLevelEvent) {
        messageLog(event.guild, "不適切な表現のフィルター変更", Color.Neutral) { "不適切な表現のフィルターが `${event.oldLevel}` から `${event.guild.explicitContentLevel.name}` に変更されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildUpdateMFALevel(event: GuildUpdateMFALevelEvent) {
        messageLog(event.guild, "サーバーの二段階認証変更", Color.Neutral) { "サーバーの二段階認証が `${event.oldMFALevel.name}` から `${event.guild.requiredMFALevel.name}` に変更されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildUpdateSplash(event: GuildUpdateSplashEvent) {
        messageLog(event.guild, "スプラッシュ変更", Color.Neutral) { "スプラッシュが `${event.oldSplashUrl}` から `${event.guild.splashUrl}` に変更されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildUpdateAfkTimeout(event: GuildUpdateAfkTimeoutEvent) {
        messageLog(event.guild, "AFKタイムアウト変更", Color.Neutral) { "AFKタイムアウトが `${event.oldAfkTimeout.seconds / 60}分` から `${event.guild.afkTimeout.seconds / 60}分` に変更されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildUpdateFeatures(event: GuildUpdateFeaturesEvent) {
        messageLog(event.guild, "機能変更", Color.Neutral) { "機能が `${event.oldFeatures.joinToString(", ")}` から `${event.guild.features.joinToString(", ")}` に変更されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildUpdateIcon(event: GuildUpdateIconEvent) {
        messageLog(event.guild, "アイコン変更", Color.Neutral) { "アイコンが `${event.oldIconUrl}` から `${event.guild.iconUrl}` に変更されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onEmoteAdded(event: EmoteAddedEvent) {
        messageLog(event.guild, "絵文字追加", Color.Good) { "絵文字 ${event.emote.asMention} `${event.emote.name}` が追加されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onEmoteRemoved(event: EmoteRemovedEvent) {
        messageLog(event.guild, "絵文字削除", Color.Bad) { "絵文字 ${event.emote.asMention} `${event.emote.name}` が削除されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onEmoteUpdateName(event: EmoteUpdateNameEvent) {
        messageLog(event.guild, "絵文字名前変更", Color.Neutral) { "絵文字の名前が :${event.newName}: `:${event.oldName}:` → `:${event.newName}:` に変更されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildMemberNickChange(event: GuildMemberNickChangeEvent) {
        slackLog(event.user, event.member, event.guild, null) { "名前を ${event.prevNick ?: event.user.name} から ${event.newNick ?: event.user.name} に変更しました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        messageLog(event.guild, "メンバー追加", Color.Good) { "${event.member.fullNameWithoutGuild} ${event.member.asMention} がサーバーに参加しました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildMemberLeave(event: GuildMemberLeaveEvent) {
        messageLog(event.guild, "メンバー退出", Color.Bad) { "`${event.member.fullNameWithoutGuild}` がサーバーから退出しました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onRoleDelete(event: RoleDeleteEvent) {
        messageLog(event.guild, "ロール削除", Color.Bad) { "ロール `${event.role.name}` が削除されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onRoleUpdateName(event: RoleUpdateNameEvent) {
        if (event.oldName == "new role") {
            messageLog(event.guild, "ロール追加", Color.Good) { "ロール `${event.role.name}` が作成されました。" }
        } else {
            messageLog(event.guild, "ロール名前変更", Color.Neutral) { "ロールの名前が `${event.oldName}` から `${event.role.name}` に変更されました。" }
        }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent) {
        val watchRoles = config.forGuild(event.guild)?.option("watch_roles") { it.longList }.orEmpty()
        event.roles.forEach {
            slackLog(event.user, event.member, event.guild, null) { "ロール \"${it.name}\" が付与されました。" }
            if (it.idLong in watchRoles) {
                messageLog(event.guild, "システムロール 付与", Color.Change, slack = {}) { "${event.member.fullNameWithoutGuild} ${event.member.asMention} に `${it.name}` が付与されました。" }
            }
        }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {
        val watchRoles = config.forGuild(event.guild)?.option("watch_roles") { it.longList }.orEmpty()
        event.roles.forEach {
            slackLog(event.user, event.member, event.guild, null) { "ロール \"${it.name}\" が剥奪されました。" }
            if (it.idLong in watchRoles) {
                messageLog(event.guild, "システムロール 剥奪", Color.Change, slack = {}) { "${event.member.fullNameWithoutGuild} ${event.member.asMention} から `${it.name}` が剥奪されました。" }
            }
        }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onRoleUpdateColor(event: RoleUpdateColorEvent) {
        slackLog(null, null, event.guild, null) { "ロール `${event.role.name}` の色が ${event.oldColor} から ${event.role.color} に変更されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onRoleUpdateMentionable(event: RoleUpdateMentionableEvent) {
        if (event.wasMentionable()) {
            slackLog(null, null, event.guild, null) { "ロール `${event.role.name}` がメンション不可能になりました。" }
        } else {
            slackLog(null, null, event.guild, null) { "ロール `${event.role.name}` がメンション可能になりました。" }
        }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onRoleUpdateHoisted(event: RoleUpdateHoistedEvent) {
        if (event.wasHoisted()) {
            slackLog(null, null, event.guild, null) { "ロール `${event.role.name}` がオンラインユーザに表示されなくなりました。" }
        } else {
            slackLog(null, null, event.guild, null) { "ロール `${event.role.name}` がオンラインユーザに表示されるようになりました。" }
        }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onRoleUpdatePermissions(event: RoleUpdatePermissionsEvent) {
        val (addedPermissions, removedPermissions) = event.newPermissions - event.oldPermissions to event.oldPermissions - event.newPermissions
        slackLog(null, null, event.guild, null) { "ロール `${event.role.name}` の権限が変更されました.\n```\n追加された権限:\n${addedPermissions.joinToString("\n") { "    ${it.getName()}" }}\n削除された権限:\n${removedPermissions.joinToString("\n") { "    ${it.getName()}" }}\n```" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        slackLog(null, event.member, event.guild, null) { "VC \"${event.channelJoined.name}\" に参加しました. このチャンネルには 現在${event.channelJoined.members.size}人が接続しています。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        slackLog(null, event.member, event.guild, null) { "VC \"${event.channelLeft.name}\" から退出しました. このチャンネルには 現在${event.channelLeft.members.size}人が接続しています。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        if (event.channelJoined == event.guild.afkChannel) {
            slackLog(null, event.member, event.guild, null) { "VC \"${event.channelLeft.name}\" から AFKチャンネル \"${event.channelJoined.name}\" に移動しました。" }
        } else {
            slackLog(null, event.member, event.guild, null) { "VC \"${event.channelLeft.name}\" から \"${event.channelJoined.name}\" に移動しました。" }
        }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildVoiceGuildMute(event: GuildVoiceGuildMuteEvent) {
        if (event.isGuildMuted) {
            slackLog(null, event.member, event.guild, null) { "サーバーマイクミュートされました。" }
        } else {
            slackLog(null, event.member, event.guild, null) { "サーバーマイクミュート解除されました。" }
        }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildVoiceSelfMute(event: GuildVoiceSelfMuteEvent) {
        if (event.isSelfMuted) {
            slackLog(null, event.member, event.guild, null) { "マイクミュートしました。" }
        } else {
            slackLog(null, event.member, event.guild, null) { "マイクミュート解除しました。" }
        }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildVoiceGuildDeafen(event: GuildVoiceGuildDeafenEvent) {
        if (event.isGuildDeafened) {
            slackLog(null, event.member, event.guild, null) { "サーバースピーカーミュートされました。" }
        } else {
            slackLog(null, event.member, event.guild, null) { "サーバースピーカーミュート解除されました。" }
        }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onGuildVoiceSelfDeafen(event: GuildVoiceSelfDeafenEvent) {
        if (event.isSelfDeafened) {
            slackLog(null, event.member, event.guild, null) { "スピーカーミュートしました。" }
        } else {
            slackLog(null, event.member, event.guild, null) { "スピーカーミュート解除しました。" }
        }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onUserTyping(event: UserTypingEvent) {
        if (event.user.isBotOrSelfUser) {
            return
        }

        val channelName = when (event.type) {
            ChannelType.TEXT -> "#${event.textChannel.name} (${event.guild.name})"
            ChannelType.PRIVATE -> "#${event.privateChannel.name} w/ ${event.privateChannel.user.displayName}"
            else -> return
        }
        slackLog(event.user, event.member, event.guild, null) { "$channelName で現在入力中です..." }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onUserUpdateName(event: UserUpdateNameEvent) {
        if (event.user.isBotOrSelfUser) {
            return
        }

        slackLog(event.user, null, null, null) { "ユーザ名を変更しました. 元のユーザ名: @${event.oldName}#${event.entity.discriminator}" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onUserUpdateDiscriminator(event: UserUpdateDiscriminatorEvent) {
        if (event.user.isBotOrSelfUser) {
            return
        }

        slackLog(event.user, null, null, null) { "識別子を変更しました. 元のユーザ名: @${event.entity.name}#${event.oldDiscriminator}" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onUserUpdateAvatar(event: UserUpdateAvatarEvent) {
        slackLog(event.user, null, null, null) { "アイコンを変更しました. 元のアイコン: ${event.oldAvatarUrl}" }
    }

    private val onlineTimeCache = ConcurrentHashMap<Long, Long>()
    private val afkTimeCache = ConcurrentHashMap<Long, Long>()
    @Event(priority = Priority.Lowest)
    override suspend fun onUserUpdateOnlineStatus(event: UserUpdateOnlineStatusEvent) {
        val (previous, current) = event.oldOnlineStatus to event.newOnlineStatus
        val onlineCount = event.guild.members.count { it.onlineStatus != OnlineStatus.OFFLINE }
        val afkTime = afkTimeCache[event.user.idLong + event.guild.idLong].toDeltaMilliSecondString()
        val onlineTime = onlineTimeCache[event.user.idLong + event.guild.idLong].toDeltaMilliSecondString()

        if (current == OnlineStatus.IDLE) {
            afkTimeCache[event.user.idLong + event.guild.idLong] = Date().time
        } else if (previous == OnlineStatus.OFFLINE) {
            onlineTimeCache[event.user.idLong + event.guild.idLong] = Date().time
        }

        val text = when (previous to current) {
            OnlineStatus.ONLINE to OnlineStatus.DO_NOT_DISTURB -> "非通知モードを有効にしました。"
            OnlineStatus.ONLINE to OnlineStatus.IDLE -> "AFKになりました。"
            OnlineStatus.ONLINE to OnlineStatus.OFFLINE -> "オフラインになりました($onlineTime). 現在のオンライン数: $onlineCount"

            OnlineStatus.DO_NOT_DISTURB to OnlineStatus.ONLINE -> "非通知モードを無効にしました。"
            OnlineStatus.DO_NOT_DISTURB to OnlineStatus.IDLE -> "AFKになりました。"
            OnlineStatus.DO_NOT_DISTURB to OnlineStatus.OFFLINE -> "オフラインになりました($onlineTime). 現在のオンライン数: $onlineCount"

            OnlineStatus.IDLE to OnlineStatus.ONLINE -> "戻りました($afkTime)。"
            OnlineStatus.IDLE to OnlineStatus.DO_NOT_DISTURB -> "非通知モードを有効にしました。"
            OnlineStatus.IDLE to OnlineStatus.OFFLINE -> "オフラインになりました($onlineTime). 現在のオンライン数: $onlineCount"

            OnlineStatus.OFFLINE to OnlineStatus.ONLINE -> "オンラインになりました. 現在のオンライン数: $onlineCount"
            OnlineStatus.OFFLINE to OnlineStatus.DO_NOT_DISTURB -> "非通知モードでオンラインになりました. 現在のオンライン数: $onlineCount"
            OnlineStatus.OFFLINE to OnlineStatus.IDLE -> "AFK状態でオンラインになりました. 現在のオンライン数: $onlineCount"

            else -> return
        }

        slackLog(event.user, event.member, event.guild, null) { text }
    }

    private val gameCache = ConcurrentHashMap<Long, Long>()
    @Event(priority = Priority.Lowest)
    override suspend fun onUserUpdateGame(event: UserUpdateGameEvent) {
        if (event.user.isBotOrSelfUser) {
            return
        }

        val (previous, current) = event.oldGame to event.newGame

        val text = when {
            previous?.name == null && current?.name != null -> {
                gameCache[event.user.idLong + event.guild.idLong] = Date().time
                "\"${current.name}\" を始めました。"
            }
            previous?.name != null && current?.name == null -> {
                val time = gameCache[event.user.idLong + event.guild.idLong].toDeltaMilliSecondString()
                "\"${previous.name}\" をやめました($time)。"
            }
            previous?.name != null && current?.name != null && previous.name != current.name -> {
                val time = gameCache[event.user.idLong + event.guild.idLong].toDeltaMilliSecondString()
                gameCache[event.user.idLong + event.guild.idLong] = Date().time
                "\"${previous.name}\" をやめ, \"${current.name}\" を始めました($time)。"
            }
            else -> return
        }

        slackLog(event.user, event.member, event.guild, null) { text }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onFriendAdded(event: FriendAddedEvent) {
        slackLog(event.user, null, null, null) { "${event.friend.user.displayName} とフレンドになりました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onFriendRemoved(event: FriendRemovedEvent) {
        slackLog(event.user, null, null, null) { "${event.friend.user.displayName} とのフレンド関係が解消しました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onFriendRequestReceived(event: FriendRequestReceivedEvent) {
        slackLog(event.user, null, null, null) { "${event.friendRequest.user.displayName} からフレンドリクエストを受け取りました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onFriendRequestSent(event: FriendRequestSentEvent) {
        slackLog(event.user, null, null, null) { "${event.friendRequest.user.displayName} にフレンドリクエストが送信されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onFriendRequestCanceled(event: FriendRequestCanceledEvent) {
        slackLog(event.user, null, null, null) { "${event.friendRequest.user.displayName} からのフレンドリクエストを破棄しました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onFriendRequestIgnored(event: FriendRequestIgnoredEvent) {
        slackLog(event.user, null, null, null) { "${event.friendRequest.user.displayName} にフレンドリクエストを破棄されました。" }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBotOrSelfUser) {
            return
        }

        if (event.guild != null) {
            event.message.attachments.forEach {
                tmpFile("attachments", event.message.guild.id, event.message.channel.id, "${it.id}_${it.fileName}") {
                    it.download(this)
                }
            }

            slackLog(event.author, event.member, event.guild, event.message) {
                buildString {
                    append("```\n${event.message.contentDisplay}\n```")
                    if (event.message.attachments.isNotEmpty()) {
                        append("\n(添付ファイル: ${event.message.attachments.joinToString(", ") { "https://secure.nephy.jp/file/discord/${event.message.guild.id}/${event.message.channel.id}/${it.id}_${it.fileName} (${it.proxyUrl})" }}")
                    }
                    if (event.message.embeds.isNotEmpty()) {
                        append("\n(埋め込み: ${event.message.embeds.asSequence().map { it.toJSONObject() }.joinToString(", ")} )")
                    }
                }
            }
        } else {
            event.message.attachments.forEach {
                tmpFile("attachments", "private", event.message.author.id, "${it.id}_${it.fileName}") {
                    it.download(this)
                }
            }

            slackLog(event.author, event.member, event.guild, event.message) {
                buildString {
                    append("```\n${event.message.contentDisplay}\n```")
                    if (event.message.attachments.isNotEmpty()) {
                        append("\n(添付ファイル: ${event.message.attachments.joinToString(", ") { "https://secure.nephy.jp/file/discord/private/${event.message.author.id}/${it.id}_${it.fileName} (${it.proxyUrl})" }}")
                    }
                    if (event.message.embeds.isNotEmpty()) {
                        append("\n(埋め込み: ${event.message.embeds.asSequence().map { it.toJSONObject() }.joinToString(", ")} )")
                    }
                }
            }
        }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onMessageUpdate(event: MessageUpdateEvent) {
        if (event.author.isBotOrSelfUser) {
            return
        }

        val updated = event.message
        if (updated.guild != null) {
            slackLog(event.author, event.member, event.guild, event.message, MessageType.Updated) {
                buildString {
                    append("```\n${event.message.contentDisplay}\n```")
                    if (event.message.attachments.isNotEmpty()) {
                        append("\n(添付ファイル: ${event.message.attachments.joinToString(", ") { "https://secure.nephy.jp/file/discord/${event.message.guild.id}/${event.message.channel.id}/${it.id}_${it.fileName} (${it.proxyUrl})" }}")
                    }
                    if (event.message.embeds.isNotEmpty()) {
                        append("\n(埋め込み: ${event.message.embeds.asSequence().map { it.toJSONObject() }.joinToString(", ")} )")
                    }
                }
            }
        } else {
            slackLog(event.author, event.member, event.guild, event.message, MessageType.Updated) {
                buildString {
                    append("```\n${event.message.contentDisplay}\n```")
                    if (event.message.attachments.isNotEmpty()) {
                        append("\n(添付ファイル: ${event.message.attachments.joinToString(", ") { "https://secure.nephy.jp/file/discord/private/${event.message.author.id}/${it.id}_${it.fileName} (${it.proxyUrl})" }}")
                    }
                    if (event.message.embeds.isNotEmpty()) {
                        append("\n(埋め込み: ${event.message.embeds.asSequence().map { it.toJSONObject() }.joinToString(", ")} )")
                    }
                }
            }
        }

        val history = MessageCollector.history(event.messageIdLong)
                ?: return logger.warn { "キャッシュされていないメッセージが編集されました: ${event.messageId} (#${event.channel.name}, ${event.guild?.name})" }
        if (history.guild != null) {
            slackLog(event.author, event.member, event.guild, history, MessageType.History) {
                buildString {
                    append("```\n${history.contentDisplay}\n```")
                    if (history.attachments.isNotEmpty()) {
                        append("\n(添付ファイル: ${history.attachments.joinToString(", ") { "https://secure.nephy.jp/file/discord/${history.guild.id}/${history.channel.id}/${it.id}_${it.fileName} (${it.proxyUrl})" }}")
                    }
                    if (history.embeds.isNotEmpty()) {
                        append("\n(埋め込み: ${history.embeds.asSequence().map { it.toJSONObject() }.joinToString(", ")} )")
                    }
                }
            }
        } else {
            slackLog(event.author, event.member, event.guild, history, MessageType.History) {
                buildString {
                    append("```\n${history.contentDisplay}\n```")
                    if (history.attachments.isNotEmpty()) {
                        append("\n(添付ファイル: ${history.attachments.joinToString(", ") { "https://secure.nephy.jp/file/discord/private/${history.author.id}/${it.id}_${it.fileName} (${it.proxyUrl})" }}")
                    }
                    if (history.embeds.isNotEmpty()) {
                        append("\n(埋め込み: ${history.embeds.asSequence().map { it.toJSONObject() }.joinToString(", ")} )")
                    }
                }
            }
        }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        if (event.user.isBotOrSelfUser) {
            return
        }

        val message = MessageCollector.latest(event.messageIdLong)
                ?: return logger.warn { "キャッシュされていないメッセージにリアクションが付きました: ${event.messageId} (#${event.channel.name}, ${event.guild?.name})" }

        slackLog(event.user, event.member, event.guild, message, MessageType.Reaction) {
            buildString {
                append("${message.member?.fullNameWithoutGuild ?: message.author.displayName}の\n```\n${message.contentDisplay}\n```")
                if (message.attachments.isNotEmpty()) {
                    append("\n(添付ファイル: ${message.attachments.joinToString(", ") { "https://secure.nephy.jp/file/discord/${message.guild?.id ?: "dm"}/${message.channel.id}/${it.id}_${it.fileName} (${it.proxyUrl})" }}")
                }
                if (message.embeds.isNotEmpty()) {
                    append("\n(埋め込み: ${message.embeds.asSequence().map { it.toJSONObject() }.joinToString(", ")} )")
                }
                append("\nにリアクション `${event.reactionEmote.name}` を付けました. (#${event.channel.name})")
            }
        }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onMessageReactionRemove(event: MessageReactionRemoveEvent) {
        if (event.user.isBotOrSelfUser) {
            return
        }

        val message = MessageCollector.latest(event.messageIdLong)
                ?: return logger.warn { "キャッシュされていないメッセージからリアクションが削除されました: ${event.messageId} (#${event.channel.name}, ${event.guild?.name})" }

        slackLog(event.user, event.member, event.guild, message, MessageType.Reaction) {
            buildString {
                append("${message.member?.fullNameWithoutGuild ?: message.author.displayName}の\n```\n${message.contentDisplay}\n```")
                if (message.attachments.isNotEmpty()) {
                    append("\n(添付ファイル: ${message.attachments.joinToString(", ") { "https://secure.nephy.jp/file/discord/${message.guild?.id ?: "dm"}/${message.channel.id}/${it.id}_${it.fileName} (${it.proxyUrl})" }}")
                }
                if (message.embeds.isNotEmpty()) {
                    append("\n(埋め込み: ${message.embeds.asSequence().map { it.toJSONObject() }.joinToString(", ")} )")
                }
                append("\nからリアクション `${event.reactionEmote.name}` を削除しました. (#${event.channel.name})")
            }
        }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onMessageReactionRemoveAll(event: MessageReactionRemoveAllEvent) {
        val message = MessageCollector.latest(event.messageIdLong)
                ?: return logger.warn { "キャッシュされていないメッセージのリアクションがリセットされました: ${event.messageId} (#${event.channel.name}, ${event.guild?.name})" }

        slackLog(null, null, event.guild, message, MessageType.Reaction) {
            buildString {
                append("```\n${message.contentDisplay}\n```")
                if (message.attachments.isNotEmpty()) {
                    append("\n(添付ファイル: ${message.attachments.joinToString(", ") { "https://secure.nephy.jp/file/discord/${message.guild.id}/${message.channel.id}/${it.id}_${it.fileName} (${it.proxyUrl})" }}")
                }
                if (message.embeds.isNotEmpty()) {
                    append("\n(埋め込み: ${message.embeds.asSequence().map { it.toJSONObject() }.joinToString(", ")} )")
                }
                append("\nのリアクションがリセットされました. (#${event.channel.name})")
            }
        }
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onMessageDelete(event: MessageDeleteEvent) {
        messageDeletionHandler(event.channel, event.guild, event.messageIdLong)
    }

    @Event(priority = Priority.Lowest)
    override suspend fun onMessageBulkDelete(event: MessageBulkDeleteEvent) {
        event.messageIds.forEach {
            messageDeletionHandler(event.channel, event.guild, it.toLong())
        }
    }

    private fun messageDeletionHandler(channel: MessageChannel, guild: Guild?, messageId: Long) {
        val deleted = MessageCollector.latest(messageId) ?: return logger.warn { "キャッシュされていないメッセージが削除されました: $messageId (${channel.name}, ${guild?.name})" }
        if (deleted.author.isBotOrSelfUser) {
            return
        }

        slackLog(deleted.author, deleted.member, deleted.guild, deleted) {
            buildString {
                append("```\n${deleted.contentDisplay}\n```")
                if (deleted.attachments.isNotEmpty()) {
                    append("\n(添付ファイル: ${deleted.attachments.joinToString(", ") { "https://secure.nephy.jp/file/discord/${deleted.guild.id}/${deleted.channel.id}/${it.id}_${it.fileName} (${it.proxyUrl})" }}")
                }
                if (deleted.embeds.isNotEmpty()) {
                    append("\n(埋め込み: ${deleted.embeds.asSequence().map { it.toJSONObject() }.joinToString(", ")} )")
                }
            }
        }
    }

    private fun messageLog(guild: Guild, title: String, color: Color, slack: (() -> Unit)? = null, message: () -> String) {
        val channel = config.forGuild(guild)?.textChannel("log") ?: return
        channel.message {
            embed {
                title(title)
                description(message)
                color(color)
                footer(guild.name, guild.iconUrl)
                timestamp()
            }
        }.launch()

        val function = slack ?: {
            slackLog(null, null, guild, null, text = message)
        }
        function.invoke()
    }

    private val slack = SlackWebhook(secret.forKey("slack_webhook_url"))
    private fun slackLog(user: User?, member: Member?, guild: Guild?, message: Message?, messageType: MessageType = MessageType.Sent, text: () -> String) {
        val guildConfig = config.forGuild(guild)
        if (guildConfig?.boolOption("enable_audit", false) == false) {
            return
        }

        val channel = when (guildConfig?.isMain) {
            true -> when {
                message != null -> messageType.mainChannel
                member != null -> "#discord-member"
                else -> "#discord-guild"
            }
            false -> when {
                message != null -> messageType.subChannel
                member != null -> "#discord-member-other"
                else -> "#discord-guild-other"
            }
            null -> "#discord-misc"
        }
        val username = when {
            message != null && messageType != MessageType.Reaction -> when (guildConfig?.isMain) {
                true -> message.fullNameWithoutGuild
                else -> message.fullName
            }
            member != null -> when (guildConfig?.isMain) {
                true -> member.fullNameWithoutGuild
                else -> member.fullName
            }
            user != null -> user.displayName
            guild != null -> guild.name
            else -> "サーバーログ"
        }
        val icon = message?.member?.user?.effectiveAvatarUrl ?: member?.user?.effectiveAvatarUrl ?: user?.effectiveAvatarUrl ?: guild?.iconUrl ?: ":desktop_computer:"

        slack.message(channel) {
            username(username)
            icon(icon)
            text(text)
        }
    }

    private enum class MessageType(internal val mainChannel: String, internal val subChannel: String) {
        Sent("#discord-message", "#discord-message-other"),
        Deleted("#discord-deleted", "#discord-deleted-other"),
        Updated("#discord-updated", "#discord-updated-other"),
        History("#discord-history", "#discord-history-other"),
        Reaction("#discord-reaction", "#discord-reaction-othe")
    }
}
