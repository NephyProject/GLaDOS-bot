package jp.nephy.glados.core.audio

import jp.nephy.glados.logger
import net.dv8tion.jda.core.audio.hooks.ConnectionListener
import net.dv8tion.jda.core.audio.hooks.ConnectionStatus
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User

class ConnectionListenerImpl(private val guild: Guild): ConnectionListener {
    override fun onStatusChange(status: ConnectionStatus) {
        logger.info { "[${guild.name}] 接続ステータスが変更されました: ${status.name}" }
    }

    override fun onUserSpeaking(user: User, speaking: Boolean) {
//        if (speaking) {
//            logger.debug { "[${guild.name}] ${user.displayName} がオーディオを送信しています." }
//        } else {
//            logger.debug { "[${guild.name}] ${user.displayName} がオーディオの送信を終了しました." }
//        }
    }

    private var lastPing: Long = 0
    override fun onPing(ping: Long) {
        if (ping - lastPing > 50 || ping > 100) {
            logger.debug { "[${guild.name}] Pingが悪化しています. 現在の値: Ping ${ping}ms" }
        }
        lastPing = ping
    }
}
