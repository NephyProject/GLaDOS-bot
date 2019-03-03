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

import kotlinx.html.*
import kotlinx.html.attributes.*

@HtmlTagMarker
fun HTML.head(prefix: String? = null, block: HEAD.() -> Unit = {}) {
    HEAD(attributesMapOf("prefix", prefix), consumer).visit(block)
}

@HtmlTagMarker
fun HTML.body(classes: String? = null, onload: String? = null, block: BODY.() -> Unit = {}) {
    BODY(attributesMapOf("class", classes, "onload", onload), consumer).visit(block)
}

@HtmlTagMarker
fun FlowOrPhrasingOrMetaDataContent.script(async: Boolean = false, defer: Boolean = false, type: String? = null, src: String? = null, integrity: String? = null, crossorigin: String? = null, id: String? = null, block: SCRIPT.() -> Unit = {}) {
    val attributes = attributesMapOf("type", type, "src", src, "integrity", integrity, "crossorigin", crossorigin, "id", id).run {
        if (defer) {
            attributesMapOf("defer", "true") + this
        } else {
            this
        }.run {
            if (async) {
                attributesMapOf("async", "true") + this
            } else {
                this
            }
        }
    }
    SCRIPT(attributes, consumer).visit(block)
}

@HtmlTagMarker
fun FlowOrPhrasingOrMetaDataContent.link(href: String? = null, asType: String? = null, rel: String? = null, type: String? = null, integrity: String? = null, crossorigin: String? = null, onload: String? = null, block: LINK.() -> Unit = {}) {
    LINK(attributesMapOf("href", href, "as", asType, "rel", rel, "type", type, "integrity", integrity, "crossorigin", crossorigin, "onload", onload), consumer).visit(block)
}

@HtmlTagMarker
fun FlowOrPhrasingOrMetaDataContent.meta(name: String? = null, httpEquiv: String? = null, property: String? = null, content: String? = null, charset: String? = null, block: META.() -> Unit = {}) {
    META(attributesMapOf("name", name, "http-equiv", httpEquiv, "property", property, "content", content, "charset", charset), consumer).visit(block)
}

@HtmlTagMarker
fun FlowContent.div(classes: String? = null, id: String? = null, onclick: String? = null, role: String? = null, style: String? = null, align: String? = null, block: DIV.() -> Unit = {}) {
    DIV(attributesMapOf("class", classes, "id", id, "role", role, "style", style, "onclick", onclick, "align", align), consumer).visit(block)
}

@HtmlTagMarker
fun FlowOrInteractiveOrPhrasingContent.iframe(sandbox: IframeSandbox? = null, classes: String? = null, src: String? = null, height: String? = null, width: String? = null, style: String? = null, allowtransparency: String? = null, frameborder: String? = null, block: IFRAME.() -> Unit = {}) {
    IFRAME(attributesMapOf("sandbox", sandbox?.enumEncode(), "class", classes, "src", src, "height", height, "width", width, "style", style, "allowtransparency", allowtransparency, "frameborder", frameborder), consumer).visit(block)
}

@HtmlTagMarker
fun SectioningOrFlowContent.nav(classes: String? = null, ariaLabel: String? = null, block: NAV.() -> Unit = {}) {
    NAV(attributesMapOf("class", classes, "aria-label", ariaLabel), consumer).visit(block)
}

@HtmlTagMarker
fun OL.li(classes: String? = null, ariaCurrent: String? = null, block: LI.() -> Unit = {}) {
    LI(attributesMapOf("class", classes, "aria-current", ariaCurrent), consumer).visit(block)
}

@HtmlTagMarker
fun FlowOrInteractiveOrPhrasingContent.button(
    formEncType: ButtonFormEncType? = null, formMethod: ButtonFormMethod? = null, name: String? = null, type: ButtonType? = null, classes: String? = null, dataDismiss: String? = null, ariaLabel: String? = null, onclick: String? = null, dataToggle: String? = null, block: BUTTON.() -> Unit = {}
) {
    BUTTON(attributesMapOf("formenctype", formEncType?.enumEncode(), "formmethod", formMethod?.enumEncode(), "name", name, "type", type?.enumEncode(), "class", classes, "data-dismiss", dataDismiss, "aria-label", ariaLabel, "onclick", onclick, "data-toggle", dataToggle), consumer).visit(block)
}

@HtmlTagMarker
fun FlowOrPhrasingContent.span(classes: String? = null, id: String? = null, style: String? = null, ariaHidden: String? = null, block: SPAN.() -> Unit = {}) {
    SPAN(attributesMapOf("class", classes, "id", id, "style", style, "aria-hidden", ariaHidden), consumer).visit(block)
}

@HtmlTagMarker
fun FlowContent.p(classes: String? = null, style: String? = null, block: P.() -> Unit = {}) {
    P(attributesMapOf("class", classes, "style", style), consumer).visit(block)
}

