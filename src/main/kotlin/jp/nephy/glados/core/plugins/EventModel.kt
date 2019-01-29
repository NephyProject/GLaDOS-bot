package jp.nephy.glados.core.plugins

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import io.ktor.http.HttpStatusCode
import io.ktor.sessions.CookieSessionBuilder
import jp.nephy.glados.core.GuildPlayer
import net.dv8tion.jda.client.events.call.CallCreateEvent
import net.dv8tion.jda.client.events.call.CallDeleteEvent
import net.dv8tion.jda.client.events.call.GenericCallEvent
import net.dv8tion.jda.client.events.call.update.CallUpdateRegionEvent
import net.dv8tion.jda.client.events.call.update.CallUpdateRingingUsersEvent
import net.dv8tion.jda.client.events.call.update.GenericCallUpdateEvent
import net.dv8tion.jda.client.events.call.voice.*
import net.dv8tion.jda.client.events.group.*
import net.dv8tion.jda.client.events.group.update.GenericGroupUpdateEvent
import net.dv8tion.jda.client.events.group.update.GroupUpdateIconEvent
import net.dv8tion.jda.client.events.group.update.GroupUpdateNameEvent
import net.dv8tion.jda.client.events.group.update.GroupUpdateOwnerEvent
import net.dv8tion.jda.client.events.message.group.*
import net.dv8tion.jda.client.events.message.group.react.GenericGroupMessageReactionEvent
import net.dv8tion.jda.client.events.message.group.react.GroupMessageReactionAddEvent
import net.dv8tion.jda.client.events.message.group.react.GroupMessageReactionRemoveAllEvent
import net.dv8tion.jda.client.events.message.group.react.GroupMessageReactionRemoveEvent
import net.dv8tion.jda.client.events.relationship.*
import net.dv8tion.jda.core.audio.CombinedAudio
import net.dv8tion.jda.core.audio.SpeakingMode
import net.dv8tion.jda.core.audio.UserAudio
import net.dv8tion.jda.core.audio.hooks.ConnectionStatus
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.*
import net.dv8tion.jda.core.events.channel.category.CategoryCreateEvent
import net.dv8tion.jda.core.events.channel.category.CategoryDeleteEvent
import net.dv8tion.jda.core.events.channel.category.GenericCategoryEvent
import net.dv8tion.jda.core.events.channel.category.update.CategoryUpdateNameEvent
import net.dv8tion.jda.core.events.channel.category.update.CategoryUpdatePermissionsEvent
import net.dv8tion.jda.core.events.channel.category.update.CategoryUpdatePositionEvent
import net.dv8tion.jda.core.events.channel.category.update.GenericCategoryUpdateEvent
import net.dv8tion.jda.core.events.channel.priv.PrivateChannelCreateEvent
import net.dv8tion.jda.core.events.channel.priv.PrivateChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.text.GenericTextChannelEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.text.update.*
import net.dv8tion.jda.core.events.channel.voice.GenericVoiceChannelEvent
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelCreateEvent
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.voice.update.*
import net.dv8tion.jda.core.events.emote.EmoteAddedEvent
import net.dv8tion.jda.core.events.emote.EmoteRemovedEvent
import net.dv8tion.jda.core.events.emote.GenericEmoteEvent
import net.dv8tion.jda.core.events.emote.update.EmoteUpdateNameEvent
import net.dv8tion.jda.core.events.emote.update.EmoteUpdateRolesEvent
import net.dv8tion.jda.core.events.emote.update.GenericEmoteUpdateEvent
import net.dv8tion.jda.core.events.guild.*
import net.dv8tion.jda.core.events.guild.member.*
import net.dv8tion.jda.core.events.guild.update.*
import net.dv8tion.jda.core.events.guild.voice.*
import net.dv8tion.jda.core.events.http.HttpRequestEvent
import net.dv8tion.jda.core.events.message.*
import net.dv8tion.jda.core.events.message.guild.*
import net.dv8tion.jda.core.events.message.guild.react.GenericGuildMessageReactionEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveAllEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent
import net.dv8tion.jda.core.events.message.priv.*
import net.dv8tion.jda.core.events.message.priv.react.GenericPrivateMessageReactionEvent
import net.dv8tion.jda.core.events.message.priv.react.PrivateMessageReactionAddEvent
import net.dv8tion.jda.core.events.message.priv.react.PrivateMessageReactionRemoveEvent
import net.dv8tion.jda.core.events.message.react.GenericMessageReactionEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveAllEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent
import net.dv8tion.jda.core.events.role.GenericRoleEvent
import net.dv8tion.jda.core.events.role.RoleCreateEvent
import net.dv8tion.jda.core.events.role.RoleDeleteEvent
import net.dv8tion.jda.core.events.role.update.*
import net.dv8tion.jda.core.events.self.*
import net.dv8tion.jda.core.events.user.GenericUserEvent
import net.dv8tion.jda.core.events.user.UserTypingEvent
import net.dv8tion.jda.core.events.user.update.*
import java.util.*

