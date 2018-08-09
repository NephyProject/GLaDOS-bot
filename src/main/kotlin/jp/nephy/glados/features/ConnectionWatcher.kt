package jp.nephy.glados.features

import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Listener
import net.dv8tion.jda.core.events.*

class ConnectionWatcher: BotFeature() {
    @Listener
    override fun onReady(event: ReadyEvent) {
        logger.info { "Discordへの接続が確立されました." }
    }

    @Listener
    override fun onDisconnect(event: DisconnectEvent) {
        logger.warn { "Discordとの接続が切断されました." }
    }

    @Listener
    override fun onResume(event: ResumedEvent) {
        logger.info { "Discordとの接続が再開しました." }
    }

    @Listener
    override fun onShutdown(event: ShutdownEvent) {
        logger.info { "Discordとの接続が終了しました." }
    }

    @Listener
    override fun onException(event: ExceptionEvent) {
        logger.error(event.cause) { "Discord接続中に例外が発生しました." }
    }
}
