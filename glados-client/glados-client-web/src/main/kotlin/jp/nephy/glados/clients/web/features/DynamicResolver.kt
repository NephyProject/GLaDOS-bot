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

package jp.nephy.glados.clients.web.features

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.features.origin
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.decodeURLPart
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.request.userAgent
import io.ktor.response.respond
import io.ktor.response.respondFile
import io.ktor.util.AttributeKey
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.pipeline.PipelineContext
import jp.nephy.glados.api.GLaDOS
import jp.nephy.glados.api.Logger
import jp.nephy.glados.api.config
import jp.nephy.glados.api.resourceFile
import jp.nephy.glados.clients.logger.of
import jp.nephy.glados.clients.utils.invoke
import jp.nephy.glados.clients.utils.subscriptions
import jp.nephy.glados.clients.web.config.web
import jp.nephy.glados.clients.web.extensions.effectiveHost
import jp.nephy.glados.clients.web.extensions.url
import jp.nephy.glados.clients.web.routing.WebRoutingSubscriptionClient

internal object DynamicResolver: ApplicationFeature<ApplicationCallPipeline, DynamicResolver.Configuration, DynamicResolver.Resolver> {
    override val key = AttributeKey<Resolver>("DynamicResolver")

    override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): Resolver {
        return DynamicResolver.Resolver.apply {
            pipeline.intercept(ApplicationCallPipeline.Call) {
                handleRequest()
            }
        }
    }

    object Configuration

    object Resolver {
        private val accessLogger = Logger.of("GLaDOS.Web.Access", "#glados-web")
        private val accessStaticLogger = Logger.of("GLaDOS.Web.AccessStatic", "#glados-web-static")

        @UseExperimental(KtorExperimentalAPI::class)
        suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
            val host = call.request.effectiveHost
            val path = call.request.path().removePrefix("/").removeSuffix("/").decodeURLPart()

            if (call.request.httpMethod == HttpMethod.Get) {
                val searchPaths = listOf(
                    arrayOf(host, path), arrayOf(host, path, "index.html"), arrayOf(path)
                )
                val file = searchPaths.map { GLaDOS.resourceFile("static", *it) }.firstOrNull { it.isFile && it.exists() }
                if (file != null) {
                    call.respondFile(file)
                    callLogging(accessStaticLogger)
                    return
                }
            }

            val routing = WebRoutingSubscriptionClient.subscriptions.find { it.canHandle(call) } ?: return
            val event = routing.makeEvent(this)

            if (call.request.httpMethod != HttpMethod.Options) {
                routing.invoke(event)
                callLogging(accessLogger)
            } else {
                call.respond(HttpStatusCode.OK, "")
            }
        }

        private fun PipelineContext<Unit, ApplicationCall>.callLogging(logger: Logger) {
            val httpStatus = call.response.status() ?: HttpStatusCode.OK
            val userAgent = call.request.userAgent()
            val remoteHost = call.request.origin.remoteHost
            if (!userAgent.isNullOrBlank() && GLaDOS.config.web.ignoreUserAgents.any { it in userAgent } || GLaDOS.config.web.ignoreIpAddressRanges.any { it in remoteHost }) {
                return
            }

            logger.info {
                buildString {
                    appendln("${httpStatus.value} ${httpStatus.description}: ${call.request.origin.version} ${call.request.origin.method.value} ${call.request.url}")
                    append("<- $remoteHost")
                    if (!userAgent.isNullOrBlank()) {
                        append(" ($userAgent)")
                    }
                }
            }
        }
    }
}
