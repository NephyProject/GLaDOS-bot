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

@file:Suppress("UNUSED")

package jp.nephy.glados.clients.utils

import jp.nephy.glados.api.Event
import jp.nephy.glados.api.Subscription
import jp.nephy.glados.api.annotations.ExperimentalFeature
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure
import kotlin.system.measureTimeMillis

/**
 * The name of Subscription.
 */
val Subscription<*, *>.name: String
    get() = function.name

/**
 * Full name of Subscription.
 */
val Subscription<*, *>.fullname: String
    get() = "${plugin.fullName}#$name"

/**
 * The flag whether Subscription is experimental.
 */
val Subscription<*, *>.isExperimental: Boolean
    get() = plugin.isExperimental || function.findAnnotation<ExperimentalFeature>() != null

/**
 * Event class of Subscription function parameter.
 */
val Subscription<*, *>.eventClass: KClass<*>
    get() = function.valueParameters.first().type.jvmErasure

/**
 * Subscription invoke operator.
 */
suspend operator fun <E: Event> Subscription<*, E>.invoke(event: E) {
    runCatching {
        val timeMillis = measureTimeMillis {
            function.callSuspend(plugin, event)
        }

        logger.trace { "$timeMillis ms で終了しました。" }
    }.onSuccess {
        onSuccess(event)
    }.onFailure {
        onFailure(it, event)
    }
}
