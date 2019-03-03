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

package jp.nephy.glados.clients.discord.listener.connection

import jp.nephy.glados.api.Plugin
import jp.nephy.glados.clients.GLaDOSSubscriptionClient
import jp.nephy.glados.clients.discord.listener.DiscordEvent
import jp.nephy.glados.clients.discord.listener.connection.events.DiscordConnectionEventBase
import jp.nephy.glados.clients.discord.listener.connection.events.DiscordConnectionStatusChangeEvent
import jp.nephy.glados.clients.discord.listener.connection.events.DiscordPingEvent
import jp.nephy.glados.clients.discord.listener.connection.events.DiscordUserSpeakingEvent
import jp.nephy.glados.clients.discord.listener.defaultDiscordEventAnnotation
import jp.nephy.glados.clients.utils.eventClass
import jp.nephy.glados.clients.utils.invoke
import jp.nephy.glados.clients.utils.subscriptions
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.audio.SpeakingMode
import net.dv8tion.jda.api.audio.hooks.ConnectionListener
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf

object DiscordConnectionEventSubscriptionClient: GLaDOSSubscriptionClient<DiscordEvent, DiscordConnectionEventBase, DiscordConnectionEventSubscription>() {
    override fun create(plugin: Plugin, function: KFunction<*>, eventClass: KClass<*>): DiscordConnectionEventSubscription? {
        if (!eventClass.isSubclassOf(DiscordConnectionEventBase::class)) {
            return null
        }

        val annotation = function.findAnnotation() ?: defaultDiscordEventAnnotation
        return DiscordConnectionEventSubscription(plugin, function, annotation)
    }

    override fun start() {}

    override fun stop() {}

    class Listener(private val guild: Guild): ConnectionListener {
        override fun onPing(ping: Long) {
            runEvent(DiscordPingEvent(guild, ping))
        }

        override fun onStatusChange(status: ConnectionStatus) {
            runEvent(DiscordConnectionStatusChangeEvent(guild, status))
        }

        override fun onUserSpeaking(user: User, speaking: Boolean) {}

        override fun onUserSpeaking(user: User, modes: EnumSet<SpeakingMode>) {
            runEvent(DiscordUserSpeakingEvent(guild, user, modes))
        }

        private fun <E: DiscordConnectionEventBase> runEvent(event: E) {
            subscriptions.filter {
                it.eventClass == event::class
            }.forEach {
                launch {
                    it.invoke(event)
                    it.logger.trace { "実行されました。(${event.guild.name})" }
                }
            }
        }
    }
}
