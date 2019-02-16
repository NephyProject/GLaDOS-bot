@file:Suppress("UNUSED")

package jp.nephy.glados.core.plugins.extensions

import jp.nephy.glados.GLaDOS
import jp.nephy.glados.core.config.*
import jp.nephy.glados.core.plugins.Plugin
import net.dv8tion.jda.core.entities.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

val Guild?.config: GLaDOSConfig.GuildConfig?
    get() {
        if (this == null) {
            return null
        }

        return GLaDOS.config.guilds.find { it.id == idLong }
    }

fun Plugin.rolesLazy(name: String): ReadOnlyProperty<Plugin, List<Role>> = object: ReadOnlyProperty<Plugin, List<Role>> {
    override fun getValue(thisRef: Plugin, property: KProperty<*>): List<Role> {
        return GLaDOS.config.guilds.mapNotNull { it.role(name) }
    }
}

fun Plugin.voiceChannelsLazy(name: String): ReadOnlyProperty<Plugin, List<VoiceChannel>> = object: ReadOnlyProperty<Plugin, List<VoiceChannel>> {
    override fun getValue(thisRef: Plugin, property: KProperty<*>): List<VoiceChannel> {
        return GLaDOS.config.guilds.mapNotNull { it.voiceChannel(name) }
    }
}

fun Plugin.textChannelsLazy(name: String): ReadOnlyProperty<Plugin, List<TextChannel>> = object: ReadOnlyProperty<Plugin, List<TextChannel>> {
    override fun getValue(thisRef: Plugin, property: KProperty<*>): List<TextChannel> {
        return GLaDOS.config.guilds.mapNotNull { it.textChannel(name) }
    }
}

fun Plugin.emotesLazy(name: String): ReadOnlyProperty<Plugin, List<Emote>> = object: ReadOnlyProperty<Plugin, List<Emote>> {
    override fun getValue(thisRef: Plugin, property: KProperty<*>): List<Emote> {
        return GLaDOS.config.guilds.mapNotNull { it.emote(name) }
    }
}

inline fun List<Role>.roleOf(guild: Guild, block: (Role) -> Unit) {
    find { it.guild == guild }?.run(block)
}

inline fun List<TextChannel>.textChannelOf(guild: Guild, block: (TextChannel) -> Unit) {
    find { it.guild == guild }?.run(block)
}

inline fun List<VoiceChannel>.voiceChannelOf(guild: Guild, block: (VoiceChannel) -> Unit) {
    find { it.guild == guild }?.run(block)
}

inline fun List<Emote>.emoteOf(guild: Guild, block: (Emote) -> Unit) {
    find { it.guild == guild }?.run(block)
}

inline fun Guild.withRole(key: String, block: (Role) -> Unit) {
    config?.withRole(key, block)
}

inline fun Guild.withTextChannel(key: String, block: (TextChannel) -> Unit) {
    config?.withTextChannel(key, block)
}

inline fun Guild.withVoiceChannel(key: String, block: (VoiceChannel) -> Unit) {
    config?.withVoiceChannel(key, block)
}

inline fun Guild.withEmote(key: String, block: (Emote) -> Unit) {
    config?.withEmote(key, block)
}
