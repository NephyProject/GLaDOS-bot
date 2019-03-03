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

import jp.nephy.glados.api.Event
import jp.nephy.glados.api.Logger
import jp.nephy.glados.api.Subscription
import jp.nephy.glados.clients.logger.of
import jp.nephy.glados.clients.utils.fullname
import java.lang.reflect.InvocationTargetException

/**
 * GLaDOS Subscription base class.
 */
abstract class GLaDOSSubscription<A: Annotation, E: Event>: Subscription<A, E> {
    final override val logger: Logger = Logger.of("GLaDOS.$fullname")

    override fun onFailure(throwable: Throwable, event: E) {
        val t = (throwable as? InvocationTargetException)?.targetException ?: throwable
        
        logger.error(t) { "実行中に例外が発生しました。(${event::class.simpleName})" }
    }
}
