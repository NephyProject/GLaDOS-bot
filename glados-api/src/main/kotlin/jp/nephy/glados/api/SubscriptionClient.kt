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

package jp.nephy.glados.api

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/**
 * GLaDOS SubscriptionClient model.
 */
interface SubscriptionClient<A: Annotation, E: Event, S: Subscription<A, E>>: CoroutineScope {
    /**
     * Creates new Subscription from function.
     * Returning null means SubscriptionClient cannot handle its Subscription.
     */
    fun create(plugin: Plugin, function: KFunction<*>, eventClass: KClass<*>): S?

    /**
     * Registers function as Subscription.
     */
    suspend fun register(plugin: Plugin, function: KFunction<*>, eventClass: KClass<*>): Boolean

    /**
     * If true, this SubscriptionClient can handle the event.
     * Constantly returning false means SubscriptionClient does not use EventBus.
     */
    fun canHandle(event: Event): Boolean = false
    
    /**
     * Starts SubscriptionClient.
     */
    fun start()

    /**
     * Stops SubscriptionClient.
     */
    fun stop()

    /**
     * Called when new Subscription was registered.
     */
    fun onSubscriptionLoaded(subscription: S) {}

    /**
     * Called when existing Subscription was unregistered.
     */
    fun onSubscriptionUnloaded(subscription: S) {}
    
    /**
     * SubscriptionStorage.
     */
    val storage: SubscriptionStorage<A, E, S>

    /**
     * SubscriptionClient logger.
     */
    val logger: Logger
    
    /**
     * Coroutine context.
     */
    override val coroutineContext: CoroutineContext
}
