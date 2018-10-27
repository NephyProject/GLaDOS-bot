package jp.nephy.glados

import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import com.mongodb.client.MongoDatabase
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import io.ktor.application.Application
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import jp.nephy.glados.core.*
import jp.nephy.glados.core.feature.FeatureManager
import jp.nephy.glados.core.wui.module
import jp.nephy.jsonkt.mongodb
import jp.nephy.utils.linkedCacheDir
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Game
import java.nio.file.Paths

var isDebugMode = false
    private set
lateinit var secret: SecretConfig
    private set
lateinit var config: GLaDOSConfig
    private set
lateinit var dispatcher: ExecutorCoroutineDispatcher
    private set
lateinit var mongodb: MongoDatabase
    private set
lateinit var httpClient: HttpClient
    private set
lateinit var eventWaiter: EventWaiter
    private set
lateinit var featureManager: FeatureManager
    private set
lateinit var jda: JDA
    private set

const val userAgent = "GLaDOS-bot (+https://github.com/SlashNephy/GLaDOS-bot)"

@ObsoleteCoroutinesApi
fun main(args: Array<String>) {
    isDebugMode = args.contains("--debug")
    linkedCacheDir = Paths.get("cache")

    secret = SecretConfig.load(secretConfigPath)

    config = if (isDebugMode) {
        GLaDOSConfig.load(developmentConfigPath)
    } else {
        GLaDOSConfig.load(productionConfigPath)
    }

    dispatcher = newFixedThreadPoolContext(config.parallelism, "GLaDOS-Worker")

    val logger = Logger("GLaDOS")
    if (config.guilds.isEmpty()) {
        logger.error { "GLaDOSのサーバ設定が空です。" }
        return
    }
    if (config.guilds.count { it.value.isMain } != 1) {
        logger.error { "GLaDOSのメインサーバがないか複数定義されています. 1つのサーバのみがメインサーバに指定できます。" }
        return
    }

    mongodb = mongodb(secret.forKey("mongodb_host")).getDatabase("bot")

    httpClient = HttpClient(Apache)

    eventWaiter = EventWaiter()

    jda = JDABuilder(AccountType.BOT).apply {
        setToken(config.token)
        setAudioSendFactory(NativeAudioSendFactory(1500))
        setGame(Game.playing("Booting..."))

        addEventListener(eventWaiter)

        featureManager = FeatureManager("jp.nephy.glados.features")
        featureManager.loadAll()
        addEventListener(featureManager.commandClient)
        addEventListener(featureManager.listenerEventClient)
        addEventListener(featureManager.loopClient)
    }.build()

    if (isDebugMode) {
        logger.info { "デバックモードで起動完了。" }
    } else {
        logger.info { "プロダクションモードで起動完了。" }
    }

    embeddedServer(Netty, host = config.wuiHost, port = config.wuiPort, module = Application::module).start(wait = true)
}
