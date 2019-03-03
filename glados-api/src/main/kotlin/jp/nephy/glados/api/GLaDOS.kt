/*
 * The MIT License (MIT)
 *
 *     Copyright (c) 2017-2019 Nephy Project Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

@file:Suppress("UNUSED", "DEPRECATION")

package jp.nephy.glados.api

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

/**
 * GLaDOS main instance.
 */
interface GLaDOS {
    companion object: CoroutineScope {
        override val coroutineContext: CoroutineContext
            get() = Instance.coroutineContext
    }
    
    /**
     * GLaDOS shared object.
     */
    object Instance: GLaDOS {
        @Deprecated("Directly accessing to this property is deprecated.")
        override var initialized: Boolean = false
            internal set

        @Deprecated("Directly accessing to this property is deprecated.")
        override var isDebugMode: Boolean = false

        @Deprecated("Directly accessing to this property is deprecated.")
        override lateinit var config: ConfigJson
        
        @Deprecated("Directly accessing to this property is deprecated.")
        override lateinit var secret: SecretJson

        @Deprecated("Directly accessing to this property is deprecated.")
        override lateinit var httpClient: HttpClient

        @Deprecated("Directly accessing to this property is deprecated.")
        override lateinit var coroutineContext: CoroutineContext

        @Deprecated("Directly accessing to this property is deprecated.")
        override lateinit var clientManager: ClassManager<SubscriptionClient<*, *, *>>

        @Deprecated("Directly accessing to this property is deprecated.")
        override lateinit var pluginManager: ClassManager<Plugin>

        @Deprecated("Directly accessing to this property is deprecated.")
        override lateinit var fileSystemWatcher: FileSystemWatcher
        
        @Deprecated("Directly accessing to this property is deprecated.")
        override lateinit var attributes: AttributeMap
    }

    /**
     * The flag whether GLaDOS main instance initialized.
     */
    val initialized: Boolean
    
    /**
     * The flag whether GLaDOS works as development mode.
     */
    val isDebugMode: Boolean

    /**
     * GLaDOS config.json model.
     */
    val config: ConfigJson

    /**
     * GLaDOS secret.json model.
     */
    val secret: SecretJson
    
    /**
     * Global Ktor HttpClient.
     */
    val httpClient: HttpClient

    /**
     * Global coroutine context.
     */
    val coroutineContext: CoroutineContext

    /**
     * GLaDOS SubscriptionClient manager.
     */
    val clientManager: ClassManager<SubscriptionClient<*, *, *>>
    
    /**
     * GLaDOS Plugin manager.
     */
    val pluginManager: ClassManager<Plugin>
    
    /**
     * GLaDOS FileSystemWatcher.
     */
    val fileSystemWatcher: FileSystemWatcher

    /**
     * GLaDOS AttributeMap.
     */
    val attributes: AttributeMap
}

/**
 * Initializes GLaDOS main instance.
 */
fun GLaDOS.Companion.initialize(block: GLaDOS.Instance.() -> Unit) {
    GLaDOS.Instance.apply(block)
    GLaDOS.Instance.initialized = true
}

/**
 * Shortcut to `GLaDOS.Instance.initialized`.
 */
val GLaDOS.Companion.initialized: Boolean
    get() = GLaDOS.Instance.initialized

/**
 * Shortcut to `GLaDOS.Instance.isDebugMode`.
 */
val GLaDOS.Companion.isDebugMode: Boolean
    get() = GLaDOS.Instance.isDebugMode

/**
 * Shortcut to `GLaDOS.Instance.config`.
 */
val GLaDOS.Companion.config: ConfigJson
    get() = GLaDOS.Instance.config

/**
 * Shortcut to `GLaDOS.Instance.secret`.
 */
val GLaDOS.Companion.secret: SecretJson
    get() = GLaDOS.Instance.secret

/**
 * Shortcut to `GLaDOS.Instance.httpClient`.
 */
val GLaDOS.Companion.httpClient: HttpClient
    get() = GLaDOS.Instance.httpClient

/**
 * Shortcut to `GLaDOS.Instance.clientManager`.
 */
val GLaDOS.Companion.clientManager: ClassManager<SubscriptionClient<*, *, *>>
    get() = GLaDOS.Instance.clientManager

/**
 * Shortcut to `GLaDOS.Instance.pluginManager`.
 */
val GLaDOS.Companion.pluginManager: ClassManager<Plugin>
    get() = GLaDOS.Instance.pluginManager

/**
 * Shortcut to `GLaDOS.Instance.fileSystemWatcher`.
 */
val GLaDOS.Companion.fileSystemWatcher: FileSystemWatcher
    get() = GLaDOS.Instance.fileSystemWatcher

/**
 * Shortcut to `GLaDOS.Instance.attributes`.
 */
val GLaDOS.Companion.attributes: AttributeMap
    get() = GLaDOS.Instance.attributes
