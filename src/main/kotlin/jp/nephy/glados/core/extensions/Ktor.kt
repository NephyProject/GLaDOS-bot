package jp.nephy.glados.core.extensions

import io.ktor.features.origin
import io.ktor.http.HttpHeaders
import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import io.ktor.request.path
import io.ktor.request.queryString
import java.net.IDN

val ApplicationRequest.effectiveHost: String
    get() = IDN.toUnicode(header(HttpHeaders.Host) ?: origin.host)

val ApplicationRequest.url: String
    get() {
        val query = call.request.queryString()
        return "${origin.scheme}://$effectiveHost${path()}${if (query.isNotBlank()) "?$query" else ""}"
    }
