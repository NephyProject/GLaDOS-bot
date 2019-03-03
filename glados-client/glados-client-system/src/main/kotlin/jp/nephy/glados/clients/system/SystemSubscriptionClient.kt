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

package jp.nephy.glados.clients.system

import jp.nephy.glados.api.Event
import jp.nephy.glados.api.Plugin
import jp.nephy.glados.clients.GLaDOSSubscriptionClient
import jp.nephy.glados.clients.system.events.ReadyEvent
import jp.nephy.glados.clients.system.events.ShutdownEvent
import jp.nephy.glados.clients.system.events.SystemEventBase
import jp.nephy.glados.clients.utils.eventClass
import jp.nephy.glados.clients.utils.invoke
import jp.nephy.glados.clients.utils.subscriptions
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf

object SystemSubscriptionClient: GLaDOSSubscriptionClient<SystemEvent, SystemEventBase, SystemSubscription>() {
    override fun create(plugin: Plugin, function: KFunction<*>, eventClass: KClass<*>): SystemSubscription? {
        if (!eventClass.isSubclassOf(SystemEventBase::class)) {
            return null
        }

        val annotation = function.findAnnotation() ?: defaultSystemEventAnnotation
        return SystemSubscription(plugin, function, annotation)
    }

    override fun canHandle(event: Event): Boolean {
        return event is SystemEventBase
    }

    override fun start() {
        subscriptions.filter { it.eventClass == ReadyEvent::class }.forEach {
            launch {
                it.invoke(ReadyEvent)
            }
        }
    }

    override fun stop() {
        subscriptions.filter { it.eventClass == ShutdownEvent::class }.forEach {
            launch {
                it.invoke(ShutdownEvent)
            }
        }
    }
}
