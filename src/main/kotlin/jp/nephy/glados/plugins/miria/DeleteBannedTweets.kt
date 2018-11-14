package jp.nephy.glados.plugins.miria

import jp.nephy.glados.config
import jp.nephy.glados.core.plugins.Plugin
import jp.nephy.penicillin.core.PenicillinException
import jp.nephy.penicillin.core.TwitterErrorMessage
import net.dv8tion.jda.core.events.ReadyEvent

object DeleteBannedTweets: Plugin() {
    private val account = config.twitterAccount("MiriaYannaiyo_Official")

    override suspend fun onReady(event: ReadyEvent) {
        var maxId: Long? = null

        account.officialClient.use { client ->
            repeat(50) { i ->
                val timeline = client.timeline.user(count = 200, maxId = maxId).await()
                for (status in timeline) {
                    val banned = BannedCollection.checkWordRules(status.fullText().split("みりあ").drop(1).joinToString("みりあ")) ?: continue

                    try {
                        client.status.delete(status.id).await()
                        logger.info { "ツイート: `${status.fullText()}` を削除しました。(${i + 1}/10)\n理由: `${banned.word}` (${banned.category}) を含むため。" }
                    } catch (e: PenicillinException) {
                        if (e.error != TwitterErrorMessage.NoStatusFoundWithThatID) {
                            logger.error(e) { "ツイートの削除に失敗しました: `${status.fullText()}`" }
                        }
                    }
                }

                maxId = timeline.lastOrNull()?.id ?: return@repeat
            }
        }
    }
}
