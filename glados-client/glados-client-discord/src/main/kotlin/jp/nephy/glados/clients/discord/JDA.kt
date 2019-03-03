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

package jp.nephy.glados.clients.discord

import jp.nephy.glados.api.GLaDOS
import jp.nephy.glados.api.attributes
import jp.nephy.glados.api.config
import jp.nephy.glados.clients.discord.command.DiscordCommandSubscriptionClient
import jp.nephy.glados.clients.discord.config.discord
import jp.nephy.glados.clients.discord.extensions.DiscordEventWaiter
import jp.nephy.glados.clients.discord.listener.websocket.DiscordWebsocketEventSubscriptionClient
import net.dv8tion.jda.api.AccountType
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.hooks.AnnotatedEventManager

val GLaDOS.Companion.jda: JDA
    get() = GLaDOS.attributes["jda"]

internal fun initializeJDA() {
    if ("jda" in GLaDOS.attributes) {
        return
    }

    GLaDOS.attributes["jda"] = {
        JDABuilder(AccountType.BOT).apply {
            setToken(GLaDOS.config.discord.token)
            setActivity(Activity.playing("Starting..."))
            setEnableShutdownHook(false)
            
            // setAudioSendFactory(NativeAudioSendFactory(5000))
            
            setEventManager(AnnotatedEventManager())
            addEventListeners(DiscordWebsocketEventSubscriptionClient, DiscordCommandSubscriptionClient, DiscordEventWaiter)
        }.build()
    }
}

internal fun disposeJDA() {
    if ("jda" !in GLaDOS.attributes) {
        return
    }

    val jda: JDA = GLaDOS.attributes["jda"]
    jda.shutdown()

    GLaDOS.attributes.dispose("jda")
}