interface EventModel {
    @Target(AnnotationTarget.FUNCTION)
    annotation class FromListenerAdapter

    @Target(AnnotationTarget.FUNCTION)
    annotation class FromConnectionListener

    @Target(AnnotationTarget.FUNCTION)
    annotation class FromAudioEventAdapter

    @Target(AnnotationTarget.FUNCTION)
    annotation class FromAudioReceiveHandler

    @Target(AnnotationTarget.FUNCTION)
    annotation class FromTweetstorm

    @Target(AnnotationTarget.FUNCTION)
    annotation class FromWebPage

    @Target(AnnotationTarget.FUNCTION)
    annotation class FromWebErrorPage

    @Target(AnnotationTarget.FUNCTION)
    annotation class FromWebSession

    /* ListenerAdapter */
    @FromListenerAdapter
    suspend fun onGenericEvent(event: Event) {
    }

    @FromListenerAdapter
    suspend fun onGenericUpdate(event: UpdateEvent<*, *>) {
    }

    @FromListenerAdapter
    suspend fun onReady(event: ReadyEvent) {
    }

    @FromListenerAdapter
    suspend fun onResume(event: ResumedEvent) {
    }

    @FromListenerAdapter
    suspend fun onReconnect(event: ReconnectedEvent) {
    }

    @FromListenerAdapter
    suspend fun onDisconnect(event: DisconnectEvent) {
    }

    @FromListenerAdapter
    suspend fun onShutdown(event: ShutdownEvent) {
    }

    @FromListenerAdapter
    suspend fun onStatusChange(event: StatusChangeEvent) {
    }

    @FromListenerAdapter
    suspend fun onException(event: ExceptionEvent) {
    }

    @FromListenerAdapter
    suspend fun onUserUpdateName(event: UserUpdateNameEvent) {
    }

    @FromListenerAdapter
    suspend fun onUserUpdateDiscriminator(event: UserUpdateDiscriminatorEvent) {
    }

    @FromListenerAdapter
    suspend fun onUserUpdateAvatar(event: UserUpdateAvatarEvent) {
    }

    @FromListenerAdapter
    suspend fun onUserUpdateOnlineStatus(event: UserUpdateOnlineStatusEvent) {
    }

    @FromListenerAdapter
    suspend fun onUserUpdateGame(event: UserUpdateGameEvent) {
    }

    @FromListenerAdapter
    suspend fun onUserTyping(event: UserTypingEvent) {
    }

    @FromListenerAdapter
    suspend fun onSelfUpdateAvatar(event: SelfUpdateAvatarEvent) {
    }

    @FromListenerAdapter
    suspend fun onSelfUpdateEmail(event: SelfUpdateEmailEvent) {
    }

    @FromListenerAdapter
    suspend fun onSelfUpdateMFA(event: SelfUpdateMFAEvent) {
    }

