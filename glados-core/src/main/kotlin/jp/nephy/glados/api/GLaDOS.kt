package jp.nephy.glados.api

import io.ktor.client.HttpClient
import jp.nephy.glados.api.config.ConfigJson
import jp.nephy.glados.core.FeatureManager
import jp.nephy.glados.core.PluginManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

interface GLaDOS: CoroutineScope {
    /**
     * The flag whether GLaDOS works as development mode.
     */
    val isDebugMode: Boolean

    val config: ConfigJson

    /**
     * Global Ktor HttpClient.
     */
    val httpClient: HttpClient

    /**
     * Global coroutine dispatcher.
     */
    override val coroutineContext: CoroutineDispatcher

    /**
     * GLaDOS Plugin manager.
     */
    val pluginManager: PluginManager

    /**
     * GLaDOS Event manager.
     */
    val featureManager: FeatureManager
}
