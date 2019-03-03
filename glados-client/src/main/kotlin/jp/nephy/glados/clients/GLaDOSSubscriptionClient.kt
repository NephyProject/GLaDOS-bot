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

package jp.nephy.glados.clients

import jp.nephy.glados.api.*
import jp.nephy.glados.clients.logger.of
import jp.nephy.glados.clients.utils.name
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/**
 * GLaDOS SubscriptionClient base class.
 */
abstract class GLaDOSSubscriptionClient<A: Annotation, E: Event, S: Subscription<A, E>>: SubscriptionClient<A, E, S>, GLaDOSCoroutineScope() {
    final override val logger: Logger = Logger.of("GLaDOS.SubscriptionClient.$name")

    final override val storage: SubscriptionStorage<A, E, S> = object: SubscriptionStorage<A, E, S> {
        private val mutex = Mutex()
        private val underlyingStorage = sortedSetOf<S>(Comparator { o1, o2 ->
            o1.priority.compareTo(o2.priority)
        })

        override val subscriptions: Set<S>
            get() = underlyingStorage.toSet()

        override suspend fun add(subscription: S): Boolean {
            return mutex.withLock(underlyingStorage) {
                underlyingStorage.add(subscription)
            }
        }

        override suspend fun add(subscriptions: Collection<S>): Boolean {
            return mutex.withLock(underlyingStorage) {
                underlyingStorage.addAll(subscriptions)
            }
        }

        override suspend fun remove(subscription: S): Boolean {
            return mutex.withLock(underlyingStorage) {
                underlyingStorage.remove(subscription)
            }
        }

        override suspend fun remove(subscriptions: Collection<S>): Boolean {
            return mutex.withLock(underlyingStorage) {
                underlyingStorage.removeAll(subscriptions)
            }
        }
    }

    final override suspend fun register(plugin: Plugin, function: KFunction<*>, eventClass: KClass<*>): Boolean {
        val subscription = create(plugin, function, eventClass) ?: return false

        return storage.add(subscription)
    }
}
