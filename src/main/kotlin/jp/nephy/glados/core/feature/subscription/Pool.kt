package jp.nephy.glados.core.feature.subscription

import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.logger
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.lang.reflect.Method
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class PoolSubscription(
        override val annotation: Pool,
        override val instance: BotFeature,
        override val method: Method
): Subscription<Pool>

annotation class Pool(
        val interval: Long,
        val unit: TimeUnit,
        val priority: Priority = Priority.Normal
)

class PoolSubscriptionClient: SubscriptionClient<Pool>, ListenerAdapter() {
    private val executor = Executors.newCachedThreadPool()
    override val subscriptions = CopyOnWriteArrayList<Subscription<Pool>>()

    override fun onReady() {
        subscriptions.sortBy { it.annotation.priority }
    }

    override fun onReady(event: ReadyEvent) {
        subscriptions.forEach {
            executor.execute {
                while (true) {
                    try {
                        it.execute()
                    } catch (e: Exception) {
                        logger.error(e) { "[${it.instance.javaClass.name}#${it.method.name}] 実行中に例外が発生しました." }
                    } finally {
                        it.annotation.unit.sleep(it.annotation.interval)
                    }
                }
            }
        }
    }
}
