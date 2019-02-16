package jp.nephy.glados.core

import ch.qos.logback.classic.Level
import jp.nephy.glados.api.config.ConfigJson
import jp.nephy.jsonkt.*
import jp.nephy.jsonkt.delegation.*
import mu.KotlinLogging
import java.nio.file.Path
import java.nio.file.Paths

internal data class GLaDOSConfig(override val json: JsonObject): ConfigJson {
    companion object {
        private val logger = KotlinLogging.logger("GLaDOS.Config")
        private val productionConfigPath = Paths.get("config.prod.json")!!
        private val developmentConfigPath = Paths.get("config.dev.json")!!
        
        fun load(debug: Boolean): GLaDOSConfig {
            return if (debug) {
                logger.info { "Development モードの設定をロードします。" }
                developmentConfigPath.parse()
            } else {
                logger.info { "Production モードの設定をロードします。" }
                productionConfigPath.parse()
            }
        }
    }

    override val parallelism by int { minOf(Runtime.getRuntime().availableProcessors() / 2, 1) }
    override val logLevel: Level by lambda("log_level", { Level.INFO }) { Level.toLevel(it.stringOrNull, Level.INFO) }

    override val tmpDir: Path by lambda("tmp_dir", { Paths.get("tmp") }) { Paths.get(it.string) }
    override val resourceDir: Path by lambda("resource_dir", { Paths.get("resources") }) { Paths.get(it.string) }
}
