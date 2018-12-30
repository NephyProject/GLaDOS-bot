package jp.nephy.glados.core.plugins.extensions.jda

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import net.dv8tion.jda.core.hooks.SubscribeEvent
import java.io.Closeable
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

typealias JDAEvent = net.dv8tion.jda.core.events.Event

object DiscordEventWaiter: EventListener {
    private val mutex = Mutex()
    private val waitingEvents = CopyOnWriteArraySet<WaitingEvent<JDAEvent>>()

    @Suppress("UNCHECKED_CAST")
    data class WaitingEvent<T: JDAEvent>(val eventClass: KClass<T>, val predicate: (T) -> Boolean): Closeable {
        private val mutex = Mutex()
        private object DummyOwner
        private lateinit var event: T

        init {
            mutex.tryLock(DummyOwner)
        }

        suspend fun start() {
            DiscordEventWaiter.mutex.withLock {
                waitingEvents += this as WaitingEvent<JDAEvent>
            }
        }

        override fun close() {
            runBlocking {
                DiscordEventWaiter.mutex.withLock {
                    waitingEvents -= this@WaitingEvent as WaitingEvent<JDAEvent>
                }
            }
        }

        fun provide(event: T) {
            this.event = event
            mutex.unlock(DummyOwner)
        }

        suspend fun await(): T {
            mutex.lock()
            return event
        }
    }

    @SubscribeEvent
    fun onEvent(event: JDAEvent) {
        if (waitingEvents.isEmpty()) {
            return
        }

        for (waitingEvent in waitingEvents.filter { event::class.isSubclassOf(it.eventClass) && it.predicate(event) }) {
            waitingEvent.provide(event)
        }
    }

    @Suppress("ResultIsResult")
    suspend inline fun <reified T: JDAEvent> waitCatching(timeout: Long? = null, unit: TimeUnit = TimeUnit.SECONDS, noinline predicate: (T) -> Boolean): Result<T> {
        val waitingEvent = WaitingEvent(T::class, predicate)

        return waitingEvent.use {
            runCatching {
                it.start()

                val timeoutMillis = unit.toMillis(timeout ?: 0)
                if (timeoutMillis > 0) {
                    withTimeout(timeoutMillis) {
                        it.await()
                    }
                } else {
                    it.await()
                }
            }
        }
    }
}
