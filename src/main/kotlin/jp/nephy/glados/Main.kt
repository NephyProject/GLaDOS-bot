package jp.nephy.glados

import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import jp.nephy.glados.core.*
import jp.nephy.glados.core.audio.music.GuildPlayer
import jp.nephy.glados.core.feature.FeatureManager
import jp.nephy.utils.linkedCacheDir
import kotlinx.coroutines.experimental.CommonPool
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Guild
import java.nio.file.Paths

internal lateinit var jda: JDA
    private set
lateinit var eventWaiter: EventWaiter
    private set

internal var isDebugMode = false
    private set
internal lateinit var config: GLaDOSConfig
    private set
internal lateinit var secret: SecretConfig
    private set

private val logger by lazy { Logger("GLaDOS") }

private val players = mutableMapOf<Long, GuildPlayer>()
val Guild.player: GuildPlayer?
    get() = synchronized(players) {
        players.getOrPut(idLong) {
            val guildConfig = config.forGuild(this) ?: return null
            val defaultVoiceChannel = guildConfig.voiceChannel("default") ?: return null

            GuildPlayer(this, guildConfig, defaultVoiceChannel).apply {
                guild.audioManager.isAutoReconnect = true
                guild.audioManager.connectionListener = connectionListener
                guild.audioManager.sendingHandler = sendingHandler

                logger.info { "[${currentVoiceChannel.name} (${guild.name})] プレイヤーが生成されました." }
            }
        }
    }

fun main(args: Array<String>) {
    linkedCacheDir = Paths.get("cache")
    isDebugMode = args.contains("--debug")

    secret = SecretConfig.load(secretConfigPath)

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
        System.setProperty(CommonPool.DEFAULT_PARALLELISM_PROPERTY_NAME, config.parallelism.toString())
    }

    eventWaiter = EventWaiter()

    jda = JDABuilder(AccountType.BOT).apply {
        setToken(config.token)
        setAudioSendFactory(NativeAudioSendFactory())
        setGame(Game.playing("Starting..."))

        addEventListener(eventWaiter)

        val featureManager = FeatureManager("jp.nephy.glados.features")
        addEventListener(featureManager.commandClient)
        addEventListener(featureManager.listenerClient)
        addEventListener(featureManager.poolClient)
    }.build()
}
