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

package jp.nephy.glados.clients.discord.listener.audio

import com.sedmelluq.discord.lavaplayer.player.event.*
import jp.nephy.glados.GLaDOSSubscriptionClient
import jp.nephy.glados.api.Plugin
import jp.nephy.glados.clients.discord.GuildPlayer
import jp.nephy.glados.clients.discord.listener.DiscordEvent
import jp.nephy.glados.clients.discord.listener.audio.events.*
import jp.nephy.glados.clients.discord.listener.defaultDiscordEventAnnotation
import jp.nephy.glados.clients.runEvent
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf

/**
 * DiscordAudioEventSubscriptionClient.
 */
object DiscordAudioEventSubscriptionClient: GLaDOSSubscriptionClient<DiscordEvent, DiscordAudioEventBase, DiscordAudioEventSubscription>() {
    override fun create(plugin: Plugin, function: KFunction<*>, eventClass: KClass<*>): DiscordAudioEventSubscription? {
        if (!eventClass.isSubclassOf(DiscordAudioEventBase::class)) {
            return null
        }

        val annotation = function.findAnnotation() ?: defaultDiscordEventAnnotation
        return DiscordAudioEventSubscription(plugin, function, annotation)
    }
    
    internal class Listener(private val guildPlayer: GuildPlayer): AudioEventListener {
        override fun onEvent(event: AudioEvent) {
            when (event) {
                is PlayerPauseEvent -> runEvent {
                    DiscordAudioPlayerPauseEvent(it, guildPlayer, event)
                }
                is PlayerResumeEvent -> runEvent {
                    DiscordAudioPlayerResumeEvent(it, guildPlayer, event)
                }
                is TrackStartEvent -> runEvent {
                    DiscordAudioTrackStartEvent(it, guildPlayer, event)
                }
                is TrackEndEvent -> runEvent {
                    DiscordAudioTrackEndEvent(it, guildPlayer, event)
                }
                is TrackExceptionEvent -> runEvent {
                    DiscordAudioTrackExceptionEvent(it, guildPlayer, event)
                }
                is TrackStuckEvent -> runEvent {
                    DiscordAudioTrackStuckEvent(it, guildPlayer, event)
                }
                else -> return logger.trace {
                    "未対応のイベントです。(${event::class.qualifiedName})"
                }
            }
        }
    }
}
