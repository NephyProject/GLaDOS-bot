package jp.nephy.glados.core

import jp.nephy.glados.config
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.Event

const val adminRoleKey = "admin"

val Event.nullableGuild
    get() = try {
        javaClass.getMethod("getGuild").invoke(this) as? Guild
    } catch (e: NoSuchMethodException) {
        null
    }

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

fun Member.isAdmin(): Boolean {
    val role = config.forGuild(guild)?.role(adminRoleKey) ?: return false
    return hasRole(role)
}

fun User.isGLaDOSOwner(): Boolean {
    val ownerId = config.ownerId ?: return false
    return idLong == ownerId
}

fun Member.isGLaDOSOwner(): Boolean {
    return user.isGLaDOSOwner()
}

fun Member.addRole(role: Role) {
    if (!hasRole(role)) {
        guild.controller.addSingleRoleToMember(this, role).queue()
    }
}

fun Member.removeRole(role: Role) {
    if (hasRole(role)) {
        guild.controller.removeSingleRoleFromMember(this, role).queue()
    }
}
