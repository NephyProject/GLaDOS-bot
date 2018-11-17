package jp.nephy.glados.core

import io.ktor.client.request.post
import io.ktor.client.response.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import jp.nephy.glados.dispatcher
import jp.nephy.glados.httpClient
import jp.nephy.jsonkt.*
import kotlinx.coroutines.*
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.writeStringUtf8
import mu.KotlinLogging
import java.io.Closeable
import java.util.concurrent.TimeUnit

class SlackWebhook(private val url: String, private val retryInterval: Long = 3, private val retryIntervalUnit: TimeUnit = TimeUnit.SECONDS, private val maxRetries: Int = 3): Closeable {
    private val logger = KotlinLogging.logger("SlackWebhook")
    private val masterJob = Job()

    fun message(channel: String? = null, builder: MessageBuilder.() -> Unit): Job {
        return GlobalScope.launch(dispatcher + masterJob) {
            messageAwait(channel, builder)
        }
    }

    suspend fun messageAwait(channel: String? = null, builder: MessageBuilder.() -> Unit): Boolean {
        val requestBody = MessageBuilder(channel).apply(builder).build()
        repeat(maxRetries) {
            try {
                httpClient.post<HttpResponse>(url) {
                    body = requestBody
                }

                return true
            } catch (e: CancellationException) {
                return false
            } catch (e: Throwable) {
                logger.error(e) { "Sending payload to Slack was failed. (${it + 1}/$maxRetries)" }
            }

            try {
                delay(retryIntervalUnit.toMillis(retryInterval))
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
        runBlocking {
            masterJob.cancelChildren()
            masterJob.cancelAndJoin()
        }
    }
}
