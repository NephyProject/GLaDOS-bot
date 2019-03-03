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

package jp.nephy.glados.clients.web

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.AutoHeadResponse
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.sessions.Sessions
import jp.nephy.glados.api.GLaDOS
import jp.nephy.glados.api.attributes
import jp.nephy.glados.api.config
import jp.nephy.glados.clients.utils.invoke
import jp.nephy.glados.clients.utils.subscriptions
import jp.nephy.glados.clients.web.config.web
import jp.nephy.glados.clients.web.error.WebErrorPageSubscriptionClient
import jp.nephy.glados.clients.web.error.domain
import jp.nephy.glados.clients.web.error.statuses
import jp.nephy.glados.clients.web.event.WebAccessEvent
import jp.nephy.glados.clients.web.event.WebErrorEvent
import jp.nephy.glados.clients.web.extensions.effectiveHost
import jp.nephy.glados.clients.web.features.DynamicResolver
import jp.nephy.glados.clients.web.features.robotsTxt
import jp.nephy.glados.clients.web.features.sitemap
import jp.nephy.glados.clients.web.routing.WebRoutingSubscriptionClient
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

val GLaDOS.Companion.webServer: ApplicationEngine
    get() = GLaDOS.attributes["web"]

internal fun initializeWebApplication() {
    if ("web" in GLaDOS.attributes) {
        return
    }

    GLaDOS.attributes["web"] = {
        embeddedServer(Netty, host = GLaDOS.config.web.host, port = GLaDOS.config.web.port) {
            install(XForwardedHeaderSupport)
            install(AutoHeadResponse)
            install(DefaultHeaders) {
                header(HttpHeaders.Server, "GLaDOS-bot")
                header("X-Powered-By", "GLaDOS-bot (+https://github.com/NephyProject/GLaDOS-bot)")
            }

            install(StatusPages) {
                exception<Exception> { e ->
                    WebRoutingSubscriptionClient.logger.error(e) { "Internal server error occurred." }
                    call.respond(HttpStatusCode.InternalServerError)
                }

                for (status in WebErrorPageSubscriptionClient.subscriptions.flatMap { it.statuses }.distinct()) {
                    status(status) {
                        val subscription = WebErrorPageSubscriptionClient.subscriptions.firstOrNull { status in it.statuses && (it.domain == null || it.domain == call.request.effectiveHost) } ?: return@status call.respond(status)

                        val accessEvent = WebAccessEvent(this, null, emptyMap())
                        subscription.invoke(WebErrorEvent(accessEvent, status))
                    }
                }
            }
            install(DynamicResolver)
            install(Sessions) {
                runBlocking {
                    // TODO
                    //                    for (subscription in Session.activeSubscriptions) {
                    //                        cookie(subscription.sessionName, subscription.sessionClass, SessionStorageMemory()) {
                    //                            subscription.invoke(this)
                    //                        }
                    //                    }
                }
            }
            install(Routing) {
                sitemap()
                robotsTxt()
            }
        }.start(wait = false)
    }
}

internal fun disposeWebApplication() {
    if ("web" !in GLaDOS.attributes) {
        return
    }

    val app: ApplicationEngine = GLaDOS.attributes["web"]
    app.stop(1, 5, TimeUnit.SECONDS)

    GLaDOS.attributes.dispose("web")
}
