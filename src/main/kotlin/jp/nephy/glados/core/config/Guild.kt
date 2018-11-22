@file:Suppress("UNUSED")

package jp.nephy.glados.core.config

import jp.nephy.glados.GLaDOS
import kotlinx.serialization.json.longOrNull
import net.dv8tion.jda.core.entities.Emote
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.VoiceChannel

fun GLaDOSConfig.GuildConfig?.textChannel(key: String): TextChannel? {
    return GLaDOS.jda.getTextChannelById(this?.textChannels?.getOrNull(key)?.longOrNull ?: return null)
}

fun GLaDOSConfig.GuildConfig?.voiceChannel(key: String): VoiceChannel? {
    return GLaDOS.jda.getVoiceChannelById(this?.voiceChannels?.getOrNull(key)?.longOrNull ?: return null)
}

fun GLaDOSConfig.GuildConfig?.role(key: String): Role? {
    return GLaDOS.jda.getRoleById(this?.roles?.getOrNull(key)?.longOrNull ?: return null)
}

fun GLaDOSConfig.GuildConfig?.emote(key: String): Emote? {
    return GLaDOS.jda.getEmoteById(this?.emotes?.getOrNull(key)?.longOrNull ?: return null)
}

inline fun <T> GLaDOSConfig.GuildConfig?.withTextChannel(key: String, operation: (TextChannel) -> T?): T? {
    return operation(textChannel(key) ?: return null)
}

inline fun <T> GLaDOSConfig.GuildConfig?.withVoiceChannel(key: String, operation: (VoiceChannel) -> T?): T? {
    return operation(voiceChannel(key) ?: return null)
}

inline fun <T> GLaDOSConfig.GuildConfig?.withRole(key: String, operation: (Role) -> T?): T? {
    return operation(role(key) ?: return null)
}

inline fun <T> GLaDOSConfig.GuildConfig?.withEmote(key: String, operation: (Emote) -> T?): T? {
    return operation(emote(key) ?: return null)
}
