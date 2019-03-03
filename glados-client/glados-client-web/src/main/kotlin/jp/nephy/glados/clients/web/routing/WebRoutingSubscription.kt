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

package jp.nephy.glados.clients.web.routing

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.features.origin
import io.ktor.http.HttpMethod
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.util.pipeline.PipelineContext
import jp.nephy.glados.api.Plugin
import jp.nephy.glados.api.annotations.Priority
import jp.nephy.glados.clients.GLaDOSSubscription
import jp.nephy.glados.clients.web.PathType
import jp.nephy.glados.clients.web.event.WebAccessEvent
import jp.nephy.glados.clients.web.extensions.meta.SitemapUpdateFrequency
import kotlin.reflect.KFunction

class WebRoutingSubscription(
    override val plugin: Plugin, override val function: KFunction<*>, override val annotation: WebRouting
): GLaDOSSubscription<WebRouting, WebAccessEvent>() {
    companion object {
        private val fragmentPattern = "^\\{(.+)}$".toRegex()
    }

    private val httpMethods: List<HttpMethod>
        get() = annotation.methods.ifEmpty { jp.nephy.glados.clients.web.HttpMethod.values() }.map { it.ktor }
    val domain: String?
        get() = annotation.domain.ifBlank { null }
    val updateFrequency: SitemapUpdateFrequency
        get() = annotation.updateFrequency
    val banRobots: Boolean
        get() = annotation.banRobots

    val pathType: PathType
        get() = annotation.pathType
    val path: String
        get() = "/${annotation.path.removePrefix("/").removeSuffix("/").trim()}"

    private val regexPath: Regex
        get() = "^$path$".toRegex(annotation.regexOptions.toSet())

    private val fragments = if (pathType == PathType.Pattern) {
        path.split("/").mapIndexedNotNull { i, it ->
            (fragmentPattern.matchEntire(it)?.groupValues?.get(1) ?: return@mapIndexedNotNull null) to i
        }.toMap()
    } else {
        null
    }.orEmpty()

    fun canHandle(call: ApplicationCall): Boolean {
        if ((call.request.httpMethod !in httpMethods && call.request.httpMethod != HttpMethod.Options) || (domain != null && domain != call.request.origin.host)) {
            return false
        }

        val requestPath = call.request.path()
        return when (annotation.pathType) {
            PathType.Normal -> {
                path == requestPath
            }
            PathType.Regex -> {
                regexPath.matches(requestPath)
            }
            PathType.Pattern -> {
                val (expectPaths, actualPaths) = path.split("/") to requestPath.split("/")
                if (expectPaths.size != actualPaths.size) {
                    return false
                }

                for ((expected, actual) in expectPaths.zip(actualPaths)) {
                    if (!fragmentPattern.matches(expected) && expected != actual) {
                        return false
                    }
                }

                return true
            }
        }
    }

    fun makeEvent(context: PipelineContext<Unit, ApplicationCall>): WebAccessEvent {
        val matchResult = if (pathType == PathType.Regex) {
            regexPath.matchEntire(context.call.request.path())
        } else {
            null
        }
        val fragments = if (pathType == PathType.Pattern) {
            val currentFragments = context.call.request.path().split("/")

            fragments.map { (name, index) ->
                name to currentFragments[index]
            }.toMap()
        } else {
            null
        }.orEmpty()

        return WebAccessEvent(context, matchResult, fragments)
    }

    override val priority: Priority
        get() = annotation.priority
}
