package jp.nephy.glados.api.event.general

import jp.nephy.glados.api.GLaDOS
import jp.nephy.glados.api.event.Event

data class PluginLoadEvent(override val glados: GLaDOS): Event
