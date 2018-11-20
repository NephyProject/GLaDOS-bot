package jp.nephy.glados.core.config

import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryWatcher
import jp.nephy.glados.config
import jp.nephy.glados.core.logger.SlackLogger
import jp.nephy.glados.isDebugMode
import jp.nephy.glados.secret
import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Paths

object ConfigFileWatcher: Closeable {
    private val logger = SlackLogger("GLaDOS.ConfigFileWatcher")

    private val watcher = DirectoryWatcher.builder().path(Paths.get(".")).listener { event ->
        when (event.eventType()) {
            DirectoryChangeEvent.EventType.CREATE -> {
                handleConfigFile(event)

                logger.trace { "ファイル作成: ${event.path()}" }
            }
            DirectoryChangeEvent.EventType.MODIFY -> {
                handleConfigFile(event)

                logger.trace { "ファイル編集: ${event.path()}" }
            }
            DirectoryChangeEvent.EventType.DELETE -> {
                logger.trace { "ファイル削除: ${event.path()}" }
            }
            else -> return@listener
        }
    }.build()

    private fun handleConfigFile(event: DirectoryChangeEvent) {
        val path = event.path()
        when {
            !isDebugMode && Files.isSameFile(path, GLaDOSConfig.productionConfigPath) -> {
                config = GLaDOSConfig.load(isDebugMode)
            }
            isDebugMode && Files.isSameFile(path, GLaDOSConfig.developmentConfigPath) -> {
                config = GLaDOSConfig.load(isDebugMode)
            }
            Files.isSameFile(path, SecretConfig.secretConfigPath) -> {
                secret = SecretConfig.load()
            }
        }
    }

    fun block() {
        watcher.watch()
    }

    override fun close() {
        watcher.close()
    }
}
