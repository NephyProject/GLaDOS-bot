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

import io.ktor.application.call
import io.ktor.features.origin
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import jp.nephy.glados.api.annotations.Priority
import jp.nephy.glados.clients.utils.subscriptions
import jp.nephy.glados.clients.web.PathType
import jp.nephy.glados.clients.web.routing.WebRoutingSubscriptionClient

fun Route.sitemap() {
    get("/sitemap.xml") {
        val allRouting = "all" in call.parameters
        call.respondText(ContentType.Text.Xml) {
            buildString {
                appendln("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                appendln("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">")
                WebRoutingSubscriptionClient.subscriptions.filter { allRouting || (it.domain == null || it.domain == call.request.origin.host) && !it.banRobots && it.pathType == PathType.Normal }.forEach { page ->
                    appendln("    <url>")
                    appendln("        <loc>${call.request.origin.scheme}://${call.request.origin.host}${page.path}</loc>")
                    appendln("        <changefreq>${page.updateFrequency.name.toLowerCase()}</changefreq>")
                    appendln(
                        "        <priority>${when (page.priority) {
                            Priority.Highest -> "1.0"
                            Priority.Higher -> "0.8"
                            Priority.High -> "0.7"
                            Priority.Normal -> "0.5"
                            Priority.Low -> "0.3"
                            Priority.Lower -> "0.2"
                            Priority.Lowest -> "0.1"
                        }}</priority>"
                    )
                    appendln("    </url>")
                }
                appendln("</urlset>")
            }
        }
    }
}

fun Route.robotsTxt() {
    get("/robots.txt") {
        call.respondText(ContentType.Text.Plain) {
            buildString {
                WebRoutingSubscriptionClient.subscriptions.filter { (it.domain == null || it.domain == call.request.origin.host) && it.banRobots && it.pathType == PathType.Normal }.forEach {
                    appendln("User-agent: *")
                    appendln("Disallow: ${it.path}")
                }
                appendln("User-agent: *")
                appendln("Sitemap: /sitemap.xml")
            }
        }
    }
}
