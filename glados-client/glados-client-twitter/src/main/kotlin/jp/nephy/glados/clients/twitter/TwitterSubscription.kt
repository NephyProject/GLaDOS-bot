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

package jp.nephy.glados.clients.twitter

import jp.nephy.glados.api.GLaDOS
import jp.nephy.glados.api.Plugin
import jp.nephy.glados.api.annotations.Priority
import jp.nephy.glados.api.config
import jp.nephy.glados.clients.GLaDOSSubscription
import jp.nephy.glados.clients.twitter.config.TwitterAccount
import jp.nephy.glados.clients.twitter.config.twitter
import jp.nephy.glados.clients.twitter.config.twitterAccount
import jp.nephy.glados.clients.twitter.event.TwitterEventBase
import kotlin.reflect.KFunction

class TwitterSubscription(
    override val plugin: Plugin, override val function: KFunction<*>, override val annotation: TwitterEvent
): GLaDOSSubscription<TwitterEvent, TwitterEventBase>() {
    override val priority: Priority
        get() = annotation.priority
}

val TwitterSubscription.accounts: List<TwitterAccount>
    get() = annotation.accounts.toList().ifEmpty {
        GLaDOS.config.twitter.keys
    }.map {
        GLaDOS.config.twitterAccount(it)
    }
