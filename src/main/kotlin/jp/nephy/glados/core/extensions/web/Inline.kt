package jp.nephy.glados.core.extensions.web

import kotlinx.html.*

private val minifyRegex = "(\\s{2,}|\\n)".toRegex()

fun FlowOrMetaDataContent.inlineCSS(path: String) {
    style {
        unsafe {
            +Cacher.getOrPut(path) {
                Cacher.getAssetAsString(path).orEmpty().replace(minifyRegex, "")
            }
        }
    }
}

fun FlowOrPhrasingOrMetaDataContent.inlineJS(path: String, async: Boolean = false, defer: Boolean = false, type: String? = null) {
    script(async = async, defer = defer, type = type) {
        unsafe {
            +Cacher.getAssetAsString(path).orEmpty().replace(minifyRegex, "")
        }
    }
}
