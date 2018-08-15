package jp.nephy.glados

import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import jp.nephy.glados.core.*
import jp.nephy.glados.core.audio.music.GuildPlayer
import jp.nephy.glados.core.feature.FeatureManager
import jp.nephy.utils.linkedCacheDir
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Guild
import java.nio.file.Paths

val jda by lazy { jdaInternal }
private lateinit var jdaInternal: JDA
val eventWaiter by lazy { eventWaiterInternal }
private lateinit var eventWaiterInternal: EventWaiter

val isDebugMode by lazy { isDebugModeInternal }
private var isDebugModeInternal = false
val config by lazy { configInternal }
private lateinit var configInternal: GLaDOSConfig
val secret by lazy { secretInternal }
private lateinit var secretInternal: SecretConfig

val logger by lazy { Logger("GLaDOS") }

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
    isDebugModeInternal = args.contains("--debug")

    secretInternal = SecretConfig.load(secretConfigPath)

    configInternal = if (isDebugMode) {
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

    eventWaiterInternal = EventWaiter()

    jdaInternal = JDABuilder(AccountType.BOT).apply {
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
