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

package jp.nephy.glados.clients.discord.config

import jp.nephy.glados.api.GLaDOS
import jp.nephy.glados.clients.discord.jda
import kotlinx.serialization.json.longOrNull
import net.dv8tion.jda.api.entities.Emote
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.VoiceChannel

fun Discord.GuildConfig?.textChannel(key: String): TextChannel? {
    return GLaDOS.jda.getTextChannelById(this?.textChannels?.getOrNull(key)?.longOrNull ?: return null)
}

fun Discord.GuildConfig?.voiceChannel(key: String): VoiceChannel? {
    return GLaDOS.jda.getVoiceChannelById(this?.voiceChannels?.getOrNull(key)?.longOrNull ?: return null)
}

fun Discord.GuildConfig?.role(key: String): Role? {
    return GLaDOS.jda.getRoleById(this?.roles?.getOrNull(key)?.longOrNull ?: return null)
}

fun Discord.GuildConfig?.emote(key: String): Emote? {
    return GLaDOS.jda.getEmoteById(this?.emotes?.getOrNull(key)?.longOrNull ?: return null)
}

inline fun <T> Discord.GuildConfig?.withTextChannel(key: String, operation: (TextChannel) -> T?): T? {
    return operation(textChannel(key) ?: return null)
}

inline fun <T> Discord.GuildConfig?.withVoiceChannel(key: String, operation: (VoiceChannel) -> T?): T? {
    return operation(voiceChannel(key) ?: return null)
}

inline fun <T> Discord.GuildConfig?.withRole(key: String, operation: (Role) -> T?): T? {
    return operation(role(key) ?: return null)
}

inline fun <T> Discord.GuildConfig?.withEmote(key: String, operation: (Emote) -> T?): T? {
    return operation(emote(key) ?: return null)
}
