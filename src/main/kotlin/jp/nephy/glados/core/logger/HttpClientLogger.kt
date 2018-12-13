package jp.nephy.glados.core.logger

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.features.HttpClientFeature
import io.ktor.client.response.HttpReceivePipeline
import io.ktor.client.response.HttpResponse
import io.ktor.http.charset
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.util.AttributeKey
import jp.nephy.glados.GLaDOS
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.io.ByteChannel
import kotlinx.coroutines.io.close
import kotlinx.coroutines.io.readRemaining
import kotlinx.coroutines.launch
import kotlinx.io.core.readText

class HttpClientLogger(private val config: Config) {
    private inline fun log(builder: StringBuilder.() -> Unit) {
        config.onMessage(buildString(builder).trimEnd())
    }

    private suspend fun logResponse(response: HttpResponse) {
        if (config.categories.isEmpty()) {
            return
        }

        log {
            if (LogCategory.Summary in config.categories) {
                appendln("${response.status.value} ${response.status.description}: ${response.version} ${response.call.request.method.value} ${response.call.request.url}")
            }

            if (LogCategory.RequestHeader in config.categories) {
                appendln("Request Headers:")
                for ((key, values) in response.call.request.headers.entries()) {
                    appendln("    $key: ${values.joinToString("; ")}")
                }
            }

            if (LogCategory.RequestBody in config.categories) {
                val content = response.call.request.content
                val charset = content.contentType?.charset() ?: Charsets.UTF_8

                val text = when (content) {
                    is OutgoingContent.WriteChannelContent -> {
                        val textChannel = ByteChannel()
                        GlobalScope.launch(GLaDOS.dispatcher) {
                            content.writeTo(textChannel)
                            textChannel.close()
                        }
                        textChannel.readRemaining().readText(charset = charset)
                    }
                    is OutgoingContent.ReadChannelContent -> {
                        content.readFrom().readRemaining().readText(charset = charset)
                    }
                    is OutgoingContent.ByteArrayContent -> String(content.bytes(), charset = charset)
                    else -> null
                }.orEmpty()

                appendln("Request Body: ${content.contentType}")
                appendln(text.ifBlank { "    (Empty)" })
            }

            appendln()

            if (LogCategory.ResponseHeader in config.categories) {
                appendln("Response Headers:")
                for ((key, values) in response.headers.entries()) {
                    appendln("    $key: ${values.joinToString("; ")}")
                }
            }

            if (LogCategory.ResponseBody in config.categories) {
                val contentType = response.contentType()

                appendln("Response Body: $contentType / Length = ${response.contentLength()}")
            }
        }
    }

    data class Config(val categories: List<LogCategory>, val onMessage: (String) -> Unit) {
        class Builder {
            private val categories = mutableListOf<LogCategory>()
            fun all() {
                categories += LogCategory.values()
            }

            fun of(vararg target: LogCategory) {
                categories += target
            }

            private var callback: (String) -> Unit = {}
            fun onMessage(msg: (String) -> Unit) {
                callback = msg
            }

            internal fun build(): Config {
                return Config(categories, callback)
            }
        }
    }

    companion object: HttpClientFeature<Config.Builder, HttpClientLogger> {
        override val key: AttributeKey<HttpClientLogger> = AttributeKey("HttpClientLogger")

        override fun prepare(block: Config.Builder.() -> Unit): HttpClientLogger {
            return HttpClientLogger(Config.Builder().apply(block).build())
        }

        override fun install(feature: HttpClientLogger, scope: HttpClient) {
            scope.receivePipeline.intercept(HttpReceivePipeline.After) { response ->
                launch {
                    feature.logResponse(response)
                }

                proceedWith(context.response)
            }
        }
    }
}

enum class LogCategory {
    Summary,

    RequestHeader, RequestBody,

    ResponseHeader, ResponseBody
}

fun HttpClientConfig<*>.installDefaultLogger() {
    install(HttpClientLogger) {
        if (GLaDOS.isDebugMode) {
            all()
        } else {
            of(LogCategory.Summary)
        }

        val logger = SlackLogger("GLaDOS.HttpClient", "#glados-http-client")
        onMessage {
            logger.info { it }
        }
    }
}
