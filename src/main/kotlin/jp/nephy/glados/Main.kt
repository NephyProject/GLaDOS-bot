package jp.nephy.glados

import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import com.mongodb.client.MongoDatabase
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import jp.nephy.glados.core.config.ConfigFileWatcher
import jp.nephy.glados.core.config.GLaDOSConfig
import jp.nephy.glados.core.config.SecretConfig
import jp.nephy.glados.core.extensions.database
import jp.nephy.glados.core.logger.SlackWebhook
import jp.nephy.glados.core.plugins.SubscriptionClient
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.newFixedThreadPoolContext
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Game
import org.litote.kmongo.KMongo

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
lateinit var slack: SlackWebhook
    private set
lateinit var mongodb: MongoDatabase
    private set
lateinit var eventWaiter: EventWaiter
    private set
lateinit var jda: JDA
    private set

const val userAgent = "GLaDOS-bot (+https://github.com/NephyProject/GLaDOS-bot)"

@UseExperimental(ObsoleteCoroutinesApi::class)
suspend fun main(args: Array<String>) {
    isDebugMode = "--debug" in args

    config = GLaDOSConfig.load(isDebugMode)
    dispatcher = newFixedThreadPoolContext(config.parallelism, "GLaDOS-Worker")
    httpClient = HttpClient(Apache)
    slack = SlackWebhook

    secret = SecretConfig.load()

    mongodb = KMongo.createClient(config.mongodbHost).database("bot")

    eventWaiter = EventWaiter()
    jda = JDABuilder(AccountType.BOT).apply {
        setToken(config.token)
        setGame(Game.playing("Starting..."))
        setEnableShutdownHook(false)
        setAudioSendFactory(NativeAudioSendFactory(5000))

        addEventListener(eventWaiter)

        SubscriptionClient.registerClients(this)
    }.build()

    ConfigFileWatcher.block()

    while (true) {
        delay(10000)
    }
}
