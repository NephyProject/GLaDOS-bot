@file:Suppress("UNUSED", "NOTHING_TO_INLINE")

package jp.nephy.glados.core.extensions

import jp.nephy.glados.config
import jp.nephy.glados.core.extensions.messages.HexColor
import jp.nephy.glados.core.plugins.CommandError
import jp.nephy.glados.core.plugins.Plugin
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.requests.restaction.MessageAction
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

inline fun Plugin.rolesLazy(name: String): ReadOnlyProperty<Plugin, List<Role>> = object: ReadOnlyProperty<Plugin, List<Role>> {
    override fun getValue(thisRef: Plugin, property: KProperty<*>): List<Role> {
        return config.guilds.mapNotNull { it.value.role(name) }
    }
}

inline fun Plugin.voiceChannelsLazy(name: String): ReadOnlyProperty<Plugin, List<VoiceChannel>> = object: ReadOnlyProperty<Plugin, List<VoiceChannel>> {
    override fun getValue(thisRef: Plugin, property: KProperty<*>): List<VoiceChannel> {
        return config.guilds.mapNotNull { it.value.voiceChannel(name) }
    }
}

inline fun Plugin.textChannelsLazy(name: String): ReadOnlyProperty<Plugin, List<TextChannel>> = object: ReadOnlyProperty<Plugin, List<TextChannel>> {
    override fun getValue(thisRef: Plugin, property: KProperty<*>): List<TextChannel> {
        return config.guilds.mapNotNull { it.value.textChannel(name) }
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

inline fun Guild.withRole(key: String, block: (Role) -> Unit) {
    config.forGuild(this)?.withRole(key, block)
}

inline fun Guild.withTextChannel(key: String, block: (TextChannel) -> Unit) {
    config.forGuild(this)?.withTextChannel(key, block)
}

inline fun Guild.withVoiceChannel(key: String, block: (VoiceChannel) -> Unit) {
    config.forGuild(this)?.withVoiceChannel(key, block)
}

@UseExperimental(ExperimentalContracts::class)
inline fun Plugin.reject(value: Boolean, fallback: () -> Any?) {
    contract {
        returns() implies !value
    }

    if (value) {
        fallback()
        throw IllegalStateException()
    }
}

@UseExperimental(ExperimentalContracts::class)
inline fun <T> Plugin.rejectNull(value: T?, fallback: () -> Any?) {
    contract {
        returns() implies (value != null)
    }

    if (value == null) {
        fallback()
        throw IllegalStateException()
    }
}

inline fun Message.embedError(commandName: String, description: () -> String): Nothing = throw CommandError.Embed(this, commandName, description.invoke())

inline fun Message.simpleError(commandName: String, description: StringBuilder.() -> Unit): Nothing = throw CommandError.Simple(this, commandName, buildString(description))

inline fun Plugin.Command.Event.embedError(description: () -> String): Nothing = throw CommandError.Simple(message, command.primaryCommandSyntax, description.invoke())

inline fun Plugin.Command.Event.simpleError(description: StringBuilder.() -> Unit): Nothing = throw CommandError.Embed(message, command.primaryCommandSyntax, buildString(description))

inline fun Message.embedResult(commandName: String, noinline description: () -> String): MessageAction {
    return reply {
        embed {
            title(commandName)
            description(description)
            timestamp()
            color(HexColor.Good)
        }
    }
}

inline fun Message.simpleResult(noinline description: () -> String): MessageAction {
    return message {
        message {
            append("${author.asMention} ${description.invoke()}")
        }
    }
}

inline fun Plugin.Command.Event.embedResult(noinline description: () -> String) = message.embedResult(command.primaryCommandSyntax, description)

inline fun Plugin.Command.Event.simpleResult(noinline description: () -> String) = message.simpleResult(description)
