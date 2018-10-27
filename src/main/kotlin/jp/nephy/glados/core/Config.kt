package jp.nephy.glados.core

import jp.nephy.glados.jda
import jp.nephy.jsonkt.*
import jp.nephy.jsonkt.delegation.*
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.VoiceChannel
import java.nio.file.Path
import java.nio.file.Paths

val productionConfigPath = Paths.get("config.prod.json")!!
val developmentConfigPath = Paths.get("config.dev.json")!!
val secretConfigPath = Paths.get("config.secret.json")!!
val sqliteDatabasePath = Paths.get("glados.db")!!

fun Boolean?.isFalseOrNull(): Boolean {
    return this != true
}

data class GLaDOSConfig(override val json: ImmutableJsonObject): JsonModel {
    companion object {
        fun load(path: Path): GLaDOSConfig {
            return path.parse()
        }
    }

    val token by string
    val clientId by string("client_id")
    val clientSecret by string("client_secret")
    val redirectUri by string("redirect_uri")
    val ownerId by nullableLong("owner_id")
    val prefix by string { "!" }
    val parallelism by int { minOf(Runtime.getRuntime().availableProcessors() / 2, 1) }
    val wuiHost by string("wui_host") { "127.0.0.1" }
    val wuiPort by int("wui_port") { 8080 }
    val guilds by lambda { it.immutableJsonObject.map { guild -> guild.key to guild.value.immutableJsonObject.parse<GuildConfig>() }.toMap() }

    fun forGuild(guild: Guild?): GuildConfig? {
        if (guild == null) {
            return null
        }

        return guilds.values.find { it.id == guild.idLong }
    }

    data class GuildConfig(override val json: ImmutableJsonObject): JsonModel {
        val id by long
        val isMain by boolean("is_main") { false }

        private val textChannels by immutableJsonObject("text_channels") { immutableJsonObjectOf() }
        private val voiceChannels by immutableJsonObject("voice_channels") { immutableJsonObjectOf() }
        private val roles by immutableJsonObject { immutableJsonObjectOf() }
        private val options by immutableJsonObject { immutableJsonObjectOf() }

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

        inline fun <T> withTextChannel(key: String, operation: (TextChannel) -> T?): T? {
            return operation(textChannel(key) ?: return null)
        }

        inline fun <T> withVoiceChannel(key: String, operation: (VoiceChannel) -> T?): T? {
            return operation(voiceChannel(key) ?: return null)
        }

        inline fun <T> withRole(key: String, operation: (Role) -> T?): T? {
            return operation(role(key) ?: return null)
        }
    }
}

class SecretConfig private constructor(override val json: ImmutableJsonObject): JsonModel {
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
        val value = json.getOrNull(key) ?: return null

        return when (T::class) {
            Boolean::class -> {
                value.toBooleanOrNull()
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
                value.toStringValueOrNull()
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
