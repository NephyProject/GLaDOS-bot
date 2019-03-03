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

package jp.nephy.glados.clients.web.extensions

import jp.nephy.glados.api.GLaDOS
import jp.nephy.glados.api.resourceFile
import kotlinx.html.*
import kotlinx.io.charsets.Charset

private val minifyRegex = "(?:\\s{2,}|\\n)".toRegex()
private fun staticFile(path: String, minified: Boolean, charset: Charset): String {
    val paths = path.removePrefix("/").removeSuffix("/").split("/").toTypedArray()

    return runCatching {
        GLaDOS.resourceFile("static", *paths).readText(charset)
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

fun FlowContent.inlineHTML(path: String, minified: Boolean = true, charset: Charset = Charsets.UTF_8) {
    consumer.onTagContentUnsafe {
        +staticFile(path, minified, charset)
    }
}
