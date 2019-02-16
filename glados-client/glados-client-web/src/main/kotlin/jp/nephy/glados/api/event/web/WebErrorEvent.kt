package jp.nephy.glados.api.event.web

import jp.nephy.glados.api.GLaDOS
import jp.nephy.glados.api.event.Event

data class WebErrorEvent(
    override val glados: GLaDOS,
    val access: WebAccessEvent
): Event
