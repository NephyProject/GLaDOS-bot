package jp.nephy.glados.core.extensions.web

import kotlinx.html.*
import kotlin.reflect.full.createInstance

interface Addon {
    fun HEAD.headerStart() {}
    fun HEAD.headerEnd() {}
    fun BODY.bodyStart() {}
    fun BODY.bodyEnd() {}

    class Builder {
        operator fun invoke(operation: Builder.() -> Unit) {
            apply(operation)
        }

        val addons = mutableListOf<Addon>()

        fun build(): List<Addon> {
            return addons
        }
    }
}

fun <T: Addon> Addon.Builder.install(addon: T) {
    addons.add(addon)
}

inline fun <reified T: Addon> Addon.Builder.install() {
    addons.add(T::class.objectInstance ?: T::class.createInstance())
}
