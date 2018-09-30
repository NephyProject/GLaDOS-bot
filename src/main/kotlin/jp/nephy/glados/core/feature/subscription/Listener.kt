package jp.nephy.glados.core.feature.subscription

import jp.nephy.glados.core.GLaDOSConfig
import jp.nephy.glados.core.Logger
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.invocationException
import jp.nephy.glados.core.nullableGuild
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KFunction
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure

private val logger = Logger("GLaDOS.Listener")

data class ListenerSubscription(
        override val annotation: Listener,
        override val instance: BotFeature,
        override val function: KFunction<*>,
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

        subscriptions.asSequence().filter {
            guild == null || it.targetGuilds.isEmpty() || it.targetGuilds.any { it.id == guild.idLong }
        }.filter {
            val required = it.function.valueParameters.first().type.jvmErasure
            event::class.isSubclassOf(required)
        }.toList().forEach {
            launch {
                try {
                    it.invoke(event)
                    logger.trace { "${it.instance.javaClass.simpleName}#${it.function.name} が実行されました. (${guild?.name})" }
                } catch (e: CancellationException) {
                    return@launch
                } catch (e: Exception) {
                    logger.error(e.invocationException) { "[${it.instance.javaClass.simpleName}#${it.function.name}] 実行中に例外が発生しました." }
                }
            }
        }
    }
}
