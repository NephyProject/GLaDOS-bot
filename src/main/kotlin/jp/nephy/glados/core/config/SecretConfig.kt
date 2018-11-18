package jp.nephy.glados.core.config

import jp.nephy.jsonkt.*
import jp.nephy.jsonkt.delegation.*
import kotlinx.serialization.json.*
import kotlinx.serialization.json.JsonObject
import java.nio.file.Path
import java.nio.file.Paths

class SecretConfig private constructor(override val json: JsonObject): JsonModel {
    companion object {
        internal val secretConfigPath = Paths.get("config.secret.json")!!

        fun load(): SecretConfig {
            return load(secretConfigPath)
        }

        private fun load(path: Path): SecretConfig {
            return SecretConfig(path.toJsonObject())
        }
    }

    inline fun <reified T> forKey(key: String): T {
        return forKeySafe<T>(key, null)!!
    }

    inline fun <reified T> forKey(key: String, default: T): T {
        return forKeySafe(key, default)!!
    }

    inline fun <reified T> forKey(key: String, noinline default: () -> T): T {
        return forKeySafe(key, default)!!
    }

    fun string(key: String): String? {
        return forKey(key)
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    inline fun <reified T> forKeySafe(key: String, default: T? = null): T? {
        val value = json.getOrNull(key) ?: return null

        return when (T::class) {
            Boolean::class -> {
                value.booleanOrNull
            }
            Int::class -> {
                value.intOrNull
            }
            Long::class -> {
                value.longOrNull
            }
            Float::class -> {
                value.floatOrNull
            }
            Double::class -> {
                value.doubleOrNull
            }
            String::class -> {
                value.stringOrNull
            }
            else -> null
        } as T? ?: default
    }

    inline fun <reified T> forKeySafe(key: String, noinline default: () -> T? = { null }): T? {
        return forKeySafe(key, default.safeInvoke())
    }

    fun <T> (() -> T?).safeInvoke() = try {
        invoke()
    } catch (e: Throwable) {
        null
    }
}
