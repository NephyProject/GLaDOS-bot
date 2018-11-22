package jp.nephy.glados.core.plugins.extensions

import jp.nephy.glados.GLaDOS
import jp.nephy.glados.core.config.role
import jp.nephy.glados.core.plugins.extensions.jda.hasRole
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User

fun Member.hasAdminCapability(): Boolean {
    return isAdmin || user.isGLaDOSOwner || isOwner
}

fun User.hasAdminCapability(): Boolean {
    return isGLaDOSOwner
}

val Member.isAdmin: Boolean
    get() {
        val role = guild.config.role("admin") ?: return false
        return hasRole(role)
    }

val User.isGLaDOSOwner: Boolean
    get() = idLong == GLaDOS.config.ownerId
