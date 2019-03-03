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

package jp.nephy.glados.clients.discord.listener.websocket

import jp.nephy.glados.api.Plugin
import jp.nephy.glados.clients.GLaDOSSubscriptionClient
import jp.nephy.glados.clients.discord.disposeJDA
import jp.nephy.glados.clients.discord.initializeJDA
import jp.nephy.glados.clients.discord.listener.DiscordEvent
import jp.nephy.glados.clients.discord.listener.DiscordEventBase
import jp.nephy.glados.clients.discord.listener.defaultDiscordEventAnnotation
import jp.nephy.glados.clients.discord.listener.websocket.events.DiscordWebsocketEventBase
import jp.nephy.glados.clients.discord.listener.websocket.events.category.DiscordCategoryCreateEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.category.DiscordCategoryDeleteEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.category.update.DiscordCategoryUpdateNameEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.category.update.DiscordCategoryUpdatePermissionsEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.category.update.DiscordCategoryUpdatePositionEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.channel.priv.DiscordPrivateChannelCreateEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.channel.priv.DiscordPrivateChannelDeleteEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.channel.text.DiscordTextChannelCreateEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.channel.text.DiscordTextChannelDeleteEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.channel.text.update.*
import jp.nephy.glados.clients.discord.listener.websocket.events.channel.voice.DiscordVoiceChannelCreateEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.channel.voice.DiscordVoiceChannelDeleteEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.channel.voice.update.*
import jp.nephy.glados.clients.discord.listener.websocket.events.emote.DiscordEmoteAddedEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.emote.DiscordEmoteRemovedEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.emote.update.DiscordEmoteUpdateNameEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.emote.update.DiscordEmoteUpdateRolesEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.general.*
import jp.nephy.glados.clients.discord.listener.websocket.events.guild.*
import jp.nephy.glados.clients.discord.listener.websocket.events.guild.member.*
import jp.nephy.glados.clients.discord.listener.websocket.events.guild.update.*
import jp.nephy.glados.clients.discord.listener.websocket.events.guild.voice.*
import jp.nephy.glados.clients.discord.listener.websocket.events.message.*
import jp.nephy.glados.clients.discord.listener.websocket.events.message.guild.*
import jp.nephy.glados.clients.discord.listener.websocket.events.message.priv.*
import jp.nephy.glados.clients.discord.listener.websocket.events.message.reaction.DiscordMessageReactionAddEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.message.reaction.DiscordMessageReactionRemoveAllEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.message.reaction.DiscordMessageReactionRemoveEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.role.DiscordRoleCreateEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.role.DiscordRoleDeleteEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.role.update.*
import jp.nephy.glados.clients.discord.listener.websocket.events.user.DiscordUserActivityEndEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.user.DiscordUserActivityStartEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.user.DiscordUserTypingEvent
import jp.nephy.glados.clients.discord.listener.websocket.events.user.self.*
import jp.nephy.glados.clients.discord.listener.websocket.events.user.update.*
import jp.nephy.glados.clients.utils.eventClass
import jp.nephy.glados.clients.utils.invoke
import jp.nephy.glados.clients.utils.subscriptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.*
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
import net.dv8tion.jda.api.hooks.SubscribeEvent
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf

object DiscordWebsocketEventSubscriptionClient: GLaDOSSubscriptionClient<DiscordEvent, DiscordWebsocketEventBase<*>, DiscordWebsocketEventSubscription>(), EventListener {
    override fun create(plugin: Plugin, function: KFunction<*>, eventClass: KClass<*>): DiscordWebsocketEventSubscription? {
        if (!eventClass.isSubclassOf(DiscordWebsocketEventBase::class)) {
            return null
        }

        val annotation = function.findAnnotation() ?: defaultDiscordEventAnnotation
        return DiscordWebsocketEventSubscription(plugin, function, annotation)
    }

    override fun canHandle(event: jp.nephy.glados.api.Event): Boolean {
        return event is DiscordEventBase
    }

    override fun start() {
        runBlocking {
            initializeJDA()
        }
    }

