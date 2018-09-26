package jp.nephy.glados.core.feature

import jp.nephy.glados.config
import jp.nephy.glados.core.Logger
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.hooks.ListenerAdapter
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class BotFeature: ListenerAdapter() {
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

fun List<Role>.roleOf(guild: Guild, block: (Role) -> Unit) {
    find { it.guild == guild }?.run(block)
}

fun List<TextChannel>.textChannelOf(guild: Guild, block: (TextChannel) -> Unit) {
    find { it.guild == guild }?.run(block)
}

fun List<VoiceChannel>.voiceChannelOf(guild: Guild, block: (VoiceChannel) -> Unit) {
    find { it.guild == guild }?.run(block)
}
