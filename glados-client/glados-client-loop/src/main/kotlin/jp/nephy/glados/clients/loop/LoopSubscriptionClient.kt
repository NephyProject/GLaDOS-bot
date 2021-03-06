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

package jp.nephy.glados.clients.loop

import jp.nephy.glados.GLaDOSSubscriptionClient
import jp.nephy.glados.api.Plugin
import jp.nephy.glados.api.Priority
import jp.nephy.glados.clients.fullName
import jp.nephy.glados.clients.invoke
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation

/**
 * LoopSubscriptionClient.
 */
@Suppress("UNUSED")
object LoopSubscriptionClient: GLaDOSSubscriptionClient<Loop, LoopEvent, LoopSubscription>() {
    override val priority: Priority
        get() = Priority.Lower
    
    override fun create(plugin: Plugin, function: KFunction<*>, eventClass: KClass<*>): LoopSubscription? {
        if (eventClass != LoopEvent::class) {
            return null
        }

        val annotation = function.findAnnotation<Loop>()
        if (annotation == null) {
            logger.warn { "関数: \"${plugin.fullName}#${function.name}\" は @Loop が付与されていません。スキップします。" }
            return null
        }
        
        return LoopSubscription(plugin, function, annotation)
    }
    
    override fun start() {
        for (subscription in storage.subscriptions) {
            subscription.start()
        }
    }

    override fun stop() {
        for (subscription in storage.subscriptions) {
            subscription.stop()
        }
    }

    override fun onSubscriptionLoaded(subscription: LoopSubscription) {
        subscription.start()
    }

    override fun onSubscriptionUnloaded(subscription: LoopSubscription) {
        subscription.stop()
    }

    private val jobs = ConcurrentHashMap<LoopSubscription, Job>()

    private fun LoopSubscription.start() {
        // TODO
        jobs[this] = launch {
            var count = 0L
            val event = LoopEvent(count, this@start)

            while (isActive) {
                if (count == Long.MAX_VALUE) {
                    logger.warn { "ループ回数が上限に達したため, 終了しました。" }
                    break
                }

                try {
                    invoke(event.copy(count=++count))
                    delay(intervalMillis)
                } catch (e: CancellationException) {
                    break
                }
            }

            logger.debug { "終了しました。" }
        }

        logger.debug { "開始しました。" }
    }
    
    private fun LoopSubscription.stop() {
        jobs[this]?.cancel()
    }
}
