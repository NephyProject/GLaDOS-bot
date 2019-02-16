package jp.nephy.glados.api.plugin.web

import jp.nephy.glados.api.event.EventModel
import jp.nephy.glados.api.event.web.WebAccessEvent
import jp.nephy.glados.api.event.web.WebErrorEvent

interface WebEventModel: EventModel {
    suspend fun onAccess(event: WebAccessEvent) {}
    
    suspend fun onError(event: WebErrorEvent) {}
}
