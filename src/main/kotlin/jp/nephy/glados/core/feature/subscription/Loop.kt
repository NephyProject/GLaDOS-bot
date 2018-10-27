package jp.nephy.glados.core.feature.subscription

import jp.nephy.glados.core.Logger
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.invocationException
import jp.nephy.glados.dispatcher
import kotlinx.coroutines.*
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.reflect.KFunction

private val logger = Logger("GLaDOS.Loop")

data class LoopSubscription(
        override val annotation: Loop,
        override val instance: BotFeature,
        override val function: KFunction<*>
): Subscription<Loop>

annotation class Loop(
        val interval: Long,
        val unit: TimeUnit,
        val priority: Priority = Priority.Normal
)

class LoopSubscriptionClient: SubscriptionClient<Loop>, ListenerAdapter() {
    override val subscriptions = CopyOnWriteArrayList<Subscription<Loop>>()

    override fun onReady() {
        subscriptions.sortBy { it.annotation.priority }
    }

    override fun onReady(event: ReadyEvent) {
        for (it in subscriptions) {
            GlobalScope.launch(dispatcher) {
                while (isActive) {
                    try {
                        it.invoke()
                        logger.trace { "${it.instance.javaClass.simpleName}#${it.function.name} が実行されました。" }
                    } catch (e: CancellationException) {
                        break
                    } catch (e: Exception) {
                        logger.error(e.invocationException) { "[${it.instance.javaClass.simpleName}#${it.function.name}] 実行中に例外が発生しました。" }
                    }

                    try {
                        delay(it.annotation.unit.toMillis(it.annotation.interval))
                    } catch (e: CancellationException) {
                        break
                    }
                }
            }
        }
    }
}
