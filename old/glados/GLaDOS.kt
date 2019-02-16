package jp.nephy.glados

import io.ktor.client.HttpClient
import io.ktor.client.features.UserAgent
import jp.nephy.glados.GLaDOSImpl.config
import jp.nephy.glados.GLaDOSImpl.dispatcher
import jp.nephy.glados.GLaDOSImpl.isDebugMode
import jp.nephy.glados.core.config.ConfigFileWatcher
import jp.nephy.glados.core.config.GLaDOSConfig
import jp.nephy.glados.core.config.SecretConfig
import jp.nephy.glados.core.logger.installDefaultLogger
import jp.nephy.glados.core.plugins.SubscriptionClient
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.AccountType
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import sun.management.resources.agent
import sun.net.www.http.HttpClient
import sun.net.www.protocol.http.HttpURLConnection.userAgent
import java.nio.file.Paths

object GLaDOS {
    lateinit var secret: SecretConfig
        internal set
    lateinit var jda: JDA
        private set

    @JvmStatic
    fun main(args: Array<String>) {
        secret = SecretConfig.load()

        jda = JDABuilder(AccountType.BOT).apply {
            setToken(config.token)
            // setGame(Game.playing("Starting..."))
            setEnableShutdownHook(false)
            // setAudioSendFactory(NativeAudioSendFactory(5000))

            runBlocking {
                SubscriptionClient.registerClients(this@apply)
            }
        }.build()

        ConfigFileWatcher.block()
    }
}
