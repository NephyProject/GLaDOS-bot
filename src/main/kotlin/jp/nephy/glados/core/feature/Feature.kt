@file:Suppress("UNUSED")
package jp.nephy.glados.core.feature

import jp.nephy.glados.config
import jp.nephy.glados.core.Logger
import jp.nephy.glados.dispatcher
import kotlinx.coroutines.CoroutineScope
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.VoiceChannel
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class BotFeature: DiscordEventModel, CoroutineScope {
    override val coroutineContext = dispatcher

    val logger by lazy { Logger("Feature.${javaClass.simpleName}") }
}

annotation class Feature(val guilds: Array<String>)

fun BotFeature.rolesLazy(name: String) = object: ReadOnlyProperty<BotFeature, List<Role>> {
    override fun getValue(thisRef: BotFeature, property: KProperty<*>): List<Role> {
        return config.guilds.mapNotNull { it.value.role(name) }
    }
}

fun BotFeature.voiceChannelsLazy(name: String) = object: ReadOnlyProperty<BotFeature, List<VoiceChannel>> {
    override fun getValue(thisRef: BotFeature, property: KProperty<*>): List<VoiceChannel> {
        return config.guilds.mapNotNull { it.value.voiceChannel(name) }
    }
}

fun BotFeature.textChannelsLazy(name: String) = object: ReadOnlyProperty<BotFeature, List<TextChannel>> {
    override fun getValue(thisRef: BotFeature, property: KProperty<*>): List<TextChannel> {
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
inline fun BotFeature.reject(value: Boolean, fallback: () -> Any?) {
    contract {
        returns() implies !value
    }

    if (value) {
        fallback()
        throw IllegalStateException()
    }
}

@UseExperimental(ExperimentalContracts::class)
inline fun <T> BotFeature.rejectNull(value: T?, fallback: () -> Any?) {
    contract {
        returns() implies (value != null)
    }

    if (value == null) {
        fallback()
        throw IllegalStateException()
    }
}
