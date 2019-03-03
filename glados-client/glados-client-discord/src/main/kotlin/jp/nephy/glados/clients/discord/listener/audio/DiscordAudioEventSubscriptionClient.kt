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
import jp.nephy.glados.api.Plugin
import jp.nephy.glados.clients.GLaDOSSubscriptionClient
import jp.nephy.glados.clients.discord.GuildPlayer
import jp.nephy.glados.clients.discord.listener.DiscordEvent
import jp.nephy.glados.clients.discord.listener.audio.events.*
import jp.nephy.glados.clients.discord.listener.defaultDiscordEventAnnotation
import jp.nephy.glados.clients.utils.eventClass
import jp.nephy.glados.clients.utils.invoke
import jp.nephy.glados.clients.utils.subscriptions
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf

object DiscordAudioEventSubscriptionClient: GLaDOSSubscriptionClient<DiscordEvent, DiscordAudioEventBase, DiscordAudioEventSubscription>() {
    override fun create(plugin: Plugin, function: KFunction<*>, eventClass: KClass<*>): DiscordAudioEventSubscription? {
        if (!eventClass.isSubclassOf(DiscordAudioEventBase::class)) {
            return null
        }

        val annotation = function.findAnnotation() ?: defaultDiscordEventAnnotation
        return DiscordAudioEventSubscription(plugin, function, annotation)
    }
    
    override fun start() {}

    override fun stop() {}

    class Listener(private val guildPlayer: GuildPlayer): AudioEventListener {
        private fun <E: DiscordAudioEventBase> runEvent(event: E) {
            DiscordAudioEventSubscriptionClient.subscriptions.filter {
                it.eventClass == event::class
            }.forEach {
                launch {
                    it.invoke(event)
                    it.logger.trace { "実行されました。(${guildPlayer.guild.name})" }
                }
            }
        }

        override fun onEvent(event: AudioEvent) {
            runEvent(
                when (event) {
                    is PlayerPauseEvent -> {
                        DiscordAudioPlayerPauseEvent(guildPlayer, event)
                    }
                    is PlayerResumeEvent -> {
                        DiscordAudioPlayerResumeEvent(guildPlayer, event)
                    }
                    is TrackStartEvent -> {
                        DiscordAudioTrackStartEvent(guildPlayer, event)
                    }
                    is TrackEndEvent -> {
                        DiscordAudioTrackEndEvent(guildPlayer, event)
                    }
                    is TrackExceptionEvent -> {
                        DiscordAudioTrackExceptionEvent(guildPlayer, event)
                    }
                    is TrackStuckEvent -> {
                        DiscordAudioTrackStuckEvent(guildPlayer, event)
                    }
                    else -> return
                }
            )
        }
    }
}
