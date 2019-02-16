package jp.nephy.glados.core.config

import jp.nephy.glados.core.logger.SlackLogger
import jp.nephy.jsonkt.*
import jp.nephy.jsonkt.delegation.*
import kotlinx.serialization.json.*
import kotlinx.serialization.json.JsonObject
import java.nio.file.Path
import java.nio.file.Paths

class SecretConfig private constructor(override val json: JsonObject): JsonModel {
    companion object {
        private val logger = SlackLogger("GLaDOS.Config.Secret")
        internal val secretConfigPath = Paths.get("config.secret.json")!!

        fun load(): SecretConfig {
            logger.info { "シークレット設定をロードします。" }
            return load(secretConfigPath)
        }

        private fun load(path: Path): SecretConfig {
            return SecretConfig(path.toJsonObject())
        }
    }

    fun boolean(key: String): Boolean? {
        return json.getOrNull(key)?.booleanOrNull
    }

    fun boolean(key: String, default: Boolean): Boolean {
        return boolean(key) ?: default
    }

    fun int(key: String): Int? {
        return json.getOrNull(key)?.intOrNull
    }

    fun int(key: String, default: Int): Int {
        return int(key) ?: default
    }

    fun long(key: String): Long? {
        return json.getOrNull(key)?.longOrNull
    }

    fun long(key: String, default: Long): Long {
        return long(key) ?: default
    }

    fun float(key: String): Float? {
        return json.getOrNull(key)?.floatOrNull
    }

    fun float(key: String, default: Float): Float {
        return float(key) ?: default
    }

    fun double(key: String): Double? {
        return json.getOrNull(key)?.doubleOrNull
    }

    fun double(key: String, default: Double): Double {
        return double(key) ?: default
    }

    fun string(key: String): String? {
        return json.getOrNull(key)?.stringOrNull
    }

    fun string(key: String, default: String): String {
        return string(key) ?: default
    }

    inline fun <reified T: JsonModel> model(key: String): T? {
        return json.getObjectOrNull(key)?.parse()
    }

    inline fun <reified T: JsonModel> modelList(key: String): List<T>? {
        return json.getArrayOrNull(key)?.parseList()
    }
}
