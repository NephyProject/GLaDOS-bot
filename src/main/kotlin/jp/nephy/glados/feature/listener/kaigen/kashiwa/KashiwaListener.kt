package jp.nephy.glados.feature.listener.kaigen.kashiwa

import jp.nephy.glados.GLaDOS
import jp.nephy.glados.logger
import jp.nephy.penicillin.model.Status
import jp.nephy.penicillin.model.StreamDelete
import jp.nephy.penicillin.request.streaming.UserStreamListener
import jp.nephy.utils.StringLinkedSingleCache
import net.dv8tion.jda.core.entities.TextChannel


class KashiwaListener(private val channels: List<TextChannel>): UserStreamListener {
    companion object {
        var hateCommandString by StringLinkedSingleCache { "hate" }
    }

    private val account by lazy { GLaDOS.instance.apiClient.twitter.account.verifyCredentials().complete().result }

    override fun onConnect() {
        logger.info { "UserStreamに接続しました." }
    }

    override fun onDisconnect() {
        logger.info { "UserStreamから切断されました." }
    }

    override fun onStatus(status: Status) {
        if (status.inReplyToUserId == account.id) {
            if (status.fullText.contains(hateCommandString)) {
                channels.forEach {
                    status.entities.urls.forEach { url ->
                        it.revealTweet(url.expandedUrl)
                    }
                }
            }
        }
    }

    // TODO
    override fun onDelete(delete: StreamDelete) {}
}
