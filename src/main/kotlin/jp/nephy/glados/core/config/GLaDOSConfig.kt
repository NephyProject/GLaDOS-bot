package jp.nephy.glados.core.config

import ch.qos.logback.classic.Level
import io.ktor.client.engine.apache.Apache
import jp.nephy.glados.core.logger.SlackLogger
import jp.nephy.jsonkt.*
import jp.nephy.jsonkt.delegation.*
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.emulation.EmulationMode
import kotlinx.serialization.json.JsonObject
import mu.KotlinLogging
import java.nio.file.Path
import java.nio.file.Paths

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
    val ownerId by nullableLong("owner_id")
    val logLevel by lambda("log_level", { Level.INFO!! }) { Level.toLevel(it.stringOrNull, Level.INFO)!! }
    val logLevelForSlack by lambda("log_level_for_slack", { Level.INFO!! }) { Level.toLevel(it.stringOrNull, Level.INFO)!! }
    val prefix by string { "!" }
    val pluginsPackagePrefixes by stringList("plugins_package_prefixes")
    val parallelism by int { minOf(Runtime.getRuntime().availableProcessors() / 2, 1) }
    val slackWebhookUrl by string("slack_webhook_url")

    val web by model<Web>()

    data class Web(override val json: JsonObject): JsonModel {
        val host by string { "127.0.0.1" }
        val port by int { 8080 }
        val ignoreIpAddressRanges by lambdaList("ignore_ip_address_ranges") { it.string.toRegex() }
        val ignoreUserAgents by lambdaList("ignore_user_agents") { it.string.toRegex() }
    }

    val guilds by lambda { it.jsonObject.map { guild -> guild.value.jsonObject.parse<GuildConfig>() } }

    data class GuildConfig(override val json: JsonObject): JsonModel {
        val id by long
        val isMain by boolean("is_main") { false }

        val textChannels by jsonObject("text_channels") { jsonObjectOf() }
        val voiceChannels by jsonObject("voice_channels") { jsonObjectOf() }
        val roles by jsonObject { jsonObjectOf() }
        val emotes by jsonObject { jsonObjectOf() }
        val options by jsonObject { jsonObjectOf() }
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
