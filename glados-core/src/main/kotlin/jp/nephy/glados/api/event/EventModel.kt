package jp.nephy.glados.api.event

import jp.nephy.glados.api.event.general.PluginExceptionEvent
import jp.nephy.glados.api.event.general.PluginLoadEvent
import jp.nephy.glados.api.event.general.PluginUnloadEvent
import jp.nephy.glados.api.event.general.ShutdownEvent

interface EventModel {
    /**
     * Called when any events are created.
     */
    suspend fun onEvent(event: Event) {}
    
    /**
     * Called when the plugin is loaded.
     */
    suspend fun onLoad(event: PluginLoadEvent) {}
    /**
     * Called when the plugin is unloaded.
     */
    suspend fun onUnload(event: PluginUnloadEvent) {}
    /**
     * Called when any exceptions are thrown in the plugin
     */
    suspend fun onException(event: PluginExceptionEvent) {}
    /**
     * Called when GLaDOS will shutdown.
     */
    suspend fun onShutdown(event: ShutdownEvent) {}
}
