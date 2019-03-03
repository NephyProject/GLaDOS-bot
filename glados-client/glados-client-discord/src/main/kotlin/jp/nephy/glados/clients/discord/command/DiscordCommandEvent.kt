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

package jp.nephy.glados.clients.discord.command

import jp.nephy.glados.api.Event
import jp.nephy.glados.clients.discord.extensions.displayName
import jp.nephy.glados.clients.discord.extensions.fullName
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent

class DiscordCommandEvent(
    val argList: List<String>, val command: DiscordCommandSubscription, val message: Message, val author: User, val member: Member?, val guild: Guild?, val textChannel: TextChannel?, val channel: MessageChannel, _jda: JDA, _responseNumber: Long
): net.dv8tion.jda.api.events.Event(_jda, _responseNumber), Event {
    constructor(argList: List<String>, command: DiscordCommandSubscription, event: MessageReceivedEvent): this(argList, command, event.message, event.author, event.member, event.guild, event.textChannel, event.channel, event.jda, event.responseNumber)
    constructor(argList: List<String>, command: DiscordCommandSubscription, event: MessageUpdateEvent): this(argList, command, event.message, event.author, event.member, event.guild, event.textChannel, event.channel, event.jda, event.responseNumber)
}

val DiscordCommandEvent.args: String
    get() = argList.joinToString(" ")

val DiscordCommandEvent.authorName: String
    get() = member?.fullName ?: author.displayName

val DiscordCommandEvent.first: String
    get() = argList[0]

operator fun DiscordCommandEvent.component1(): String = first

val DiscordCommandEvent.second: String
    get() = argList[1]

operator fun DiscordCommandEvent.component2(): String = second

operator fun DiscordCommandEvent.component3(): String = argList[2]

operator fun DiscordCommandEvent.component4(): String = argList[3]

operator fun DiscordCommandEvent.component5(): String = argList[4]

operator fun DiscordCommandEvent.component6(): String = argList[5]

operator fun DiscordCommandEvent.component7(): String = argList[6]

operator fun DiscordCommandEvent.component8(): String = argList[7]

operator fun DiscordCommandEvent.component9(): String = argList[8]

operator fun DiscordCommandEvent.component10(): String = argList[9]
