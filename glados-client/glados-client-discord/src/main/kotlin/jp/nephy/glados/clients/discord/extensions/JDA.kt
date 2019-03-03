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

package jp.nephy.glados.clients.discord.extensions

import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.*

val User.displayName: String
    get() = "@$name#$discriminator"
val User.isSelfUser: Boolean
    get() = idLong == jda.selfUser.idLong
val User.isBotOrSelfUser: Boolean
    get() = isBot || isSelfUser
val TextChannel.fullName: String
    get() = "#$name (${guild?.name})"
val TextChannel.fullNameWithoutGuild: String
    get() = "#$name"
val VoiceChannel.isNoOneExceptSelf: Boolean
    get() = members.count { !it.user.isSelfUser } == 0
val Member.fullName: String
    get() = "$effectiveName (${user.displayName}, ${guild.name})"
val Member.fullNameWithoutGuild: String
    get() = "$effectiveName (${user.displayName})"
val Message.fullName: String
    get() = "${member?.fullNameWithoutGuild ?: author.displayName} ${textChannel?.fullName ?: channel.name}"
val Message.fullNameWithoutGuild: String
    get() = "${member?.fullNameWithoutGuild ?: author.displayName} ${textChannel?.fullNameWithoutGuild ?: channel.name}"

fun Member.hasRole(id: Long): Boolean {
    return roles.any { it.idLong == id }
}

fun Member.hasRole(role: Role): Boolean {
    return hasRole(role.idLong)
}

fun TextChannel.getHistory(limit: Int): List<Message> {
    return iterableHistory.cache(false).take(limit)
}

fun Member.addRole(id: Long) {
    if (!hasRole(id)) {
        guild.controller.addSingleRoleToMember(this, jda.getRoleById(id)).launch()
    }
}

fun Member.addRole(role: Role) {
    if (!hasRole(role)) {
        guild.controller.addSingleRoleToMember(this, role).launch()
    }
}

fun Member.removeRole(id: Long) {
    if (hasRole(id)) {
        guild.controller.removeSingleRoleFromMember(this, jda.getRoleById(id)).launch()
    }
}

fun Member.removeRole(role: Role) {
    if (hasRole(role)) {
        guild.controller.removeSingleRoleFromMember(this, role).launch()
    }
}
