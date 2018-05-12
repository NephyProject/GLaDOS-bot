package jp.nephy.glados

import com.jagrosh.jdautilities.command.CommandClientBuilder
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import jp.nephy.glados.component.MessageCacheManager
import jp.nephy.glados.component.api.ApiClient
import jp.nephy.glados.component.audio.music.PlayerManager
import jp.nephy.glados.component.config.GLaDOSConfig
import jp.nephy.glados.component.config.GLaDOSParameter
import jp.nephy.glados.component.config.GLaDOSSecret
import jp.nephy.glados.component.helper.Color
import jp.nephy.glados.component.helper.embedMention
import jp.nephy.glados.feature.CommandFeature
import jp.nephy.glados.feature.ListenerFeature
import jp.nephy.utils.enumurateClasses
import jp.nephy.utils.linkedCacheDir
import jp.nephy.utils.logger
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import java.nio.file.Paths
import kotlin.system.measureTimeMillis


val logger = logger("GLaDOS")

class GLaDOS internal constructor(val config: GLaDOSConfig, val parameter: GLaDOSParameter, val secret: GLaDOSSecret, val isDebugMode: Boolean) {
    companion object {
        const val version = "1.0.0-alpha2"

        private lateinit var instanceInternal: GLaDOS
        val instance by lazy { instanceInternal }
    }

    init {
        linkedCacheDir = Paths.get("tmp", "cache")
        instanceInternal = this
    }

    val apiClient = ApiClient()
    val playerManager = PlayerManager()
    val messageCacheManager = MessageCacheManager()

    val eventWaiter = EventWaiter()
    private val commandClient = CommandClientBuilder()
            .setPrefix(parameter.primaryCommandPrefix)
            .setAlternativePrefix(parameter.secondaryCommandPrefix)
            .setOwnerId(parameter.ownerId.toString())
            .setEmojis("\uD83D\uDE03", "\uD83D\uDE2E", "\uD83D\uDE26")
            .setHelpConsumer {
                it.embedMention {
                    title("利用可能なコマンドの一覧です")
                    color(Color.Good)
                    it.client.commands.filterNot { it.isHidden }.forEach {
                        field("${it.name} ${it.arguments ?: ""}") { it.help }
                    }
                }.queue()
            }
            .build()!!

    val jda = JDABuilder(AccountType.BOT)
            .setToken(config.token)
            .setAudioSendFactory(NativeAudioSendFactory())
            .addEventListener(eventWaiter)
            .addEventListener(commandClient)
            .buildAsync()!!

    init {
        val featuresLoadMs = measureTimeMillis {
            enumurateClasses<ListenerFeature>("jp.nephy.glados.feature.listener")
                    .map { it.newInstance() }
                    .forEach {
                        jda.addEventListener(it)
                        logger.debug { "Listener: ${it.javaClass.canonicalName} がロードされました." }
                    }
            enumurateClasses<CommandFeature>("jp.nephy.glados.feature.command")
                    .map { it.newInstance() }
                    .forEach {
                        commandClient.addCommand(it)
                        logger.debug { "Command: ${it.aliases.plus(it.name).joinToString(", ")} が追加されました." }
                    }
        }
        logger.info { "${featuresLoadMs}ms でFeatureのロードを完了しました." }
    }
}
