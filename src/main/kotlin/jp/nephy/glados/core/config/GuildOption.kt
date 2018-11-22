@file:Suppress("UNUSED")

package jp.nephy.glados.core.config

import jp.nephy.jsonkt.*
import jp.nephy.jsonkt.delegation.*
import kotlinx.serialization.json.*
import kotlinx.serialization.json.JsonElement

inline fun <T> GLaDOSConfig.GuildConfig?.option(key: String, operation: (JsonElement) -> T): T? {
    return this?.options?.getOrNull(key)?.let(operation)
}

fun GLaDOSConfig.GuildConfig?.booleanOption(key: String): Boolean? {
    return option(key) { it.booleanOrNull }
}

fun GLaDOSConfig.GuildConfig?.booleanOption(key: String, default: Boolean): Boolean {
    return booleanOption(key) ?: default
}

fun GLaDOSConfig.GuildConfig?.intOption(key: String): Int? {
    return option(key) { it.intOrNull }
}

fun GLaDOSConfig.GuildConfig?.intOption(key: String, default: Int): Int {
    return intOption(key) ?: default
}

fun GLaDOSConfig.GuildConfig?.longOption(key: String): Long? {
    return option(key) { it.longOrNull }
}

fun GLaDOSConfig.GuildConfig?.longOption(key: String, default: Long): Long {
    return longOption(key) ?: default
}

fun GLaDOSConfig.GuildConfig?.floatOption(key: String): Float? {
    return option(key) { it.floatOrNull }
}

fun GLaDOSConfig.GuildConfig?.floatOption(key: String, default: Float): Float {
    return floatOption(key) ?: default
}

fun GLaDOSConfig.GuildConfig?.doubleOption(key: String): Double? {
    return option(key) { it.doubleOrNull }
}

fun GLaDOSConfig.GuildConfig?.doubleOption(key: String, default: Double): Double {
    return doubleOption(key) ?: default
}

fun GLaDOSConfig.GuildConfig?.stringOption(key: String): String? {
    return option(key) { it.stringOrNull }
}

fun GLaDOSConfig.GuildConfig?.stringOption(key: String, default: String): String {
    return stringOption(key) ?: default
}

inline fun <reified T: JsonModel> GLaDOSConfig.GuildConfig?.modelOption(key: String): T? {
    return option(key) { it.jsonObject.parse<T>() }
}

inline fun <reified T: JsonModel> GLaDOSConfig.GuildConfig?.modelListOption(key: String): List<T>? {
    return option(key) { it.jsonArray.parseList<T>() }
}
