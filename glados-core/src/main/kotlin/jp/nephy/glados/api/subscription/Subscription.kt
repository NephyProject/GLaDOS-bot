package jp.nephy.glados.api.subscription

import jp.nephy.glados.api.plugin.*
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation

interface Subscription<A: Annotation> {
    val plugin: GLaDOSPlugin
    val function: KFunction<*>
    val annotation: A
    val priority: Priority
}

val Subscription<*>.isExperimental: Boolean
    get() = plugin.isExperimental || function.findAnnotation<Experimental>() != null

val Subscription<*>.fullname: String
    get() = "${plugin.fullName}#$name"

val Subscription<*>.name: String
    get() = function.name
