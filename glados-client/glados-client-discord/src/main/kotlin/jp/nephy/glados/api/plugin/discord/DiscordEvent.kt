package jp.nephy.glados.api.plugin.discord

import jp.nephy.glados.api.plugin.Priority

@Target(AnnotationTarget.FUNCTION)
annotation class DiscordEvent(
    val priority: Priority = Priority.Normal
)
