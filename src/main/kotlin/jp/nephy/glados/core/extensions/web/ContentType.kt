package jp.nephy.glados.core.extensions.web

import io.ktor.http.ContentType

val ContentType.Application.FontOtf: ContentType
    get() = ContentType("application", "x-font-otf")

val ContentType.Application.FontWoff2: ContentType
    get() = ContentType("font", "woff2")

val ContentType.Application.Navimap: ContentType
    get() = ContentType("application", "x-navimap")
