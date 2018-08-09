package jp.nephy.glados.core.feature.subscription

import jp.nephy.glados.core.GLaDOSConfig
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.nullableGuild
import jp.nephy.glados.logger
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.CopyOnWriteArrayList

data class ListenerSubscription(
        override val annotation: Listener,
        override val instance: BotFeature,
        override val method: Method,
        override val targetGuilds: List<GLaDOSConfig.GuildConfig>
): GuildSpecificSubscription<Listener>

@Target(AnnotationTarget.FUNCTION)
annotation class Listener(
        val guilds: Array<String> = [],
        val priority: Priority = Priority.Normal
)

class ListenerSubscriptionClient: SubscriptionClient<Listener>, ListenerAdapter() {
    override val subscriptions = CopyOnWriteArrayList<GuildSpecificSubscription<Listener>>()

    override fun onReady() {
        subscriptions.sortBy { it.annotation.priority }
    }

    override fun onGenericEvent(event: Event) {
        val guild = event.nullableGuild

        subscriptions.filter {
            guild == null || it.targetGuilds.isEmpty() || it.targetGuilds.any { it.id == guild.idLong }
        }.filter {
            it.method.parameterTypes.first() == event.javaClass
        }.forEach {
            try {
                it.execute(event)
            } catch (e: Exception) {
                val exception = if (e is InvocationTargetException) {
                    e.targetException
                } else {
                    e
                }

                logger.error(exception) { "[${it.instance.javaClass.name}#${it.method.name}] 実行中に例外が発生しました." }
            }
        }
    }
}
