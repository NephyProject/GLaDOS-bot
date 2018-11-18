package jp.nephy.glados.core.config

import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryWatcher
import jp.nephy.glados.config
import jp.nephy.glados.isDebugMode
import mu.KotlinLogging
import java.io.Closeable
import java.nio.file.Paths

object ConfigFileWatcher: Closeable {
    private val watcher = DirectoryWatcher.builder().path(Paths.get(".")).listener { event ->
        when (event.eventType()) {
            DirectoryChangeEvent.EventType.CREATE -> {
            }
            DirectoryChangeEvent.EventType.MODIFY -> {
                when (event.path()) {
                    GLaDOSConfig.productionConfigPath -> {
                        if (!isDebugMode) {
                            config = GLaDOSConfig.load(isDebugMode)
                        }
                    }
                    GLaDOSConfig.developmentConfigPath -> {
                        if (isDebugMode) {
                            config = GLaDOSConfig.load(isDebugMode)
                        }
                    }
                    SecretConfig.secretConfigPath -> {

                    }
                }
            }
            DirectoryChangeEvent.EventType.DELETE -> {
            }
            else -> return@listener
        }
    }.logger(KotlinLogging.logger("GLaDOS.ConfigFileWatcher")).build()

    fun block() {
        watcher.watch()
    }

    override fun close() {
        watcher.close()
    }
}
