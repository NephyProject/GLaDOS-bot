package jp.nephy.glados.core.extensions.web

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

fun main() {
    var html = ""
    while (true) {
        html += readLine().orEmpty()
        if (html.endsWith("..")) {
            html = html.removeSuffix("..")
            break
        }
    }

    Jsoup.parse(html).body().childNodes().forEach {
        println(dump(it))
    }
}

private fun dump(node: Node): String {
    return buildString {
        if (node is Element) {
            append(node.tagName())
            if (node.attributes().size() > 0) {
                append("(${node.attributes().joinToString(", ") {
                    "${if (it.key == "class") {
                        "classes"
                    } else {
                        it.key
                    }} = \"${it.value}\""
                }})")
            } else if (node.childNodeSize() == 0) {
                append("()")
            }

            if (node.childNodeSize() > 0) {
                appendln(" {")
                node.childNodes().filter { it is Element || (it is TextNode && !it.isBlank) }.forEach {
                    appendln("    ${dump(it)}")
                }
                append("}")
            }
        } else if (node is TextNode) {
            append("+ \"${node.text()}\"")
        }
    }
}
