package jp.nephy.glados

import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import io.ktor.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import jp.nephy.glados.core.*
import jp.nephy.glados.core.feature.FeatureManager
import jp.nephy.glados.core.wui.module
import jp.nephy.utils.linkedCacheDir
import kotlinx.coroutines.experimental.CommonPool
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
lateinit var eventWaiter: EventWaiter
    private set
lateinit var featureManager: FeatureManager
    private set
lateinit var jda: JDA
    private set

fun main(args: Array<String>) {
    isDebugMode = args.contains("--debug")
    linkedCacheDir = Paths.get("cache")

    secret = SecretConfig.load(secretConfigPath)

    val logger = Logger("GLaDOS")
    config = if (isDebugMode) {
        logger.debug { "デバックモードで起動しています." }
        GLaDOSConfig.load(developmentConfigPath)
    } else {
        logger.debug { "プロダクションモードで起動しています." }
        GLaDOSConfig.load(productionConfigPath)
    }

    if (config.guilds.isEmpty()) {
        logger.error { "GLaDOSのサーバ設定が空です." }
        return
    }
    if (config.guilds.count { it.value.isMain } != 1) {
        logger.error { "GLaDOSのメインサーバがないか複数定義されています. 1つのサーバのみがメインサーバに指定できます." }
        return
    }

    if (config.parallelism != null) {
        logger.info { "オーバライドされたCommonPoolの並列数 = ${config.parallelism}" }
        System.setProperty(CommonPool.DEFAULT_PARALLELISM_PROPERTY_NAME, config.parallelism.toString())
    }

    eventWaiter = EventWaiter()

    jda = JDABuilder(AccountType.BOT).apply {
        setToken(config.token)
        setAudioSendFactory(NativeAudioSendFactory(1000))
        setGame(Game.playing("Starting..."))

        addEventListener(eventWaiter)

        featureManager = FeatureManager("jp.nephy.glados.features")
        addEventListener(featureManager.commandClient)
        addEventListener(featureManager.listenerClient)
        addEventListener(featureManager.poolClient)
    }.build()

    embeddedServer(Netty, host = config.wuiHost, port = config.wuiPort, module = Application::module).start(wait = true)
}