@HtmlTagMarker
fun FlowContent.pre(classes: String? = null, id: String? = null, style: String? = null, block: PRE.() -> Unit = {}) {
    PRE(attributesMapOf("class", classes, "id", id, "style", style), consumer).visit(block)
}

@HtmlTagMarker
fun FlowOrPhrasingContent.code(classes: String? = null, id: String? = null, block: CODE.() -> Unit = {}) {
    CODE(attributesMapOf("class", classes, "id", id), consumer).visit(block)
}

@HtmlTagMarker
fun FlowContent.form(action: String? = null, target: String? = null, encType: FormEncType? = null, method: FormMethod? = null, classes: String? = null, id: String? = null, style: String? = null, block: FORM.() -> Unit = {}) {
    FORM(attributesMapOf("action", action, "enctype", encType?.enumEncode(), "method", method?.enumEncode(), "class", classes, "id", id, "style", style, "target", target), consumer).visit(block)
}

@HtmlTagMarker
fun FlowOrInteractiveOrPhrasingContent.textArea(rows: String? = null, cols: String? = null, wrap: TextAreaWrap? = null, id: String? = null, name: String? = null, placeholder: String? = null, classes: String? = null, block: TEXTAREA.() -> Unit = {}) {
    TEXTAREA(attributesMapOf("rows", rows, "cols", cols, "wrap", wrap?.enumEncode(), "class", classes, "id", id, "name", name, "placeholder", placeholder), consumer).visit(block)
}

@HtmlTagMarker
fun FlowOrInteractiveOrPhrasingContent.input(
    type: InputType? = null,
    formEncType: InputFormEncType? = null,
    formMethod: InputFormMethod? = null,
    name: String? = null,
    classes: String? = null,
    id: String? = null,
    placeholder: String? = null,
    ariaLabel: String? = null,
    ariaDescribedBy: String? = null,
    value: String? = null,
    onkeypress: String? = null,
    oninput: String? = null,
    block: INPUT.() -> Unit = {}
) {
    INPUT(
        attributesMapOf(
            "type", type?.enumEncode(), "formenctype", formEncType?.enumEncode(), "formmethod", formMethod?.enumEncode(), "name", name, "class", classes, "id", id, "placeholder", placeholder, "aria-label", ariaLabel, "aria-describedby", ariaDescribedBy, "value", value, "onkeypress", onkeypress,
            "oninput", oninput
        ), consumer
    ).visit(block)
}

@HtmlTagMarker
fun SectioningOrFlowContent.section(classes: String? = null, id: String? = null, block: SECTION.() -> Unit = {}) {
    SECTION(attributesMapOf("class", classes, "id", id), consumer).visit(block)
}

@HtmlTagMarker
fun FlowOrPhrasingContent.i(classes: String? = null, title: String? = null, onclick: String? = null, block: I.() -> Unit = {}) {
    I(attributesMapOf("class", classes, "title", title, "onclick", onclick), consumer).visit(block)
}

open class CENTER(initialAttributes: Map<String, String>, override val consumer: TagConsumer<*>): HTMLTag("center", consumer, initialAttributes, null, false, false), HtmlBlockInlineTag

@HtmlTagMarker
fun FlowContent.center(classes: String? = null, block: CENTER.() -> Unit = {}): Unit = CENTER(attributesMapOf("class", classes), consumer).visit(block)

open class U(initialAttributes: Map<String, String>, override val consumer: TagConsumer<*>): HTMLTag("u", consumer, initialAttributes, null, true, false), HtmlBlockInlineTag

@HtmlTagMarker
fun FlowOrPhrasingContent.u(classes: String? = null, block: U.() -> Unit = {}): Unit = U(attributesMapOf("class", classes), consumer).visit(block)

@HtmlTagMarker
fun FlowOrInteractiveOrPhrasingContent.select(classes: String? = null, name: String? = null, block: SELECT.() -> Unit = {}) {
    SELECT(attributesMapOf("class", classes, "name", name), consumer).visit(block)
}

@HtmlTagMarker
fun SELECT.option(classes: String? = null, value: String? = null, block: OPTION.() -> Unit = {}) {
    OPTION(attributesMapOf("class", classes, "value", value), consumer).visit(block)
}

@HtmlTagMarker
fun FlowOrPhrasingContent.canvas(classes: String? = null, id: String? = null, block: CANVAS.() -> Unit = {}) {
    CANVAS(attributesMapOf("class", classes, "id", id), consumer).visit(block)
}

@HtmlTagMarker
fun FlowOrInteractiveOrPhrasingContent.video(classes: String? = null, src: String? = null, alt: String? = null, block: VIDEO.() -> Unit = {}) {
    VIDEO(attributesMapOf("class", classes, "src", src, "alt", alt), consumer).visit(block)
}
