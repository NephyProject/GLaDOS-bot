package jp.nephy.glados.core.config

import ch.qos.logback.classic.Level
import io.ktor.client.engine.apache.Apache
import jp.nephy.glados.core.extensions.EmptyJsonObject
import jp.nephy.glados.core.logger.SlackLogger
import jp.nephy.glados.jda
import jp.nephy.jsonkt.*
import jp.nephy.jsonkt.delegation.*
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.emulation.EmulationMode
import kotlinx.serialization.json.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import mu.KotlinLogging
import net.dv8tion.jda.core.entities.*
import java.nio.file.Path
import java.nio.file.Paths

val Guild?.config: GLaDOSConfig.GuildConfig?
    get() = jp.nephy.glados.config.forGuild(this)

data class GLaDOSConfig(override val json: JsonObject): JsonModel {
    companion object {
        private val logger by lazy { SlackLogger("GLaDOS.Config.GLaDOS") }
        internal val productionConfigPath = Paths.get("config.prod.json")!!
        internal val developmentConfigPath = Paths.get("config.dev.json")!!

        private var first = true
        fun load(debug: Boolean): GLaDOSConfig {
            return if (debug) {
                if (first) {
                    KotlinLogging.logger("GLaDOS.Config.GLaDOS")
                } else {
                    logger.info { "デバッグモードの設定をロードします。" }
                }
                load(developmentConfigPath)
            } else {
                if (first) {
                    KotlinLogging.logger("GLaDOS.Config.GLaDOS")
                } else {
                    logger.info { "プロダクションモードの設定をロードします。" }
                }
                load(productionConfigPath)
            }.also {
                first = false
            }
        }

        private fun load(path: Path): GLaDOSConfig {
            return path.parse()
        }
    }

    val token by string
    val clientId by string("client_id")
    val clientSecret by string("client_secret")
    val redirectUri by string("redirect_uri")
    val ownerId by nullableLong("owner_id")
    val logLevel by lambda("log_level", { Level.INFO!! }) { Level.toLevel(it.stringOrNull, Level.INFO)!! }
    val logLevelForSlack by lambda("log_level_for_slack", { Level.INFO!! }) { Level.toLevel(it.stringOrNull, Level.INFO)!! }
    val prefix by string { "!" }
    val pluginsPackagePrefixes by stringList("plugins_package_prefixes")
    val parallelism by int { minOf(Runtime.getRuntime().availableProcessors() / 2, 1) }
    val mongodbHost by string("mongodb_host") { "127.0.0.1" }
    val slackWebhookUrl by string("slack_webhook_url")

    val web by model<Web>()

    data class Web(override val json: JsonObject): JsonModel {
        val host by string { "127.0.0.1" }
        val port by int { 8080 }
        val staticResourcePatterns by lambdaList("static_resource_patterns") { it.string.toRegex() }
        val ignoreIpAddressRanges by lambdaList("ignore_ip_address_ranges") { it.string.toRegex() }
        val ignoreUserAgents by lambdaList("ignore_user_agents") { it.string.toRegex() }
    }

    val guilds by lambda { it.jsonObject.map { guild -> guild.key to guild.value.jsonObject.parse<GuildConfig>() }.toMap() }

    fun forGuild(guild: Guild?): GuildConfig? {
        if (guild == null) {
            return null
        }

        return guilds.values.find { it.id == guild.idLong }
    }

    data class GuildConfig(override val json: JsonObject): JsonModel {
        val id by long
        val isMain by boolean("is_main") { false }

        private val textChannels by jsonObject("text_channels") { EmptyJsonObject }
        private val voiceChannels by jsonObject("voice_channels") { EmptyJsonObject }
        private val roles by jsonObject { EmptyJsonObject }
        private val emotes by jsonObject { EmptyJsonObject }
        private val options by jsonObject { EmptyJsonObject }

        fun textChannel(key: String): TextChannel? {
            return jda.getTextChannelById(textChannels.getOrNull(key)?.longOrNull ?: return null)
        }

        fun voiceChannel(key: String): VoiceChannel? {
            return jda.getVoiceChannelById(voiceChannels.getOrNull(key)?.longOrNull ?: return null)
        }

        fun role(key: String): Role? {
            return jda.getRoleById(roles.getOrNull(key)?.longOrNull ?: return null)
        }

        fun emote(key: String): Emote? {
            return jda.getEmoteById(emotes.getOrNull(key)?.longOrNull ?: return null)
        }

        fun <T> option(key: String, operation: (JsonElement) -> T): T? {
            return options.getOrNull(key)?.let(operation)
        }

        fun stringOption(key: String, default: String): String {
            return option(key) { it.stringOrNull } ?: default
        }

        fun stringOption(key: String): String? {
            return option(key) { it.stringOrNull }
        }

        fun intOption(key: String, default: Int): Int {
            return option(key) { it.intOrNull } ?: default
        }

        fun boolOption(key: String, default: Boolean): Boolean {
            return boolOption(key) ?: default
        }

        fun boolOption(key: String): Boolean? {
            return option(key) { it.booleanOrNull }
        }

        inline fun <reified T: JsonModel> modelListOption(key: String): List<T>? {
            return option(key) { it.jsonArray.parseList<T>() }
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

        inline fun <T> withEmote(key: String, operation: (Emote) -> T?): T? {
            return operation(emote(key) ?: return null)
        }
    }

    val accounts by model<Accounts>()

    data class Accounts(override val json: JsonObject): JsonModel {
        val twitter by lambda { it.jsonObject.map { guild -> guild.key to guild.value.jsonObject.parse<TwitterAccount>() }.toMap() }

        data class TwitterAccount(override val json: JsonObject): JsonModel {
            private val ck by string
            private val cs by string
            private val at by string
            private val ats by string

            val client: PenicillinClient
                get() = client()
            val officialClient: PenicillinClient
                get() = client(EmulationMode.TwitterForiPhone)
            val user by lazy {
                client.use {
                    it.account.verifyCredentials().complete().use {
                        it.result
                    }
                }
            }

            fun client(mode: EmulationMode = EmulationMode.None): PenicillinClient {
                return PenicillinClient {
                    account {
                        application(ck, cs)
                        token(at, ats)
                    }
                    httpClient(Apache)
                    emulationMode = mode
                }
            }
        }
    }

    fun twitterAccount(name: String): Accounts.TwitterAccount {
        return accounts.twitter[name] ?: throw IllegalArgumentException("$name is not found in config.json.")
    }
}
