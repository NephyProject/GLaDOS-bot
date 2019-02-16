package jp.nephy.glados

import io.ktor.client.HttpClient
import io.ktor.client.features.UserAgent
import jp.nephy.glados.api.GLaDOS
import jp.nephy.glados.core.FeatureManager
import jp.nephy.glados.core.GLaDOSConfig
import jp.nephy.glados.core.PluginManager
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext

@UseExperimental(ObsoleteCoroutinesApi::class)
object GLaDOSApplication {
    internal lateinit var instance: GLaDOS
        
    @JvmStatic
    suspend fun main(args: Array<String>) {
        instance = object: GLaDOS {
            override val isDebugMode = "--debug" in args
            override val config = GLaDOSConfig.load(isDebugMode)

            override val httpClient = HttpClient {
                install(UserAgent) {
                    agent = "GLaDOS-bot (+https://github.com/NephyProject/GLaDOS-bot)"
                }

                // TODO: installDefaultLogger()
            }

            override val coroutineContext = newFixedThreadPoolContext(config.parallelism, "GLaDOS")

            override val pluginManager = PluginManager(this)
            override val featureManager = FeatureManager(this)
        }
        
        launch()
    }
    
    private suspend fun launch() {
        instance.pluginManager.loadAll()
    }
}
