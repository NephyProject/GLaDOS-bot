package jp.nephy.glados.core

import jp.nephy.glados.api.GLaDOS
import jp.nephy.glados.api.event.Event
import jp.nephy.glados.api.event.general.PluginExceptionEvent
import jp.nephy.glados.api.event.general.PluginLoadEvent
import jp.nephy.glados.api.event.general.PluginUnloadEvent
import jp.nephy.glados.api.event.general.ShutdownEvent
import jp.nephy.glados.api.subscription.SubscriptionClient

class FeatureManager(private val instance: GLaDOS) {
    @PublishedApi
    internal val events = ClassContainer(
        PluginLoadEvent::class, PluginUnloadEvent::class, PluginExceptionEvent::class, ShutdownEvent::class
    )

    inline fun <reified T: Event> registerEvent() {
        events += T::class
    }
    
    @PublishedApi
    internal val clients = ClassContainer<SubscriptionClient<*>>()
    
    @Suppress("UNCHECKED_CAST")
    inline fun <A: Annotation, reified C: SubscriptionClient<A>> registerClient() {
        clients += C::class
    }
    
    fun fire(event: Event) {
        
    }
}