    override fun stop() {
        runBlocking {
            disposeJDA()
        }
    }

    @SubscribeEvent
    fun onEvent(jdaEvent: Event) {
        val event = jdaEvent.toGLaDOSEvent() ?: return logger.trace { 
            "未対応のイベントです。(${jdaEvent::class.qualifiedName})"
        }

        launch {
            val targets = subscriptions.filter { it.eventClass == event::class }
            if (targets.isEmpty()) {
                return@launch
            }
            
            targets.forEach {
                launch {
                    it.invoke(event)
                    it.logger.trace {
                        val guild = try {
                            event::class.java.getMethod("getGuild").invoke(event) as? Guild
                        } catch (e: NoSuchMethodException) {
                            null
                        }
                        
                        "実行されました。(${guild?.name})" 
                    }
                }
            }
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun <E: Event> E.toGLaDOSEvent(): DiscordWebsocketEventBase<E>? {
        return when (this) {
            is ReadyEvent -> DiscordReadyEvent(this)
            is ResumedEvent -> DiscordResumedEvent(this)
            is ReconnectedEvent -> DiscordReconnectedEvent(this)
            is DisconnectEvent -> DiscordDisconnectEvent(this)
            is ShutdownEvent -> DiscordShutdownEvent(this)
            is StatusChangeEvent -> DiscordStatusChangeEvent(this)
            is ExceptionEvent -> DiscordExceptionEvent(this)
            is HttpRequestEvent -> DiscordHttpRequestEvent(this)

            is UserUpdateNameEvent -> DiscordUserUpdateNameEvent(this)
            is UserUpdateDiscriminatorEvent -> DiscordUserUpdateDiscriminatorEvent(this)
            is UserUpdateAvatarEvent -> DiscordUserUpdateAvatarEvent(this)
            is UserUpdateOnlineStatusEvent -> DiscordUserUpdateOnlineStatusEvent(this)
            is UserUpdateActivityOrderEvent -> DiscordUserUpdateActivityOrderEvent(this)
            is UserTypingEvent -> DiscordUserTypingEvent(this)
            is UserActivityStartEvent -> DiscordUserActivityStartEvent(this)
            is UserActivityEndEvent -> DiscordUserActivityEndEvent(this)

            is SelfUpdateAvatarEvent -> DiscordSelfUpdateAvatarEvent(this)
            is SelfUpdateEmailEvent -> DiscordSelfUpdateEmailEvent(this)
            is SelfUpdateMFAEvent -> DiscordSelfUpdateMFAEvent(this)
            is SelfUpdateNameEvent -> DiscordSelfUpdateNameEvent(this)
            is SelfUpdateVerifiedEvent -> DiscordSelfUpdateVerifiedEvent(this)

            is GuildMessageReceivedEvent -> DiscordGuildMessageReceivedEvent(this)
            is GuildMessageUpdateEvent -> DiscordGuildMessageUpdateEvent(this)
            is GuildMessageDeleteEvent -> DiscordGuildMessageDeleteEvent(this)
            is GuildMessageEmbedEvent -> DiscordGuildMessageEmbedEvent(this)
            is GuildMessageReactionAddEvent -> DiscordGuildMessageReactionAddEvent(this)
            is GuildMessageReactionRemoveEvent -> DiscordGuildMessageReactionRemoveEvent(this)
            is GuildMessageReactionRemoveAllEvent -> DiscordGuildMessageReactionRemoveAllEvent(this)

            is PrivateMessageReceivedEvent -> DiscordPrivateMessageReceivedEvent(this)
            is PrivateMessageUpdateEvent -> DiscordPrivateMessageUpdateEvent(this)
            is PrivateMessageDeleteEvent -> DiscordPrivateMessageDeleteEvent(this)
            is PrivateMessageEmbedEvent -> DiscordPrivateMessageEmbedEvent(this)
            is PrivateMessageReactionAddEvent -> DiscordPrivateMessageReactionAddEvent(this)
            is PrivateMessageReactionRemoveEvent -> DiscordPrivateMessageReactionRemoveEvent(this)

            is MessageReceivedEvent -> DiscordMessageReceivedEvent(this)
            is MessageUpdateEvent -> DiscordMessageUpdateEvent(this)
            is MessageDeleteEvent -> DiscordMessageDeleteEvent(this)
            is MessageBulkDeleteEvent -> DiscordMessageBulkDeleteEvent(this)
            is MessageEmbedEvent -> DiscordMessageEmbedEvent(this)
            is MessageReactionAddEvent -> DiscordMessageReactionAddEvent(this)
            is MessageReactionRemoveEvent -> DiscordMessageReactionRemoveEvent(this)
            is MessageReactionRemoveAllEvent -> DiscordMessageReactionRemoveAllEvent(this)

            is TextChannelDeleteEvent -> DiscordTextChannelDeleteEvent(this)
            is TextChannelUpdateNameEvent -> DiscordTextChannelUpdateNameEvent(this)
            is TextChannelUpdateTopicEvent -> DiscordTextChannelUpdateTopicEvent(this)
            is TextChannelUpdatePositionEvent -> DiscordTextChannelUpdatePositionEvent(this)
            is TextChannelUpdatePermissionsEvent -> DiscordTextChannelUpdatePermissionsEvent(this)
            is TextChannelUpdateNSFWEvent -> DiscordTextChannelUpdateNSFWEvent(this)
            is TextChannelUpdateParentEvent -> DiscordTextChannelUpdateParentEvent(this)
            is TextChannelUpdateSlowmodeEvent -> DiscordTextChannelUpdateSlowmodeEvent(this)
            is TextChannelCreateEvent -> DiscordTextChannelCreateEvent(this)

            is VoiceChannelDeleteEvent -> DiscordVoiceChannelDeleteEvent(this)
            is VoiceChannelUpdateNameEvent -> DiscordVoiceChannelUpdateNameEvent(this)
            is VoiceChannelUpdatePositionEvent -> DiscordVoiceChannelUpdatePositionEvent(this)
            is VoiceChannelUpdateUserLimitEvent -> DiscordVoiceChannelUpdateUserLimitEvent(this)
            is VoiceChannelUpdateBitrateEvent -> DiscordVoiceChannelUpdateBitrateEvent(this)
            is VoiceChannelUpdatePermissionsEvent -> DiscordVoiceChannelUpdatePermissionsEvent(this)
            is VoiceChannelUpdateParentEvent -> DiscordVoiceChannelUpdateParentEvent(this)
            is VoiceChannelCreateEvent -> DiscordVoiceChannelCreateEvent(this)

            is CategoryDeleteEvent -> DiscordCategoryDeleteEvent(this)
            is CategoryUpdateNameEvent -> DiscordCategoryUpdateNameEvent(this)
            is CategoryUpdatePositionEvent -> DiscordCategoryUpdatePositionEvent(this)
            is CategoryUpdatePermissionsEvent -> DiscordCategoryUpdatePermissionsEvent(this)
            is CategoryCreateEvent -> DiscordCategoryCreateEvent(this)

            is PrivateChannelCreateEvent -> DiscordPrivateChannelCreateEvent(this)
            is PrivateChannelDeleteEvent -> DiscordPrivateChannelDeleteEvent(this)

            is GuildReadyEvent -> DiscordGuildReadyEvent(this)
            is GuildJoinEvent -> DiscordGuildJoinEvent(this)
            is GuildLeaveEvent -> DiscordGuildLeaveEvent(this)
            is GuildAvailableEvent -> DiscordGuildAvailableEvent(this)
            is GuildUnavailableEvent -> DiscordGuildUnavailableEvent(this)
            is UnavailableGuildJoinedEvent -> DiscordUnavailableGuildJoinedEvent(this)
            is GuildBanEvent -> DiscordGuildBanEvent(this)
            is GuildUnbanEvent -> DiscordGuildUnbanEvent(this)
            is GuildUpdateAfkChannelEvent -> DiscordGuildUpdateAfkChannelEvent(this)
            is GuildUpdateSystemChannelEvent -> DiscordGuildUpdateSystemChannelEvent(this)
            is GuildUpdateAfkTimeoutEvent -> DiscordGuildUpdateAfkTimeoutEvent(this)
            is GuildUpdateExplicitContentLevelEvent -> DiscordGuildUpdateExplicitContentLevelEvent(this)
            is GuildUpdateIconEvent -> DiscordGuildUpdateIconEvent(this)
            is GuildUpdateMFALevelEvent -> DiscordGuildUpdateMFALevelEvent(this)
            is GuildUpdateNameEvent -> DiscordGuildUpdateNameEvent(this)
            is GuildUpdateNotificationLevelEvent -> DiscordGuildUpdateNotificationLevelEvent(this)
            is GuildUpdateOwnerEvent -> DiscordGuildUpdateOwnerEvent(this)
            is GuildUpdateRegionEvent -> DiscordGuildUpdateRegionEvent(this)
            is GuildUpdateSplashEvent -> DiscordGuildUpdateSplashEvent(this)
            is GuildUpdateVerificationLevelEvent -> DiscordGuildUpdateVerificationLevelEvent(this)
            is GuildUpdateFeaturesEvent -> DiscordGuildUpdateFeaturesEvent(this)

            is GuildMemberJoinEvent -> DiscordGuildMemberJoinEvent(this)
            is GuildMemberLeaveEvent -> DiscordGuildMemberLeaveEvent(this)
            is GuildMemberRoleAddEvent -> DiscordGuildMemberRoleAddEvent(this)
            is GuildMemberRoleRemoveEvent -> DiscordGuildMemberRoleRemoveEvent(this)
            is GuildMemberNickChangeEvent -> DiscordGuildMemberNickChangeEvent(this)

            is GuildVoiceUpdateEvent -> DiscordGuildVoiceUpdateEvent(this)
            is GuildVoiceJoinEvent -> DiscordGuildVoiceJoinEvent(this)
            is GuildVoiceMoveEvent -> DiscordGuildVoiceMoveEvent(this)
            is GuildVoiceLeaveEvent -> DiscordGuildVoiceLeaveEvent(this)
            is GuildVoiceMuteEvent -> DiscordGuildVoiceMuteEvent(this)
            is GuildVoiceDeafenEvent -> DiscordGuildVoiceDeafenEvent(this)
            is GuildVoiceGuildMuteEvent -> DiscordGuildVoiceGuildMuteEvent(this)
            is GuildVoiceGuildDeafenEvent -> DiscordGuildVoiceGuildDeafenEvent(this)
            is GuildVoiceSelfMuteEvent -> DiscordGuildVoiceSelfMuteEvent(this)
            is GuildVoiceSelfDeafenEvent -> DiscordGuildVoiceSelfDeafenEvent(this)
            is GuildVoiceSuppressEvent -> DiscordGuildVoiceSuppressEvent(this)

            is RoleCreateEvent -> DiscordRoleCreateEvent(this)
            is RoleDeleteEvent -> DiscordRoleDeleteEvent(this)
            is RoleUpdateColorEvent -> DiscordRoleUpdateColorEvent(this)
            is RoleUpdateHoistedEvent -> DiscordRoleUpdateHoistedEvent(this)
            is RoleUpdateMentionableEvent -> DiscordRoleUpdateMentionableEvent(this)
            is RoleUpdateNameEvent -> DiscordRoleUpdateNameEvent(this)
            is RoleUpdatePermissionsEvent -> DiscordRoleUpdatePermissionsEvent(this)
            is RoleUpdatePositionEvent -> DiscordRoleUpdatePositionEvent(this)

            is EmoteAddedEvent -> DiscordEmoteAddedEvent(this)
            is EmoteRemovedEvent -> DiscordEmoteRemovedEvent(this)
            is EmoteUpdateNameEvent -> DiscordEmoteUpdateNameEvent(this)
            is EmoteUpdateRolesEvent -> DiscordEmoteUpdateRolesEvent(this)
            
            else -> return null
        } as DiscordWebsocketEventBase<E>
    }
}
