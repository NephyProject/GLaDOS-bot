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

package jp.nephy.glados.clients.utils

import jp.nephy.glados.api.Event
import jp.nephy.glados.api.Subscription
import jp.nephy.glados.api.SubscriptionStorage

/**
 * plusAssign (+=) syntax sugar for add(subscription: S).
 */
suspend operator fun <A: Annotation, E: Event, S: Subscription<A, E>> SubscriptionStorage<A, E, S>.plusAssign(subscription: S) {
    add(subscription)
}

/**
 * plusAssign (+=) syntax sugar for add(subscriptions: Collection<S>).
 */
suspend operator fun <A: Annotation, E: Event, S: Subscription<A, E>> SubscriptionStorage<A, E, S>.plusAssign(subscriptions: Collection<S>) {
    add(subscriptions)
}

/**
 * minusAssign (-=) syntax sugar for remove(subscription: S).
 */
suspend operator fun <A: Annotation, E: Event, S: Subscription<A, E>> SubscriptionStorage<A, E, S>.minusAssign(subscription: S) {
    remove(subscription)
}

/**
 * minusAssign (-=) syntax sugar for remove(subscriptions: Collection<S>).
 */
suspend operator fun <A: Annotation, E: Event, S: Subscription<A, E>> SubscriptionStorage<A, E, S>.minusAssign(subscriptions: Collection<S>) {
    remove(subscriptions)
}
