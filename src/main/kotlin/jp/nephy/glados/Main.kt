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
import jp.nephy.glados.core.plugins.PluginManager
import jp.nephy.glados.core.plugins.SubscriptionClient
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
import net.dv8tion.jda.core.hooks.AnnotatedEventManager
import java.nio.file.Paths

var isDebugMode = false
    private set
lateinit var secret: SecretConfig
    private set
lateinit var config: GLaDOSConfig
    private set
lateinit var dispatcher: ExecutorCoroutineDispatcher
    private set
lateinit var httpClient: HttpClient
    private set
lateinit var slack: SlackWebhook
    private set
lateinit var mongodb: MongoDatabase
    private set
lateinit var eventWaiter: EventWaiter
    private set
lateinit var jda: JDA
    private set

const val userAgent = "GLaDOS-bot (+https://github.com/SlashNephy/GLaDOS-bot)"

@UseExperimental(ObsoleteCoroutinesApi::class)
suspend fun main(args: Array<String>) {
    isDebugMode = args.contains("--debug")
    linkedCacheDir = Paths.get("cache")

    secret = SecretConfig.load(secretConfigPath)
    config = if (isDebugMode) {
        GLaDOSConfig.load(developmentConfigPath)
    } else {
        GLaDOSConfig.load(productionConfigPath)
    }

    dispatcher = newFixedThreadPoolContext(config.parallelism, "GLaDOS-Worker")
    httpClient = HttpClient(Apache)
    slack = SlackWebhook(secret.forKey("slack_webhook_url"))

    val logger = Logger("GLaDOS")
    if (isDebugMode) {
        logger.info { "デバックモードで起動しています。" }
    } else {
        logger.info { "プロダクションモードで起動しています。" }
    }
    if (config.guilds.isEmpty()) {
        logger.error { "GLaDOSのサーバ設定が空です。" }
        return
    }
    if (config.guilds.count { it.value.isMain } != 1) {
        logger.error { "GLaDOSのメインサーバがないか複数定義されています. 1つのサーバのみがメインサーバに指定できます。" }
        return
    }

    mongodb = mongodb(secret.forKey("mongodb_host")).getDatabase("bot")

    eventWaiter = EventWaiter()
    jda = JDABuilder(AccountType.BOT).apply {
        setToken(config.token)
        setGame(Game.playing("Booting..."))
        setEnableShutdownHook(false)
        setAudioSendFactory(NativeAudioSendFactory(1500))

        addEventListener(eventWaiter)

        PluginManager.loadAll()
        setEventManager(AnnotatedEventManager())
        addEventListener(SubscriptionClient.Command)
        addEventListener(SubscriptionClient.ListenerEvent)
        addEventListener(SubscriptionClient.Loop)
        addEventListener(SubscriptionClient.Schedule)
    }.build()

    embeddedServer(Netty, host = config.wuiHost, port = config.wuiPort, module = Application::module).start(wait = true)
}
