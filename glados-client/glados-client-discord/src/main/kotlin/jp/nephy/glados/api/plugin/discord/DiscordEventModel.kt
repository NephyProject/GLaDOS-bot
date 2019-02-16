package jp.nephy.glados.api.plugin.discord

import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import jp.nephy.glados.api.event.EventModel
import net.dv8tion.jda.api.audio.CombinedAudio
import net.dv8tion.jda.api.audio.SpeakingMode
import net.dv8tion.jda.api.audio.UserAudio
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.*
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateNameEvent
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdatePositionEvent
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateTopicEvent
import net.dv8tion.jda.api.events.message.*
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageEmbedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveAllEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent
import net.dv8tion.jda.api.events.message.priv.PrivateMessageDeleteEvent
import net.dv8tion.jda.api.events.message.priv.PrivateMessageEmbedEvent
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.api.events.message.priv.PrivateMessageUpdateEvent
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionRemoveEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveAllEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent
import net.dv8tion.jda.api.events.self.*
import net.dv8tion.jda.api.events.user.UserTypingEvent
import net.dv8tion.jda.api.events.user.update.UserUpdateAvatarEvent
import net.dv8tion.jda.api.events.user.update.UserUpdateDiscriminatorEvent
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent
import java.util.*

interface DiscordEventModel: EventModel {
    /* ListenerAdapter */
    
