package jp.nephy.glados.api.event

import jp.nephy.glados.api.GLaDOS

interface Event {
    /**
     * Shared GLaDOS bot instance.
     */
    val glados: GLaDOS
}
