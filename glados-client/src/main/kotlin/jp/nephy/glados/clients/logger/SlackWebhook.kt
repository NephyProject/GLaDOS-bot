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

package jp.nephy.glados.clients.logger

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.response.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import jp.nephy.glados.api.GLaDOS
import jp.nephy.glados.api.config
import jp.nephy.glados.clients.GLaDOSCoroutineScope
import jp.nephy.jsonkt.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.writeStringUtf8
import kotlinx.coroutines.launch
import mu.KotlinLogging

object SlackWebhook: GLaDOSCoroutineScope() {
    private val logger = KotlinLogging.logger("GLaDOS.Logger.SlackWebhook")

    private val httpClient = HttpClient()
    
    fun message(channel: String? = null, builder: MessageBuilder.() -> Unit): Job {
        return launch {
            messageAwait(channel, builder)
        }
    }

    private suspend fun messageAwait(channel: String? = null, builder: MessageBuilder.() -> Unit) {
        val requestBody = MessageBuilder(channel).apply(builder).build()
        repeat(3) {
            try {
                httpClient.post<HttpResponse>(GLaDOS.config.logging.slackWebhookUrl ?: return) {
                    body = requestBody
                }

                return
            } catch (e: CancellationException) {
                return
            } catch (e: Throwable) {
                logger.error(e) { "ペイロードの送信に失敗しました。(${it + 1}/3)" }
            }

            try {
                delay(3000)
            } catch (e: CancellationException) {
                return
            }
        }
    }

    private val urlRegex = "^http(s)?://.+".toRegex()
    
    class MessageBuilder(slackChannel: String? = null) {
        private val payload = mutableMapOf<String, String?>()

        var channel by payload
        
        init {
            if (slackChannel != null) {
                channel = if (slackChannel.startsWith("#")) {
                    slackChannel
                } else {
                    "#$slackChannel"
                }
            }
        }

        var text: String? by payload
        
        var username: String? by payload
        
        var icon: String?
            get() = payload["icon_url"] ?: payload["icon_emoji"]
            set(value) {
                if (value == null) {
                    return
                }
                
                if (urlRegex.matches(value)) {
                    payload["icon_url"] = value
                } else {
                    payload["icon_emoji"] = value
                }
            }

        internal fun build(): OutgoingContent.WriteChannelContent {
            return object: OutgoingContent.WriteChannelContent() {
                override val contentType = ContentType.Application.Json

                override suspend fun writeTo(channel: ByteWriteChannel) {
                    channel.writeStringUtf8(payload.filterValues { it != null }.toJsonObject().toJsonString())
                }
            }
        }
    }
}

fun SlackWebhook.MessageBuilder.text(block: () -> Any?) {
    text = block.toStringSafe()
}
