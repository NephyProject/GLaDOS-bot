/*
 * The MIT License (MIT)
 *
 *     Copyright (c) 2017-2019 Nephy Project Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package jp.nephy.glados.clients.discord.extensions

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.hooks.SubscribeEvent
import java.io.Closeable
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

object DiscordEventWaiter: EventListener {
    private val mutex = Mutex()
    private val waitingEvents = CopyOnWriteArraySet<WaitingEvent<Event>>()

    @Suppress("UNCHECKED_CAST")
    data class WaitingEvent<T: Event>(val eventClass: KClass<T>, val predicate: (T) -> Boolean): Closeable {
        private val mutex = Mutex()

        private object DummyOwner

        private lateinit var event: T

        init {
            mutex.tryLock(DummyOwner)
        }

        suspend fun start() {
            DiscordEventWaiter.mutex.withLock {
                waitingEvents += this as WaitingEvent<Event>
            }
        }

        override fun close() {
            runBlocking {
                DiscordEventWaiter.mutex.withLock {
                    waitingEvents -= this@WaitingEvent as WaitingEvent<Event>
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
    fun onEvent(event: Event) {
        if (waitingEvents.isEmpty()) {
            return
        }

        for (waitingEvent in waitingEvents.filter { event::class.isSubclassOf(it.eventClass) && it.predicate(event) }) {
            waitingEvent.provide(event)
        }
    }

    @Suppress("ResultIsResult")
    suspend inline fun <reified T: Event> waitCatching(timeout: Long? = null, unit: TimeUnit = TimeUnit.SECONDS, noinline predicate: (T) -> Boolean): Result<T> {
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