    @FromListenerAdapter
    suspend fun onSelfUpdateName(event: SelfUpdateNameEvent) {
    }

    @FromListenerAdapter
    suspend fun onSelfUpdateVerified(event: SelfUpdateVerifiedEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildMessageDelete(event: GuildMessageDeleteEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildMessageEmbed(event: GuildMessageEmbedEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildMessageReactionAdd(event: GuildMessageReactionAddEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildMessageReactionRemove(event: GuildMessageReactionRemoveEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildMessageReactionRemoveAll(event: GuildMessageReactionRemoveAllEvent) {
    }

    @FromListenerAdapter
    suspend fun onPrivateMessageReceived(event: PrivateMessageReceivedEvent) {
    }

    @FromListenerAdapter
    suspend fun onPrivateMessageUpdate(event: PrivateMessageUpdateEvent) {
    }

    @FromListenerAdapter
    suspend fun onPrivateMessageDelete(event: PrivateMessageDeleteEvent) {
    }

    @FromListenerAdapter
    suspend fun onPrivateMessageEmbed(event: PrivateMessageEmbedEvent) {
    }

    @FromListenerAdapter
    suspend fun onPrivateMessageReactionAdd(event: PrivateMessageReactionAddEvent) {
    }

    @FromListenerAdapter
    suspend fun onPrivateMessageReactionRemove(event: PrivateMessageReactionRemoveEvent) {
    }

    @FromListenerAdapter
    suspend fun onMessageReceived(event: MessageReceivedEvent) {
    }

    @FromListenerAdapter
    suspend fun onMessageUpdate(event: MessageUpdateEvent) {
    }

    @FromListenerAdapter
    suspend fun onMessageDelete(event: MessageDeleteEvent) {
    }

    @FromListenerAdapter
    suspend fun onMessageBulkDelete(event: MessageBulkDeleteEvent) {
    }

    @FromListenerAdapter
    suspend fun onMessageEmbed(event: MessageEmbedEvent) {
    }

    @FromListenerAdapter
    suspend fun onMessageReactionAdd(event: MessageReactionAddEvent) {
    }

    @FromListenerAdapter
    suspend fun onMessageReactionRemove(event: MessageReactionRemoveEvent) {
    }

    @FromListenerAdapter
    suspend fun onMessageReactionRemoveAll(event: MessageReactionRemoveAllEvent) {
    }

    @FromListenerAdapter
    suspend fun onTextChannelDelete(event: TextChannelDeleteEvent) {
    }

    @FromListenerAdapter
    suspend fun onTextChannelUpdateName(event: TextChannelUpdateNameEvent) {
    }

    @FromListenerAdapter
    suspend fun onTextChannelUpdateTopic(event: TextChannelUpdateTopicEvent) {
    }

    @FromListenerAdapter
    suspend fun onTextChannelUpdatePosition(event: TextChannelUpdatePositionEvent) {
    }

    @FromListenerAdapter
    suspend fun onTextChannelUpdatePermissions(event: TextChannelUpdatePermissionsEvent) {
    }

    @FromListenerAdapter
    suspend fun onTextChannelUpdateNSFW(event: TextChannelUpdateNSFWEvent) {
    }

    @FromListenerAdapter
    suspend fun onTextChannelUpdateParent(event: TextChannelUpdateParentEvent) {
    }

    @FromListenerAdapter
    suspend fun onTextChannelUpdateSlowmode(event: TextChannelUpdateSlowmodeEvent) {
    }

    @FromListenerAdapter
    suspend fun onTextChannelCreate(event: TextChannelCreateEvent) {
    }

    @FromListenerAdapter
    suspend fun onVoiceChannelDelete(event: VoiceChannelDeleteEvent) {
    }

    @FromListenerAdapter
    suspend fun onVoiceChannelUpdateName(event: VoiceChannelUpdateNameEvent) {
    }

    @FromListenerAdapter
    suspend fun onVoiceChannelUpdatePosition(event: VoiceChannelUpdatePositionEvent) {
    }

    @FromListenerAdapter
    suspend fun onVoiceChannelUpdateUserLimit(event: VoiceChannelUpdateUserLimitEvent) {
    }

    @FromListenerAdapter
    suspend fun onVoiceChannelUpdateBitrate(event: VoiceChannelUpdateBitrateEvent) {
    }

    @FromListenerAdapter
    suspend fun onVoiceChannelUpdatePermissions(event: VoiceChannelUpdatePermissionsEvent) {
    }

    @FromListenerAdapter
    suspend fun onVoiceChannelUpdateParent(event: VoiceChannelUpdateParentEvent) {
    }

    @FromListenerAdapter
    suspend fun onVoiceChannelCreate(event: VoiceChannelCreateEvent) {
    }

    @FromListenerAdapter
    suspend fun onCategoryDelete(event: CategoryDeleteEvent) {
    }

    @FromListenerAdapter
    suspend fun onCategoryUpdateName(event: CategoryUpdateNameEvent) {
    }

    @FromListenerAdapter
    suspend fun onCategoryUpdatePosition(event: CategoryUpdatePositionEvent) {
    }

    @FromListenerAdapter
    suspend fun onCategoryUpdatePermissions(event: CategoryUpdatePermissionsEvent) {
    }

    @FromListenerAdapter
    suspend fun onCategoryCreate(event: CategoryCreateEvent) {
    }

    @FromListenerAdapter
    suspend fun onPrivateChannelCreate(event: PrivateChannelCreateEvent) {
    }

    @FromListenerAdapter
    suspend fun onPrivateChannelDelete(event: PrivateChannelDeleteEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildReady(event: GuildReadyEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildJoin(event: GuildJoinEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildLeave(event: GuildLeaveEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildAvailable(event: GuildAvailableEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildUnavailable(event: GuildUnavailableEvent) {
    }

    @FromListenerAdapter
    suspend fun onUnavailableGuildJoined(event: UnavailableGuildJoinedEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildBan(event: GuildBanEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildUnban(event: GuildUnbanEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildUpdateAfkChannel(event: GuildUpdateAfkChannelEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildUpdateSystemChannel(event: GuildUpdateSystemChannelEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildUpdateAfkTimeout(event: GuildUpdateAfkTimeoutEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildUpdateExplicitContentLevel(event: GuildUpdateExplicitContentLevelEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildUpdateIcon(event: GuildUpdateIconEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildUpdateMFALevel(event: GuildUpdateMFALevelEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildUpdateName(event: GuildUpdateNameEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildUpdateNotificationLevel(event: GuildUpdateNotificationLevelEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildUpdateOwner(event: GuildUpdateOwnerEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildUpdateRegion(event: GuildUpdateRegionEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildUpdateSplash(event: GuildUpdateSplashEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildUpdateVerificationLevel(event: GuildUpdateVerificationLevelEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildUpdateFeatures(event: GuildUpdateFeaturesEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildMemberLeave(event: GuildMemberLeaveEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildMemberNickChange(event: GuildMemberNickChangeEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildVoiceMute(event: GuildVoiceMuteEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildVoiceDeafen(event: GuildVoiceDeafenEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildVoiceGuildMute(event: GuildVoiceGuildMuteEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildVoiceGuildDeafen(event: GuildVoiceGuildDeafenEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildVoiceSelfMute(event: GuildVoiceSelfMuteEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildVoiceSelfDeafen(event: GuildVoiceSelfDeafenEvent) {
    }

    @FromListenerAdapter
    suspend fun onGuildVoiceSuppress(event: GuildVoiceSuppressEvent) {
    }

    @FromListenerAdapter
    suspend fun onRoleCreate(event: RoleCreateEvent) {
    }

    @FromListenerAdapter
    suspend fun onRoleDelete(event: RoleDeleteEvent) {
    }

    @FromListenerAdapter
    suspend fun onRoleUpdateColor(event: RoleUpdateColorEvent) {
    }

    @FromListenerAdapter
    suspend fun onRoleUpdateHoisted(event: RoleUpdateHoistedEvent) {
    }

    @FromListenerAdapter
    suspend fun onRoleUpdateMentionable(event: RoleUpdateMentionableEvent) {
    }

    @FromListenerAdapter
    suspend fun onRoleUpdateName(event: RoleUpdateNameEvent) {
    }

    @FromListenerAdapter
    suspend fun onRoleUpdatePermissions(event: RoleUpdatePermissionsEvent) {
    }

    @FromListenerAdapter
    suspend fun onRoleUpdatePosition(event: RoleUpdatePositionEvent) {
    }

    @FromListenerAdapter
    suspend fun onEmoteAdded(event: EmoteAddedEvent) {
    }

    @FromListenerAdapter
    suspend fun onEmoteRemoved(event: EmoteRemovedEvent) {
    }

    @FromListenerAdapter
    suspend fun onEmoteUpdateName(event: EmoteUpdateNameEvent) {
    }

    @FromListenerAdapter
    suspend fun onEmoteUpdateRoles(event: EmoteUpdateRolesEvent) {
    }

    @FromListenerAdapter
    suspend fun onHttpRequest(event: HttpRequestEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericMessage(event: GenericMessageEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericMessageReaction(event: GenericMessageReactionEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericGuildMessage(event: GenericGuildMessageEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericGuildMessageReaction(event: GenericGuildMessageReactionEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericPrivateMessage(event: GenericPrivateMessageEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericPrivateMessageReaction(event: GenericPrivateMessageReactionEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericUser(event: GenericUserEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericUserPresence(event: GenericUserPresenceEvent<*>) {
    }

    @FromListenerAdapter
    suspend fun onGenericSelfUpdate(event: GenericSelfUpdateEvent<*>) {
    }

    @FromListenerAdapter
    suspend fun onGenericTextChannel(event: GenericTextChannelEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericTextChannelUpdate(event: GenericTextChannelUpdateEvent<*>) {
    }

    @FromListenerAdapter
    suspend fun onGenericVoiceChannel(event: GenericVoiceChannelEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericVoiceChannelUpdate(event: GenericVoiceChannelUpdateEvent<*>) {
    }

    @FromListenerAdapter
    suspend fun onGenericCategory(event: GenericCategoryEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericCategoryUpdate(event: GenericCategoryUpdateEvent<*>) {
    }

    @FromListenerAdapter
    suspend fun onGenericGuild(event: GenericGuildEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericGuildUpdate(event: GenericGuildUpdateEvent<*>) {
    }

    @FromListenerAdapter
    suspend fun onGenericGuildMember(event: GenericGuildMemberEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericGuildVoice(event: GenericGuildVoiceEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericRole(event: GenericRoleEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericRoleUpdate(event: GenericRoleUpdateEvent<*>) {
    }

    @FromListenerAdapter
    suspend fun onGenericEmote(event: GenericEmoteEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericEmoteUpdate(event: GenericEmoteUpdateEvent<*>) {
    }

    @FromListenerAdapter
    suspend fun onFriendAdded(event: FriendAddedEvent) {
    }

    @FromListenerAdapter
    suspend fun onFriendRemoved(event: FriendRemovedEvent) {
    }

    @FromListenerAdapter
    suspend fun onUserBlocked(event: UserBlockedEvent) {
    }

    @FromListenerAdapter
    suspend fun onUserUnblocked(event: UserUnblockedEvent) {
    }

    @FromListenerAdapter
    suspend fun onFriendRequestSent(event: FriendRequestSentEvent) {
    }

    @FromListenerAdapter
    suspend fun onFriendRequestCanceled(event: FriendRequestCanceledEvent) {
    }

    @FromListenerAdapter
    suspend fun onFriendRequestReceived(event: FriendRequestReceivedEvent) {
    }

    @FromListenerAdapter
    suspend fun onFriendRequestIgnored(event: FriendRequestIgnoredEvent) {
    }

    @FromListenerAdapter
    suspend fun onGroupJoin(event: GroupJoinEvent) {
    }

    @FromListenerAdapter
    suspend fun onGroupLeave(event: GroupLeaveEvent) {
    }

    @FromListenerAdapter
    suspend fun onGroupUserJoin(event: GroupUserJoinEvent) {
    }

    @FromListenerAdapter
    suspend fun onGroupUserLeave(event: GroupUserLeaveEvent) {
    }

    @FromListenerAdapter
    suspend fun onGroupMessageReceived(event: GroupMessageReceivedEvent) {
    }

    @FromListenerAdapter
    suspend fun onGroupMessageUpdate(event: GroupMessageUpdateEvent) {
    }

    @FromListenerAdapter
    suspend fun onGroupMessageDelete(event: GroupMessageDeleteEvent) {
    }

    @FromListenerAdapter
    suspend fun onGroupMessageEmbed(event: GroupMessageEmbedEvent) {
    }

    @FromListenerAdapter
    suspend fun onGroupMessageReactionAdd(event: GroupMessageReactionAddEvent) {
    }

    @FromListenerAdapter
    suspend fun onGroupMessageReactionRemove(event: GroupMessageReactionRemoveEvent) {
    }

    @FromListenerAdapter
    suspend fun onGroupMessageReactionRemoveAll(event: GroupMessageReactionRemoveAllEvent) {
    }

    @FromListenerAdapter
    suspend fun onGroupUpdateIcon(event: GroupUpdateIconEvent) {
    }

    @FromListenerAdapter
    suspend fun onGroupUpdateName(event: GroupUpdateNameEvent) {
    }

    @FromListenerAdapter
    suspend fun onGroupUpdateOwner(event: GroupUpdateOwnerEvent) {
    }

    @FromListenerAdapter
    suspend fun onCallCreate(event: CallCreateEvent) {
    }

    @FromListenerAdapter
    suspend fun onCallDelete(event: CallDeleteEvent) {
    }

    @FromListenerAdapter
    suspend fun onCallUpdateRegion(event: CallUpdateRegionEvent) {
    }

    @FromListenerAdapter
    suspend fun onCallUpdateRingingUsers(event: CallUpdateRingingUsersEvent) {
    }

    @FromListenerAdapter
    suspend fun onCallVoiceJoin(event: CallVoiceJoinEvent) {
    }

    @FromListenerAdapter
    suspend fun onCallVoiceLeave(event: CallVoiceLeaveEvent) {
    }

    @FromListenerAdapter
    suspend fun onCallVoiceSelfMute(event: CallVoiceSelfMuteEvent) {
    }

    @FromListenerAdapter
    suspend fun onCallVoiceSelfDeafen(event: CallVoiceSelfDeafenEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericRelationship(event: GenericRelationshipEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericRelationshipAdd(event: GenericRelationshipAddEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericRelationshipRemove(event: GenericRelationshipRemoveEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericGroup(event: GenericGroupEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericGroupMessage(event: GenericGroupMessageEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericGroupMessageReaction(event: GenericGroupMessageReactionEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericGroupUpdate(event: GenericGroupUpdateEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericCall(event: GenericCallEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericCallUpdate(event: GenericCallUpdateEvent) {
    }

    @FromListenerAdapter
    suspend fun onGenericCallVoice(event: GenericCallVoiceEvent) {
    }

    @FromListenerAdapter
    suspend fun onEvent(event: Event) {
    }

    /* ConnectionListener */
    @FromConnectionListener
    suspend fun onPing(guild: Guild, ping: Long) {
    }

    @FromConnectionListener
    suspend fun onStatusChange(guild: Guild, status: ConnectionStatus) {
    }

    @FromConnectionListener
    suspend fun onUserSpeaking(guild: Guild, user: User, speaking: Boolean) {
    }

    @FromConnectionListener
    suspend fun onUserSpeaking(guild: Guild, user: User, modes: EnumSet<SpeakingMode>) {
    }

    @FromConnectionListener
    suspend fun onUserSpeaking(guild: Guild, user: User, speaking: Boolean, soundshare: Boolean) {
    }

    /* AudioEventAdapter */
    @FromAudioEventAdapter
    suspend fun onPlayerPause(guildPlayer: GuildPlayer, player: AudioPlayer) {
    }

    @FromAudioEventAdapter
    suspend fun onPlayerResume(guildPlayer: GuildPlayer, player: AudioPlayer) {
    }

    @FromAudioEventAdapter
    suspend fun onTrackStart(guildPlayer: GuildPlayer, player: AudioPlayer, track: AudioTrack) {
    }

    @FromAudioEventAdapter
    suspend fun onTrackEnd(guildPlayer: GuildPlayer, player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
    }

    @FromAudioEventAdapter
    suspend fun onTrackException(guildPlayer: GuildPlayer, player: AudioPlayer, track: AudioTrack, exception: FriendlyException) {
    }

    @FromAudioEventAdapter
    suspend fun onTrackStuck(guildPlayer: GuildPlayer, player: AudioPlayer, track: AudioTrack, thresholdMs: Long) {
    }

    @FromAudioEventAdapter
    suspend fun onEvent(guildPlayer: GuildPlayer, event: AudioEvent) {
    }

    /* AudioReceiveHandler */
    @FromAudioReceiveHandler
    suspend fun onCombinedAudio(guildPlayer: GuildPlayer, combinedAudio: CombinedAudio) {
    }

    @FromAudioReceiveHandler
    suspend fun onUserAudio(guildPlayer: GuildPlayer, userAudio: UserAudio) {
    }

    /* Tweetstorm */
    @FromTweetstorm
    suspend fun onTweetstormConnect(event: Plugin.Tweetstorm.ConnectEvent) {
    }

    @FromTweetstorm
    suspend fun onTweetstormDisconnect(event: Plugin.Tweetstorm.DisconnectEvent) {
    }

    @FromTweetstorm
    suspend fun onTweetstormStatus(event: Plugin.Tweetstorm.StatusEvent) {
    }

    @FromTweetstorm
    suspend fun onTweetstormDirectMessage(event: Plugin.Tweetstorm.DirectMessageEvent) {
    }

    @FromTweetstorm
    suspend fun onTweetstormFriends(event: Plugin.Tweetstorm.FriendsEvent) {
    }

    @FromTweetstorm
    suspend fun onTweetstormDelete(event: Plugin.Tweetstorm.DeleteEvent) {
    }

    @FromTweetstorm
    suspend fun onTweetstormHeartbeat(event: Plugin.Tweetstorm.HeartbeatEvent) {
    }

    @FromTweetstorm
    suspend fun onTweetstormLength(event: Plugin.Tweetstorm.LengthEvent) {
    }

    @FromTweetstorm
    suspend fun onTweetstormAnyJson(event: Plugin.Tweetstorm.AnyJsonEvent) {
    }

    @FromTweetstorm
    suspend fun onTweetstormUnhandledJson(event: Plugin.Tweetstorm.UnhandledJsonEvent) {
    }

    @FromTweetstorm
    suspend fun onTweetstormUnknownData(event: Plugin.Tweetstorm.UnknownDataEvent) {
    }

    @FromTweetstorm
    suspend fun onTweetstormRawData(event: Plugin.Tweetstorm.RawDataEvent) {
    }

    /* Web */
    @FromWebPage
    suspend fun onAccess(event: Plugin.Web.AccessEvent) {
    }

    @FromWebErrorPage
    suspend fun onError(event: Plugin.Web.AccessEvent, status: HttpStatusCode) {
    }

    @FromWebSession
    suspend fun onCookieSetup(builder: CookieSessionBuilder<Any>) {
    }
}
