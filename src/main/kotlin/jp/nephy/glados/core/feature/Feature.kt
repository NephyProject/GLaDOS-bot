package jp.nephy.glados.core.feature

import jp.nephy.glados.core.Logger
import net.dv8tion.jda.core.hooks.ListenerAdapter

abstract class BotFeature: ListenerAdapter() {
    val logger by lazy { Logger("Feature.${javaClass.simpleName}") }
}

annotation class Feature(val guilds: Array<String>)
