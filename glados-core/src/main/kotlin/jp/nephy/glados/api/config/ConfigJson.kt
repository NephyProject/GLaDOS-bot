package jp.nephy.glados.api.config

import ch.qos.logback.classic.Level
import jp.nephy.jsonkt.delegation.*
import java.nio.file.Path

/**
 * Represents glados.xxx.json model.
 */
interface ConfigJson: JsonModel {
    /**
     * GLaDOS coroutine dispatcher parallelism.
     */
    val parallelism: Int

    /**
     * GLaDOS global logger's level. 
     */
    val logLevel: Level

    /**
     * tmp directory.
     */
    val tmpDir: Path

    /**
     * resource directory.
     */
    val resourceDir: Path
}
