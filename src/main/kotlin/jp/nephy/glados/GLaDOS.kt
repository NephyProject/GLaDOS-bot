package jp.nephy.glados

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import io.ktor.client.HttpClient
import io.ktor.client.features.UserAgent
import jp.nephy.glados.core.config.ConfigFileWatcher
import jp.nephy.glados.core.config.GLaDOSConfig
import jp.nephy.glados.core.config.SecretConfig
import jp.nephy.glados.core.logger.installDefaultLogger
import jp.nephy.glados.core.plugins.SubscriptionClient
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Game
import java.nio.file.Paths

object GLaDOS {
    var isDebugMode = false
        private set
    lateinit var secret: SecretConfig
        internal set
    lateinit var config: GLaDOSConfig
        internal set
    lateinit var dispatcher: ExecutorCoroutineDispatcher
        private set
    lateinit var httpClient: HttpClient
        private set
    lateinit var jda: JDA
        private set

    const val userAgent = "GLaDOS-bot (+https://github.com/NephyProject/GLaDOS-bot)"

    val tmpDir = Paths.get("tmp")!!
    val resourceDir = Paths.get("resources")!!

    @JvmStatic
    @UseExperimental(ObsoleteCoroutinesApi::class)
    fun main(args: Array<String>) {
        isDebugMode = "--debug" in args

        config = GLaDOSConfig.load(isDebugMode)
        dispatcher = newFixedThreadPoolContext(config.parallelism, "GLaDOS-Worker")
        httpClient = HttpClient {
            install(UserAgent) {
                agent = userAgent
            }

            installDefaultLogger()
        }

        secret = SecretConfig.load()

        jda = JDABuilder(AccountType.BOT).apply {
            setToken(config.token)
            setGame(Game.playing("Starting..."))
            setEnableShutdownHook(false)
            setAudioSendFactory(NativeAudioSendFactory(5000))

            runBlocking {
                SubscriptionClient.registerClients(this@apply)
            }
        }.build()

        ConfigFileWatcher.block()
    }
}
