package jp.nephy.glados.core.logger

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.response.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import jp.nephy.glados.GLaDOS
import jp.nephy.jsonkt.*
import kotlinx.coroutines.*
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.writeStringUtf8
import mu.KotlinLogging
import java.io.Closeable
import kotlin.coroutines.CoroutineContext

object SlackWebhook: Closeable, CoroutineScope {
    private val logger = KotlinLogging.logger("GLaDOS.logger.SlackWebhook")
    private val httpClient = HttpClient()
    private val masterJob = Job()

    override val coroutineContext: CoroutineContext
        get() = GLaDOS.dispatcher + masterJob

    fun message(channel: String? = null, builder: MessageBuilder.() -> Unit): Job {
        return launch {
            messageAwait(channel, builder)
        }
    }

    suspend fun messageAwait(channel: String? = null, builder: MessageBuilder.() -> Unit): Boolean {
        val requestBody = MessageBuilder(channel).apply(builder).build()
        repeat(3) {
            try {
                httpClient.post<HttpResponse>(GLaDOS.config.slackWebhookUrl) {
                    body = requestBody
                }

                return true
            } catch (e: CancellationException) {
                return false
            } catch (e: Throwable) {
                logger.error(e) { "ペイロードの送信に失敗しました。(${it + 1}/3)" }
            }

            try {
                delay(3000)
            } catch (e: CancellationException) {
                return false
            }
        }

        return false
    }

    class MessageBuilder(channel: String? = null) {
        companion object {
            private val urlRegex = "^http(s)?://.+".toRegex()
        }

        private val payload = mutableMapOf<String, String>()

        init {
            if (channel != null) {
                payload["channel"] = if (channel.startsWith("#")) {
                    channel
                } else {
                    "#$channel"
                }
            }
        }

        fun text(value: String) {
            payload["text"] = value
        }

        fun text(text: () -> String) {
            payload["text"] = text()
        }

        fun textBuilder(builder: StringBuilder.() -> Unit) {
            payload["text"] = buildString(builder)
        }

        fun username(value: String) {
            payload["username"] = value
        }

        fun icon(value: String) {
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
                    channel.writeStringUtf8(payload.toJsonString())
                }
            }
        }
    }

    override fun close() {
        httpClient.close()
        runBlocking {
            masterJob.cancelChildren()
            masterJob.cancelAndJoin()
        }
    }
}
