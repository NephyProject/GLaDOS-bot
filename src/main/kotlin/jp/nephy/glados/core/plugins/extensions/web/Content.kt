package jp.nephy.glados.core.plugins.extensions.web

import io.ktor.application.ApplicationCall
import io.ktor.html.Template
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.withCharset
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.cio.bufferedWriter
import jp.nephy.jsonkt.*
import jp.nephy.jsonkt.delegation.*
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.html.*
import kotlinx.html.stream.*

@UseExperimental(KtorExperimentalAPI::class)
suspend inline fun ApplicationCall.respondMinifiedHtml(status: HttpStatusCode = HttpStatusCode.OK, noinline block: HTML.() -> Unit) {
    respond(status, object: OutgoingContent.WriteChannelContent() {
        override val contentType = ContentType.Text.Html.withCharset(Charsets.UTF_8)

        override suspend fun writeTo(channel: ByteWriteChannel) {
            channel.bufferedWriter().use {
                it.append("<!DOCTYPE html>")
                it.appendHTML(prettyPrint = false).html(block = block)
            }
        }
    })
}

suspend inline fun <T: Template<HTML>> ApplicationCall.respondMinifiedHtmlTemplate(template: T, status: HttpStatusCode = HttpStatusCode.OK, body: T.() -> Unit) {
    template.body()
    respondMinifiedHtml(status) { with(template) { apply() } }
}

suspend inline fun ApplicationCall.respondJsonModel(model: JsonModel, status: HttpStatusCode? = null) {
    respondJsonObject(status) { model.json }
}

suspend inline fun ApplicationCall.respondJsonObject(status: HttpStatusCode? = null, block: () -> Map<String, Any?>) {
    respondText(block().toJsonString(), ContentType.Application.Json, status)
}

suspend inline fun ApplicationCall.respondJsonArray(status: HttpStatusCode? = null, block: () -> Iterable<Map<String, Any?>>) {
    respondText(block().toJsonArray().toJsonString(), ContentType.Application.Json, status)
}
