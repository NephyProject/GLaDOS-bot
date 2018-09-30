package jp.nephy.glados.core

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import jp.nephy.glados.jda
import jp.nephy.jsonkt.*
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.VoiceChannel
import java.nio.file.Path
import java.nio.file.Paths

val productionConfigPath = Paths.get("config.prod.json")!!
val developmentConfigPath = Paths.get("config.dev.json")!!
val secretConfigPath = Paths.get("config.secret.json")!!

fun Boolean?.isFalseOrNull(): Boolean {
    return this != true
}

class GLaDOSConfig private constructor(override val json: JsonObject): JsonModel {
    companion object {
        fun load(path: Path): GLaDOSConfig {
            return GLaDOSConfig(path.toJsonObject())
        }
    }

    val token by json.byString
    val ownerId by json.byNullableLong("owner_id")
    val prefix by json.byString { "!" }
    val parallelism by json.byNullableInt
    val wuiHost by json.byString("wui_host") { "127.0.0.1" }
    val wuiPort by json.byInt("wui_port") { 8080 }
    val guilds by json.byLambda { jsonObject.map { it.key to GuildConfig(it.value.jsonObject) }.toMap() }

    fun forGuild(guild: Guild?): GuildConfig? {
        if (guild == null) {
            return null
        }
        return guilds.values.find { it.id == guild.idLong }
    }

    class GuildConfig(override val json: JsonObject): JsonModel {
        val id by json.byLong
        val isMain by json.byBool("is_main") { false }

        private val textChannels by json.byJsonObject("text_channels") { jsonObject() }
        private val voiceChannels by json.byJsonObject("voice_channels") { jsonObject() }
        private val roles by json.byJsonObject { jsonObject() }
        private val options by json.byJsonObject { jsonObject() }

        fun textChannel(key: String): TextChannel? {
            return jda.getTextChannelById(textChannels.getOrNull(key)?.nullableLong ?: return null)
        }

        fun voiceChannel(key: String): VoiceChannel? {
            return jda.getVoiceChannelById(voiceChannels.getOrNull(key)?.nullableLong ?: return null)
        }

        fun role(key: String): Role? {
            return jda.getRoleById(roles.getOrNull(key)?.nullableLong ?: return null)
        }

        fun <T> option(key: String, operation: (JsonElement) -> T): T? {
            return options.getOrNull(key)?.let(operation)
        }

        fun stringOption(key: String, default: String): String {
            return option(key) { it.nullableString } ?: default
        }

        fun intOption(key: String, default: Int): Int {
            return option(key) { it.nullableInt } ?: default
        }

        fun boolOption(key: String, default: Boolean): Boolean {
            return boolOption(key) ?: default
        }

        fun boolOption(key: String): Boolean? {
            return option(key) { it.nullableBool }
        }
    }
}

class SecretConfig private constructor(override val json: JsonObject): JsonModel {
    companion object {
        fun load(path: Path): SecretConfig {
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

    @Suppress("IMPLICIT_CAST_TO_ANY")
    inline fun <reified T> forKeySafe(key: String, default: T? = null): T? {
        val value = json.getOrNull(key)

        return when (T::class) {
            Boolean::class -> {
                value.toBoolOrNull()
            }
            Byte::class -> {
                value.toByteOrNull()
            }
            Char::class -> {
                value.toCharOrNull()
            }
            Short::class -> {
                value.toShortOrNull()
            }
            Int::class -> {
                value.toIntOrNull()
            }
            Long::class -> {
                value.toLongOrNull()
            }
            Float::class -> {
                value.toFloatOrNull()
            }
            Double::class -> {
                value.toDoubleOrNull()
            }
            String::class -> {
                value.toStringOrNull()
            }
            else -> null
        } as T? ?: default
    }

    inline fun <reified T> forKeySafe(key: String, noinline default: () -> T? = { null }): T? {
        return forKeySafe(key, default.safeInvoke())
    }

    fun <T> (() -> T?).safeInvoke() = try {
        invoke()
    } catch (e: Exception) {
        null
    }
}
