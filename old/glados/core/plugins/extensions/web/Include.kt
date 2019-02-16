package jp.nephy.glados.core.plugins.extensions.web

import io.ktor.html.Placeholder

abstract class Include<T>: Placeholder<T>() {
    abstract fun T.block()

    init {
        invoke { block() }
    }
}
