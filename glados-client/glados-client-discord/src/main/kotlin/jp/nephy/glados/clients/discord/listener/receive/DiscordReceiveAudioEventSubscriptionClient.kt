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

package jp.nephy.glados.clients.discord.listener.receive

import jp.nephy.glados.api.Plugin
import jp.nephy.glados.clients.GLaDOSSubscriptionClient
import jp.nephy.glados.clients.discord.GuildPlayer
import jp.nephy.glados.clients.discord.listener.DiscordEvent
import jp.nephy.glados.clients.discord.listener.defaultDiscordEventAnnotation
import jp.nephy.glados.clients.discord.listener.receive.events.DiscordCombinedAudioReceiveEvent
import jp.nephy.glados.clients.discord.listener.receive.events.DiscordReceiveAudioEventBase
import jp.nephy.glados.clients.discord.listener.receive.events.DiscordUserAudioReceiveEvent
import jp.nephy.glados.clients.utils.eventClass
import jp.nephy.glados.clients.utils.invoke
import jp.nephy.glados.clients.utils.subscriptions
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.audio.AudioReceiveHandler
import net.dv8tion.jda.api.audio.CombinedAudio
import net.dv8tion.jda.api.audio.UserAudio
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf

object DiscordReceiveAudioEventSubscriptionClient: GLaDOSSubscriptionClient<DiscordEvent, DiscordReceiveAudioEventBase, DiscordReceiveAudioEventSubscription>() {
    override fun create(plugin: Plugin, function: KFunction<*>, eventClass: KClass<*>): DiscordReceiveAudioEventSubscription? {
        if (!eventClass.isSubclassOf(DiscordReceiveAudioEventBase::class)) {
            return null
        }

        val annotation = function.findAnnotation() ?: defaultDiscordEventAnnotation
        return DiscordReceiveAudioEventSubscription(plugin, function, annotation)
    }

    override fun start() {}

    override fun stop() {}
    
    class Listener(private val guildPlayer: GuildPlayer): AudioReceiveHandler {
        override fun canReceiveUser(): Boolean = true

        override fun handleUserAudio(userAudio: UserAudio) {
            runEvent(DiscordUserAudioReceiveEvent(guildPlayer, userAudio))
        }

        override fun canReceiveCombined(): Boolean = true

        override fun handleCombinedAudio(combinedAudio: CombinedAudio) {
            runEvent(DiscordCombinedAudioReceiveEvent(guildPlayer, combinedAudio))
        }

        private fun <E: DiscordReceiveAudioEventBase> runEvent(event: E) {
            subscriptions.filter {
                it.eventClass == event::class
            }.forEach {
                launch {
                    it.invoke(event)
                    it.logger.trace { "実行されました。(${guildPlayer.guild.name})" }
                }
            }
        }
    }
}
