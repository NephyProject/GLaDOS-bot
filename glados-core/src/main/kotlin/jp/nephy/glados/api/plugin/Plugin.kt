package jp.nephy.glados.api.plugin

import jp.nephy.glados.GLaDOSApplication
import jp.nephy.glados.api.event.EventModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.full.findAnnotation

abstract class GLaDOSPlugin(name: String? = null, val version: String? = null, val description: String): EventModel, CoroutineScope {
    val name: String = name?.ifBlank { null } ?: this::class.simpleName.orEmpty()

    // TODO
    val logger = KotlinLogging.logger("Plugin.$fullName")
    
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = GLaDOSApplication.instance.coroutineContext + job
}

val GLaDOSPlugin.fullName: String
    get() = "$name[v${version ?: "1.0.0.0"}]"

val GLaDOSPlugin.isExperimental: Boolean
    get() = this::class.findAnnotation<Experimental>() != null
