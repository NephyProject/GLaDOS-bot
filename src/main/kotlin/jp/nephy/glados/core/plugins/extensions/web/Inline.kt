package jp.nephy.glados.core.plugins.extensions.web

import jp.nephy.glados.core.plugins.extensions.resourceFile
import kotlinx.html.*
import kotlinx.io.charsets.Charset

private val minifyRegex = "(\\s{2,}|\\n)".toRegex()

private fun staticFile(path: String, minified: Boolean, charset: Charset): String {
    val paths = path.removePrefix("/").removeSuffix("/").split("/").toTypedArray()

    return runCatching {
        resourceFile("static", *paths).readText(charset)
    }.getOrNull()?.let {
        if (minified) {
            it.replace(minifyRegex, "")
        } else {
            it
        }
    }.orEmpty()
}

fun FlowOrMetaDataContent.inlineCSS(path: String, minified: Boolean = true, charset: Charset = Charsets.UTF_8) {
    style {
        unsafe {
            +staticFile(path, minified, charset)
        }
    }
}

fun FlowOrPhrasingOrMetaDataContent.inlineJS(path: String, async: Boolean = false, defer: Boolean = false, type: String? = null, minified: Boolean = true, charset: Charset = Charsets.UTF_8) {
    script(async = async, defer = defer, type = type) {
        unsafe {
            +staticFile(path, minified, charset)
        }
    }
}
