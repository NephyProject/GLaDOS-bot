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

import jp.nephy.glados.api.GLaDOS
import jp.nephy.glados.api.Logger
import jp.nephy.glados.clients.logger.of
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.MessageAction
import java.util.concurrent.TimeUnit

suspend fun <T> RestAction<T>.await() = suspendCancellableCoroutine<T> {
    val result = runCatching {
        complete()
    }
    it.resumeWith(result)
}

fun <T> RestAction<T>.launch(timeout: Long = 0, unit: TimeUnit = TimeUnit.SECONDS, callback: suspend (T) -> Unit = {}) = GLaDOS.launch {
    try {
        await().also {
            delay(unit.toMillis(timeout))
            callback(it)
        }
    } catch (e: Throwable) {
        val logger = Logger.of("GLaDOS.SubscriptionClient.Discord.Coroutine")
        logger.error(e) { "非同期処理中に例外が発生しました。" }
    }
}

suspend fun MessageAction.awaitAndDelete(timeout: Long, unit: TimeUnit) {
    await().also {
        delay(unit.toMillis(timeout))
        it.delete().await()
    }
}

fun MessageAction.launchAndDelete(timeout: Long, unit: TimeUnit) = launch(timeout, unit) {
    it.delete().await()
}