    suspend fun onGenericEvent(event: Event) {
    }

    
    suspend fun onGenericUpdate(event: UpdateEvent<*, *>) {
    }

    
    suspend fun onReady(event: ReadyEvent) {
    }

    
    suspend fun onResume(event: ResumedEvent) {
    }

    
    suspend fun onReconnect(event: ReconnectedEvent) {
    }

    
    suspend fun onDisconnect(event: DisconnectEvent) {
    }

    
    suspend fun onShutdown(event: ShutdownEvent) {
    }

    
    suspend fun onStatusChange(event: StatusChangeEvent) {
    }

    
    suspend fun onException(event: ExceptionEvent) {
    }

    
    suspend fun onUserUpdateName(event: UserUpdateNameEvent) {
    }

    
    suspend fun onUserUpdateDiscriminator(event: UserUpdateDiscriminatorEvent) {
    }

    
    suspend fun onUserUpdateAvatar(event: UserUpdateAvatarEvent) {
    }

    
    suspend fun onUserUpdateOnlineStatus(event: UserUpdateOnlineStatusEvent) {
    }

    
    suspend fun onUserUpdateGame(event: UserUpdateGameEvent) {
    }

    
    suspend fun onUserTyping(event: UserTypingEvent) {
    }

    
    suspend fun onSelfUpdateAvatar(event: SelfUpdateAvatarEvent) {
    }

    
    suspend fun onSelfUpdateEmail(event: SelfUpdateEmailEvent) {
    }

    
    suspend fun onSelfUpdateMFA(event: SelfUpdateMFAEvent) {
    }

    
    suspend fun onSelfUpdateName(event: SelfUpdateNameEvent) {
    }

    
    suspend fun onSelfUpdateVerified(event: SelfUpdateVerifiedEvent) {
    }

    
    suspend fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
    }

    
    suspend fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) {
    }

    
    suspend fun onGuildMessageDelete(event: GuildMessageDeleteEvent) {
    }

    
    suspend fun onGuildMessageEmbed(event: GuildMessageEmbedEvent) {
    }

    
    suspend fun onGuildMessageReactionAdd(event: GuildMessageReactionAddEvent) {
    }

    
    suspend fun onGuildMessageReactionRemove(event: GuildMessageReactionRemoveEvent) {
    }

    
    suspend fun onGuildMessageReactionRemoveAll(event: GuildMessageReactionRemoveAllEvent) {
    }

    
    suspend fun onPrivateMessageReceived(event: PrivateMessageReceivedEvent) {
    }

    
    suspend fun onPrivateMessageUpdate(event: PrivateMessageUpdateEvent) {
    }

    
    suspend fun onPrivateMessageDelete(event: PrivateMessageDeleteEvent) {
    }

    
    suspend fun onPrivateMessageEmbed(event: PrivateMessageEmbedEvent) {
    }

    
    suspend fun onPrivateMessageReactionAdd(event: PrivateMessageReactionAddEvent) {
    }

    
    suspend fun onPrivateMessageReactionRemove(event: PrivateMessageReactionRemoveEvent) {
    }

    
    suspend fun onMessageReceived(event: MessageReceivedEvent) {
    }

    
    suspend fun onMessageUpdate(event: MessageUpdateEvent) {
    }

    
    suspend fun onMessageDelete(event: MessageDeleteEvent) {
    }

    
    suspend fun onMessageBulkDelete(event: MessageBulkDeleteEvent) {
    }

    
    suspend fun onMessageEmbed(event: MessageEmbedEvent) {
    }

    
    suspend fun onMessageReactionAdd(event: MessageReactionAddEvent) {
    }

    
    suspend fun onMessageReactionRemove(event: MessageReactionRemoveEvent) {
    }

    
    suspend fun onMessageReactionRemoveAll(event: MessageReactionRemoveAllEvent) {
    }

    
    suspend fun onTextChannelDelete(event: TextChannelDeleteEvent) {
    }

    
    suspend fun onTextChannelUpdateName(event: TextChannelUpdateNameEvent) {
    }

    
    suspend fun onTextChannelUpdateTopic(event: TextChannelUpdateTopicEvent) {
    }

    
    suspend fun onTextChannelUpdatePosition(event: TextChannelUpdatePositionEvent) {
    }

    
    suspend fun onTextChannelUpdatePermissions(event: TextChannelUpdatePermissionsEvent) {
    }

    
    suspend fun onTextChannelUpdateNSFW(event: TextChannelUpdateNSFWEvent) {
    }

    
    suspend fun onTextChannelUpdateParent(event: TextChannelUpdateParentEvent) {
    }

    
    suspend fun onTextChannelUpdateSlowmode(event: TextChannelUpdateSlowmodeEvent) {
    }

    
    suspend fun onTextChannelCreate(event: TextChannelCreateEvent) {
    }

    
    suspend fun onVoiceChannelDelete(event: VoiceChannelDeleteEvent) {
    }

    
    suspend fun onVoiceChannelUpdateName(event: VoiceChannelUpdateNameEvent) {
    }

    
    suspend fun onVoiceChannelUpdatePosition(event: VoiceChannelUpdatePositionEvent) {
    }

    
    suspend fun onVoiceChannelUpdateUserLimit(event: VoiceChannelUpdateUserLimitEvent) {
    }

    
    suspend fun onVoiceChannelUpdateBitrate(event: VoiceChannelUpdateBitrateEvent) {
    }

    
    suspend fun onVoiceChannelUpdatePermissions(event: VoiceChannelUpdatePermissionsEvent) {
    }

    
    suspend fun onVoiceChannelUpdateParent(event: VoiceChannelUpdateParentEvent) {
    }

    
    suspend fun onVoiceChannelCreate(event: VoiceChannelCreateEvent) {
    }

    
    suspend fun onCategoryDelete(event: CategoryDeleteEvent) {
    }

    
    suspend fun onCategoryUpdateName(event: CategoryUpdateNameEvent) {
    }

    
    suspend fun onCategoryUpdatePosition(event: CategoryUpdatePositionEvent) {
    }

    
    suspend fun onCategoryUpdatePermissions(event: CategoryUpdatePermissionsEvent) {
    }

    
    suspend fun onCategoryCreate(event: CategoryCreateEvent) {
    }

    
    suspend fun onPrivateChannelCreate(event: PrivateChannelCreateEvent) {
    }

    
    suspend fun onPrivateChannelDelete(event: PrivateChannelDeleteEvent) {
    }

    
    suspend fun onGuildReady(event: GuildReadyEvent) {
    }

    
    suspend fun onGuildJoin(event: GuildJoinEvent) {
    }

    
    suspend fun onGuildLeave(event: GuildLeaveEvent) {
    }

    
    suspend fun onGuildAvailable(event: GuildAvailableEvent) {
    }

    
    suspend fun onGuildUnavailable(event: GuildUnavailableEvent) {
    }

    
    suspend fun onUnavailableGuildJoined(event: UnavailableGuildJoinedEvent) {
    }

    
    suspend fun onGuildBan(event: GuildBanEvent) {
    }

    
    suspend fun onGuildUnban(event: GuildUnbanEvent) {
    }

    
    suspend fun onGuildUpdateAfkChannel(event: GuildUpdateAfkChannelEvent) {
    }

    
    suspend fun onGuildUpdateSystemChannel(event: GuildUpdateSystemChannelEvent) {
    }

    
    suspend fun onGuildUpdateAfkTimeout(event: GuildUpdateAfkTimeoutEvent) {
    }

    
    suspend fun onGuildUpdateExplicitContentLevel(event: GuildUpdateExplicitContentLevelEvent) {
    }

    
    suspend fun onGuildUpdateIcon(event: GuildUpdateIconEvent) {
    }

    
    suspend fun onGuildUpdateMFALevel(event: GuildUpdateMFALevelEvent) {
    }

    
    suspend fun onGuildUpdateName(event: GuildUpdateNameEvent) {
    }

    
    suspend fun onGuildUpdateNotificationLevel(event: GuildUpdateNotificationLevelEvent) {
    }

    
    suspend fun onGuildUpdateOwner(event: GuildUpdateOwnerEvent) {
    }

    
    suspend fun onGuildUpdateRegion(event: GuildUpdateRegionEvent) {
    }

    
    suspend fun onGuildUpdateSplash(event: GuildUpdateSplashEvent) {
    }

    
    suspend fun onGuildUpdateVerificationLevel(event: GuildUpdateVerificationLevelEvent) {
    }

    
    suspend fun onGuildUpdateFeatures(event: GuildUpdateFeaturesEvent) {
    }

    
    suspend fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
    }

    
    suspend fun onGuildMemberLeave(event: GuildMemberLeaveEvent) {
    }

    
    suspend fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent) {
    }

    
    suspend fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {
    }

    
    suspend fun onGuildMemberNickChange(event: GuildMemberNickChangeEvent) {
    }

    
    suspend fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
    }

    
    suspend fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
    }

    
    suspend fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
    }

    
    suspend fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
    }

    
    suspend fun onGuildVoiceMute(event: GuildVoiceMuteEvent) {
    }

    
    suspend fun onGuildVoiceDeafen(event: GuildVoiceDeafenEvent) {
    }

    
    suspend fun onGuildVoiceGuildMute(event: GuildVoiceGuildMuteEvent) {
    }

    
    suspend fun onGuildVoiceGuildDeafen(event: GuildVoiceGuildDeafenEvent) {
    }

    
    suspend fun onGuildVoiceSelfMute(event: GuildVoiceSelfMuteEvent) {
    }

    
    suspend fun onGuildVoiceSelfDeafen(event: GuildVoiceSelfDeafenEvent) {
    }

    
    suspend fun onGuildVoiceSuppress(event: GuildVoiceSuppressEvent) {
    }

    
    suspend fun onRoleCreate(event: RoleCreateEvent) {
    }

    
    suspend fun onRoleDelete(event: RoleDeleteEvent) {
    }

    
    suspend fun onRoleUpdateColor(event: RoleUpdateColorEvent) {
    }

    
    suspend fun onRoleUpdateHoisted(event: RoleUpdateHoistedEvent) {
    }

    
    suspend fun onRoleUpdateMentionable(event: RoleUpdateMentionableEvent) {
    }

    
    suspend fun onRoleUpdateName(event: RoleUpdateNameEvent) {
    }

    
    suspend fun onRoleUpdatePermissions(event: RoleUpdatePermissionsEvent) {
    }

    
    suspend fun onRoleUpdatePosition(event: RoleUpdatePositionEvent) {
    }

    
    suspend fun onEmoteAdded(event: EmoteAddedEvent) {
    }

    
    suspend fun onEmoteRemoved(event: EmoteRemovedEvent) {
    }

    
    suspend fun onEmoteUpdateName(event: EmoteUpdateNameEvent) {
    }

    
    suspend fun onEmoteUpdateRoles(event: EmoteUpdateRolesEvent) {
    }

    
    suspend fun onHttpRequest(event: HttpRequestEvent) {
    }

    
    suspend fun onGenericMessage(event: GenericMessageEvent) {
    }

    
    suspend fun onGenericMessageReaction(event: GenericMessageReactionEvent) {
    }

    
    suspend fun onGenericGuildMessage(event: GenericGuildMessageEvent) {
    }

    
    suspend fun onGenericGuildMessageReaction(event: GenericGuildMessageReactionEvent) {
    }

    
    suspend fun onGenericPrivateMessage(event: GenericPrivateMessageEvent) {
    }

    
    suspend fun onGenericPrivateMessageReaction(event: GenericPrivateMessageReactionEvent) {
    }

    
    suspend fun onGenericUser(event: GenericUserEvent) {
    }

    
    suspend fun onGenericUserPresence(event: GenericUserPresenceEvent<*>) {
    }

    
    suspend fun onGenericSelfUpdate(event: GenericSelfUpdateEvent<*>) {
    }

    
    suspend fun onGenericTextChannel(event: GenericTextChannelEvent) {
    }

    
    suspend fun onGenericTextChannelUpdate(event: GenericTextChannelUpdateEvent<*>) {
    }

    
    suspend fun onGenericVoiceChannel(event: GenericVoiceChannelEvent) {
    }

    
    suspend fun onGenericVoiceChannelUpdate(event: GenericVoiceChannelUpdateEvent<*>) {
    }

    
    suspend fun onGenericCategory(event: GenericCategoryEvent) {
    }

    
    suspend fun onGenericCategoryUpdate(event: GenericCategoryUpdateEvent<*>) {
    }

    
    suspend fun onGenericGuild(event: GenericGuildEvent) {
    }

    
    suspend fun onGenericGuildUpdate(event: GenericGuildUpdateEvent<*>) {
    }

    
    suspend fun onGenericGuildMember(event: GenericGuildMemberEvent) {
    }

    
    suspend fun onGenericGuildVoice(event: GenericGuildVoiceEvent) {
    }

    
    suspend fun onGenericRole(event: GenericRoleEvent) {
    }

    
    suspend fun onGenericRoleUpdate(event: GenericRoleUpdateEvent<*>) {
    }

    
    suspend fun onGenericEmote(event: GenericEmoteEvent) {
    }

    
    suspend fun onGenericEmoteUpdate(event: GenericEmoteUpdateEvent<*>) {
    }

    
    suspend fun onFriendAdded(event: FriendAddedEvent) {
    }

    
    suspend fun onFriendRemoved(event: FriendRemovedEvent) {
    }

    
    suspend fun onUserBlocked(event: UserBlockedEvent) {
    }

    
    suspend fun onUserUnblocked(event: UserUnblockedEvent) {
    }

    
    suspend fun onFriendRequestSent(event: FriendRequestSentEvent) {
    }

    
    suspend fun onFriendRequestCanceled(event: FriendRequestCanceledEvent) {
    }

    
    suspend fun onFriendRequestReceived(event: FriendRequestReceivedEvent) {
    }

    
    suspend fun onFriendRequestIgnored(event: FriendRequestIgnoredEvent) {
    }

    
    suspend fun onGroupJoin(event: GroupJoinEvent) {
    }

    
    suspend fun onGroupLeave(event: GroupLeaveEvent) {
    }

    
    suspend fun onGroupUserJoin(event: GroupUserJoinEvent) {
    }

    
    suspend fun onGroupUserLeave(event: GroupUserLeaveEvent) {
    }

    
    suspend fun onGroupMessageReceived(event: GroupMessageReceivedEvent) {
    }

    
    suspend fun onGroupMessageUpdate(event: GroupMessageUpdateEvent) {
    }

    
    suspend fun onGroupMessageDelete(event: GroupMessageDeleteEvent) {
    }

    
    suspend fun onGroupMessageEmbed(event: GroupMessageEmbedEvent) {
    }

    
    suspend fun onGroupMessageReactionAdd(event: GroupMessageReactionAddEvent) {
    }

    
    suspend fun onGroupMessageReactionRemove(event: GroupMessageReactionRemoveEvent) {
    }

    
    suspend fun onGroupMessageReactionRemoveAll(event: GroupMessageReactionRemoveAllEvent) {
    }

    
    suspend fun onGroupUpdateIcon(event: GroupUpdateIconEvent) {
    }

    
    suspend fun onGroupUpdateName(event: GroupUpdateNameEvent) {
    }

    
    suspend fun onGroupUpdateOwner(event: GroupUpdateOwnerEvent) {
    }

    
    suspend fun onCallCreate(event: CallCreateEvent) {
    }

    
    suspend fun onCallDelete(event: CallDeleteEvent) {
    }

    
    suspend fun onCallUpdateRegion(event: CallUpdateRegionEvent) {
    }

    
    suspend fun onCallUpdateRingingUsers(event: CallUpdateRingingUsersEvent) {
    }

    
    suspend fun onCallVoiceJoin(event: CallVoiceJoinEvent) {
    }

    
    suspend fun onCallVoiceLeave(event: CallVoiceLeaveEvent) {
    }

    
    suspend fun onCallVoiceSelfMute(event: CallVoiceSelfMuteEvent) {
    }

    
    suspend fun onCallVoiceSelfDeafen(event: CallVoiceSelfDeafenEvent) {
    }

    
    suspend fun onGenericRelationship(event: GenericRelationshipEvent) {
    }

    
    suspend fun onGenericRelationshipAdd(event: GenericRelationshipAddEvent) {
    }

    
    suspend fun onGenericRelationshipRemove(event: GenericRelationshipRemoveEvent) {
    }

    
    suspend fun onGenericGroup(event: GenericGroupEvent) {
    }

    
    suspend fun onGenericGroupMessage(event: GenericGroupMessageEvent) {
    }

    
    suspend fun onGenericGroupMessageReaction(event: GenericGroupMessageReactionEvent) {
    }

    
    suspend fun onGenericGroupUpdate(event: GenericGroupUpdateEvent) {
    }

    
    suspend fun onGenericCall(event: GenericCallEvent) {
    }

    
    suspend fun onGenericCallUpdate(event: GenericCallUpdateEvent) {
    }

    
    suspend fun onGenericCallVoice(event: GenericCallVoiceEvent) {
    }

    
    suspend fun onEvent(event: Event) {
    }

    /* ConnectionListener */
    
    suspend fun onPing(guild: Guild, ping: Long) {
    }

    
    suspend fun onStatusChange(guild: Guild, status: ConnectionStatus) {
    }

    
    suspend fun onUserSpeaking(guild: Guild, user: User, speaking: Boolean) {
    }

    
    suspend fun onUserSpeaking(guild: Guild, user: User, modes: EnumSet<SpeakingMode>) {
    }

    
    suspend fun onUserSpeaking(guild: Guild, user: User, speaking: Boolean, soundshare: Boolean) {
    }

    /* AudioEventAdapter */
    
    suspend fun onPlayerPause(guildPlayer: GuildPlayer, player: AudioPlayer) {
    }

    
    suspend fun onPlayerResume(guildPlayer: GuildPlayer, player: AudioPlayer) {
    }

    
    suspend fun onTrackStart(guildPlayer: GuildPlayer, player: AudioPlayer, track: AudioTrack) {
    }

    
    suspend fun onTrackEnd(guildPlayer: GuildPlayer, player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
    }

    
    suspend fun onTrackException(guildPlayer: GuildPlayer, player: AudioPlayer, track: AudioTrack, exception: FriendlyException) {
    }

    
    suspend fun onTrackStuck(guildPlayer: GuildPlayer, player: AudioPlayer, track: AudioTrack, thresholdMs: Long) {
    }

    
    suspend fun onEvent(guildPlayer: GuildPlayer, event: AudioEvent) {
    }

    /* AudioReceiveHandler */
    
    suspend fun onCombinedAudio(guildPlayer: GuildPlayer, combinedAudio: CombinedAudio) {
    }

    
    suspend fun onUserAudio(guildPlayer: GuildPlayer, userAudio: UserAudio) {
    }
}
