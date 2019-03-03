/*
 * The MIT License (MIT)
 *
 *     Copyright (c) 2017-2019 Nephy Project Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

@file:Suppress("UNUSED")

package jp.nephy.glados.clients.discord.listener.websocket

import jp.nephy.glados.clients.discord.listener.websocket.events.general.DiscordReadyEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.general.DiscordReconnectedEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.general.DiscordResumedEvent
import jp.nephy.glados.api.EventModel
import net.dv8tion.jda.api.events.DisconnectEvent
import net.dv8tion.jda.api.events.ExceptionEvent
import net.dv8tion.jda.api.events.ShutdownEvent
import net.dv8tion.jda.api.events.StatusChangeEvent
import net.dv8tion.jda.api.events.channel.category.CategoryCreateEvent
import net.dv8tion.jda.api.events.channel.category.CategoryDeleteEvent
import net.dv8tion.jda.api.events.channel.category.update.CategoryUpdateNameEvent
import net.dv8tion.jda.api.events.channel.category.update.CategoryUpdatePermissionsEvent
import net.dv8tion.jda.api.events.channel.category.update.CategoryUpdatePositionEvent
import net.dv8tion.jda.api.events.channel.priv.PrivateChannelCreateEvent
import net.dv8tion.jda.api.events.channel.priv.PrivateChannelDeleteEvent
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent
import net.dv8tion.jda.api.events.channel.text.update.*
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelCreateEvent
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelDeleteEvent
import net.dv8tion.jda.api.events.channel.voice.update.*
import net.dv8tion.jda.api.events.emote.EmoteAddedEvent
import net.dv8tion.jda.api.events.emote.EmoteRemovedEvent
import net.dv8tion.jda.api.events.emote.update.EmoteUpdateNameEvent
import net.dv8tion.jda.api.events.emote.update.EmoteUpdateRolesEvent
import net.dv8tion.jda.api.events.guild.*
import net.dv8tion.jda.api.events.guild.member.*
import net.dv8tion.jda.api.events.guild.update.*
import net.dv8tion.jda.api.events.guild.voice.*
import net.dv8tion.jda.api.events.http.HttpRequestEvent
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
import net.dv8tion.jda.api.events.role.RoleCreateEvent
import net.dv8tion.jda.api.events.role.RoleDeleteEvent
import net.dv8tion.jda.api.events.role.update.*
import net.dv8tion.jda.api.events.self.*
import net.dv8tion.jda.api.events.user.UserActivityEndEvent
import net.dv8tion.jda.api.events.user.UserActivityStartEvent
import net.dv8tion.jda.api.events.user.UserTypingEvent
import net.dv8tion.jda.api.events.user.update.*

interface ListenerAdapterModel: EventModel {
    suspend fun onDiscordReady(event: DiscordReadyEvent) {}
    suspend fun onDiscordResume(event: DiscordResumedEvent) {}
    suspend fun onDiscordReconnect(event: DiscordReconnectedEvent) {}
    suspend fun onDiscordDisconnect(event: DisconnectEvent) {}
    suspend fun onDiscordShutdown(event: ShutdownEvent) {}
    suspend fun onDiscordStatusChange(event: StatusChangeEvent) {}
    suspend fun onDiscordException(event: ExceptionEvent) {}
    suspend fun onDiscordHttpRequest(event: HttpRequestEvent) {}
    
    suspend fun onDiscordUserUpdateName(event: UserUpdateNameEvent) {}
    suspend fun onDiscordUserUpdateDiscriminator(event: UserUpdateDiscriminatorEvent) {}
    suspend fun onDiscordUserUpdateAvatar(event: UserUpdateAvatarEvent) {}
    suspend fun onDiscordUserUpdateOnlineStatus(event: UserUpdateOnlineStatusEvent) {}
    suspend fun onDiscordUserUpdateActivityOrder(event: UserUpdateActivityOrderEvent) {}
    suspend fun onDiscordUserTyping(event: UserTypingEvent) {}
    suspend fun onDiscordUserActivityStart(event: UserActivityStartEvent) {}
    suspend fun onDiscordUserActivityEnd(event: UserActivityEndEvent) {}

    suspend fun onDiscordSelfUpdateAvatar(event: SelfUpdateAvatarEvent) {}
    suspend fun onDiscordSelfUpdateEmail(event: SelfUpdateEmailEvent) {}
    suspend fun onDiscordSelfUpdateMFA(event: SelfUpdateMFAEvent) {}
    suspend fun onDiscordSelfUpdateName(event: SelfUpdateNameEvent) {}
    suspend fun onDiscordSelfUpdateVerified(event: SelfUpdateVerifiedEvent) {}
    
    suspend fun onDiscordGuildMessageReceived(event: GuildMessageReceivedEvent) {}
    suspend fun onDiscordGuildMessageUpdate(event: GuildMessageUpdateEvent) {}
    suspend fun onDiscordGuildMessageDelete(event: GuildMessageDeleteEvent) {}
    suspend fun onDiscordGuildMessageEmbed(event: GuildMessageEmbedEvent) {}
    suspend fun onDiscordGuildMessageReactionAdd(event: GuildMessageReactionAddEvent) {}
    suspend fun onDiscordGuildMessageReactionRemove(event: GuildMessageReactionRemoveEvent) {}
    suspend fun onDiscordGuildMessageReactionRemoveAll(event: GuildMessageReactionRemoveAllEvent) {}
    
    suspend fun onDiscordPrivateMessageReceived(event: PrivateMessageReceivedEvent) {}
    suspend fun onDiscordPrivateMessageUpdate(event: PrivateMessageUpdateEvent) {}
    suspend fun onDiscordPrivateMessageDelete(event: PrivateMessageDeleteEvent) {}
    suspend fun onDiscordPrivateMessageEmbed(event: PrivateMessageEmbedEvent) {}
    suspend fun onDiscordPrivateMessageReactionAdd(event: PrivateMessageReactionAddEvent) {}
    suspend fun onDiscordPrivateMessageReactionRemove(event: PrivateMessageReactionRemoveEvent) {}
    
    suspend fun onDiscordMessageReceived(event: MessageReceivedEvent) {}
    suspend fun onDiscordMessageUpdate(event: MessageUpdateEvent) {}
    suspend fun onDiscordMessageDelete(event: MessageDeleteEvent) {}
    suspend fun onDiscordMessageBulkDelete(event: MessageBulkDeleteEvent) {}
    suspend fun onDiscordMessageEmbed(event: MessageEmbedEvent) {}
    suspend fun onDiscordMessageReactionAdd(event: MessageReactionAddEvent) {}
    suspend fun onDiscordMessageReactionRemove(event: MessageReactionRemoveEvent) {}
    suspend fun onDiscordMessageReactionRemoveAll(event: MessageReactionRemoveAllEvent) {}
    
    suspend fun onDiscordTextChannelDelete(event: TextChannelDeleteEvent) {}
    suspend fun onDiscordTextChannelUpdateName(event: TextChannelUpdateNameEvent) {}
    suspend fun onDiscordTextChannelUpdateTopic(event: TextChannelUpdateTopicEvent) {}
    suspend fun onDiscordTextChannelUpdatePosition(event: TextChannelUpdatePositionEvent) {}
    suspend fun onDiscordTextChannelUpdatePermissions(event: TextChannelUpdatePermissionsEvent) {}
    suspend fun onDiscordTextChannelUpdateNSFW(event: TextChannelUpdateNSFWEvent) {}
    suspend fun onDiscordTextChannelUpdateParent(event: TextChannelUpdateParentEvent) {}
    suspend fun onDiscordTextChannelUpdateSlowmode(event: TextChannelUpdateSlowmodeEvent) {}
    suspend fun onDiscordTextChannelCreate(event: TextChannelCreateEvent) {}
    
    suspend fun onDiscordVoiceChannelDelete(event: VoiceChannelDeleteEvent) {}
    suspend fun onDiscordVoiceChannelUpdateName(event: VoiceChannelUpdateNameEvent) {}
    suspend fun onDiscordVoiceChannelUpdatePosition(event: VoiceChannelUpdatePositionEvent) {}
    suspend fun onDiscordVoiceChannelUpdateUserLimit(event: VoiceChannelUpdateUserLimitEvent) {}
    suspend fun onDiscordVoiceChannelUpdateBitrate(event: VoiceChannelUpdateBitrateEvent) {}
    suspend fun onDiscordVoiceChannelUpdatePermissions(event: VoiceChannelUpdatePermissionsEvent) {}
    suspend fun onDiscordVoiceChannelUpdateParent(event: VoiceChannelUpdateParentEvent) {}
    suspend fun onDiscordVoiceChannelCreate(event: VoiceChannelCreateEvent) {}
    
    suspend fun onDiscordCategoryDelete(event: CategoryDeleteEvent) {}
    suspend fun onDiscordCategoryUpdateName(event: CategoryUpdateNameEvent) {}
    suspend fun onDiscordCategoryUpdatePosition(event: CategoryUpdatePositionEvent) {}
    suspend fun onDiscordCategoryUpdatePermissions(event: CategoryUpdatePermissionsEvent) {}
    suspend fun onDiscordCategoryCreate(event: CategoryCreateEvent) {}
    
    suspend fun onDiscordPrivateChannelCreate(event: PrivateChannelCreateEvent) {}
    suspend fun onDiscordPrivateChannelDelete(event: PrivateChannelDeleteEvent) {}
    
    suspend fun onDiscordGuildReady(event: GuildReadyEvent) {}
    suspend fun onDiscordGuildJoin(event: GuildJoinEvent) {}
    suspend fun onDiscordGuildLeave(event: GuildLeaveEvent) {}
    suspend fun onDiscordGuildAvailable(event: GuildAvailableEvent) {}
    suspend fun onDiscordGuildUnavailable(event: GuildUnavailableEvent) {}
    suspend fun onDiscordUnavailableGuildJoined(event: UnavailableGuildJoinedEvent) {}
    suspend fun onDiscordGuildBan(event: GuildBanEvent) {}
    suspend fun onDiscordGuildUnban(event: GuildUnbanEvent) {}
    suspend fun onDiscordGuildUpdateAfkChannel(event: GuildUpdateAfkChannelEvent) {}
    suspend fun onDiscordGuildUpdateSystemChannel(event: GuildUpdateSystemChannelEvent) {}
    suspend fun onDiscordGuildUpdateAfkTimeout(event: GuildUpdateAfkTimeoutEvent) {}
    suspend fun onDiscordGuildUpdateExplicitContentLevel(event: GuildUpdateExplicitContentLevelEvent) {}
    suspend fun onDiscordGuildUpdateIcon(event: GuildUpdateIconEvent) {}
    suspend fun onDiscordGuildUpdateMFALevel(event: GuildUpdateMFALevelEvent) {}
    suspend fun onDiscordGuildUpdateName(event: GuildUpdateNameEvent) {}
    suspend fun onDiscordGuildUpdateNotificationLevel(event: GuildUpdateNotificationLevelEvent) {}
    suspend fun onDiscordGuildUpdateOwner(event: GuildUpdateOwnerEvent) {}
    suspend fun onDiscordGuildUpdateRegion(event: GuildUpdateRegionEvent) {}
    suspend fun onDiscordGuildUpdateSplash(event: GuildUpdateSplashEvent) {}
    suspend fun onDiscordGuildUpdateVerificationLevel(event: GuildUpdateVerificationLevelEvent) {}
    suspend fun onDiscordGuildUpdateFeatures(event: GuildUpdateFeaturesEvent) {}
    
    suspend fun onDiscordGuildMemberJoin(event: GuildMemberJoinEvent) {}
    suspend fun onDiscordGuildMemberLeave(event: GuildMemberLeaveEvent) {}
    suspend fun onDiscordGuildMemberRoleAdd(event: GuildMemberRoleAddEvent) {}
    suspend fun onDiscordGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {}
    suspend fun onDiscordGuildMemberNickChange(event: GuildMemberNickChangeEvent) {}
    
    suspend fun onDiscordGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {}
    suspend fun onDiscordGuildVoiceJoin(event: GuildVoiceJoinEvent) {}
    suspend fun onDiscordGuildVoiceMove(event: GuildVoiceMoveEvent) {}
    suspend fun onDiscordGuildVoiceLeave(event: GuildVoiceLeaveEvent) {}
    suspend fun onDiscordGuildVoiceMute(event: GuildVoiceMuteEvent) {}
    suspend fun onDiscordGuildVoiceDeafen(event: GuildVoiceDeafenEvent) {}
    suspend fun onDiscordGuildVoiceGuildMute(event: GuildVoiceGuildMuteEvent) {}
    suspend fun onDiscordGuildVoiceGuildDeafen(event: GuildVoiceGuildDeafenEvent) {}
    suspend fun onDiscordGuildVoiceSelfMute(event: GuildVoiceSelfMuteEvent) {}
    suspend fun onDiscordGuildVoiceSelfDeafen(event: GuildVoiceSelfDeafenEvent) {}
    suspend fun onDiscordGuildVoiceSuppress(event: GuildVoiceSuppressEvent) {}
    
    suspend fun onDiscordRoleCreate(event: RoleCreateEvent) {}
    suspend fun onDiscordRoleDelete(event: RoleDeleteEvent) {}
    suspend fun onDiscordRoleUpdateColor(event: RoleUpdateColorEvent) {}
    suspend fun onDiscordRoleUpdateHoisted(event: RoleUpdateHoistedEvent) {}
    suspend fun onDiscordRoleUpdateMentionable(event: RoleUpdateMentionableEvent) {}
    suspend fun onDiscordRoleUpdateName(event: RoleUpdateNameEvent) {}
    suspend fun onDiscordRoleUpdatePermissions(event: RoleUpdatePermissionsEvent) {}
    suspend fun onDiscordRoleUpdatePosition(event: RoleUpdatePositionEvent) {}
    
    suspend fun onDiscordEmoteAdded(event: EmoteAddedEvent) {}
    suspend fun onDiscordEmoteRemoved(event: EmoteRemovedEvent) {}
    suspend fun onDiscordEmoteUpdateName(event: EmoteUpdateNameEvent) {}
    suspend fun onDiscordEmoteUpdateRoles(event: EmoteUpdateRolesEvent) {}
}
