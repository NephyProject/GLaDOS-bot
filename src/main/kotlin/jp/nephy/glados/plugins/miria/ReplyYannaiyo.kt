package jp.nephy.glados.plugins.miria

import com.mongodb.client.model.Filters
import jp.nephy.glados.config
import jp.nephy.glados.core.extensions.MongoConfigMap
import jp.nephy.glados.core.extensions.collection
import jp.nephy.glados.core.extensions.contains
import jp.nephy.glados.core.extensions.insertOne
import jp.nephy.glados.core.plugins.Plugin
import jp.nephy.glados.mongodb
import jp.nephy.penicillin.core.PenicillinException
import jp.nephy.penicillin.core.TwitterErrorMessage
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object ReplyYannaiyo: Plugin() {
    private val account = config.twitterAccount("MiriaYannaiyo_Official")

    private val replyHistories = mongodb.collection("MiriaYannaiyoReplyHistory")
    private var replySinceId: Long? = null

    private val respondWordsInReply = arrayOf("やって", "やろう", "やんなよ", "やめろ", "やんないで", "やれ", "やめて", "やる気", "やりな", "せえや", "しろ", "して", "しよう", "せよ", "やるな")
    private val followBackWordsInReply = arrayOf("フォロバ", "フォロー", "ふぉろば", "ふぉろー", "follow")

    private val yannaiyoRegex1 = "^みりあ(.+?)やんないよ$".toRegex()
    private val yannaiyoRegex2 = "^みりあも(.+?)(やるー|やーるー！|やーらない！)$".toRegex()

    @Loop(12, TimeUnit.SECONDS)
    suspend fun reply() {
        account.officialClient.use { client ->
            val mentions = client.timeline.mention(count = 50, sinceId = replySinceId, tweetMode = "extended").await()
            if (mentions.isEmpty()) {
                return
            }

            for (status in mentions) {
                if (status.user.screenName == "Miria_Feedback") {
                    continue
                }
                // すでに反応済みであればbreak
                if (replyHistories.contains(Filters.eq("id", status.id))) {
                    return
                }
                val text = status.fullText()
                replyHistories.insertOne("id" to status.id, "user_id" to status.user.id, "text" to text)

                // リプライ先のツイート情報がなければスキップ
                if (status.inReplyToStatusId == null) {
                    continue
                }
                // BANクライアントチェック
                val bannedClient = BannedCollection.checkClientRules(status.source.name)
                if (bannedClient != null) {
                    logger.info { "https://twitter.com/${status.user.screenName}/status/${status.id} を無視しました。\n理由: `${bannedClient.name}` (${bannedClient.category}) からのツイートであるため。" }
                    continue
                }
                // BANスクリーンネームチェック
                val bannedUser = BannedCollection.checkUserRules(status.user.screenName)
                if (bannedUser != null) {
                    logger.info { "https://twitter.com/${status.user.screenName}/status/${status.id} を無視しました。\n理由: @${bannedUser.screen_name} (${bannedUser.reason}) のツイートであるため。" }
                    continue
                }
                // 非日本語圏のツイートを除外
                if (status.lang.value != "ja" && status.user.lang.value != "ja") {
                    continue
                }
                // クールダウンチェック
                val remaining = Cooldowns.remainingSeconds(status.user.id)
                if (remaining > 0) {
                    logger.debug { "クールダウン発動: @${status.user.screenName} 残り${remaining}秒" }
                    continue
                }
                Cooldowns.update(status.user.id)

                if (followBackWordsInReply.any { it in text }) {  // フォロバ
                    try {
                        client.friendship.create(userId = status.user.id).await()
                    } catch (e: Exception) {
                        logger.warn { "フォロー規制中のため @${status.user.screenName} にフォロー返しできませんでした。" }
                    }
                } else if (respondWordsInReply.any { it in text }) {  // リプライ反応
                    val targetStatus = client.status.show(id = status.inReplyToStatusId!!).await()
                    // @MiriaYannaiyoのツイート
                    if (targetStatus.result.user.id == account.user.id) {
                        val previousWord = yannaiyoRegex1.matchEntire(targetStatus.result.text)?.groupValues?.get(1) ?: yannaiyoRegex2.matchEntire(targetStatus.result.text)?.groupValues?.get(1) ?: continue

                        // BANワードチェック
                        val bannedWord = BannedCollection.checkWordRules(previousWord.toLowerCase())
                        if (bannedWord != null) {
                            logger.info { "https://twitter.com/${status.user.screenName}/status/${status.id} を無視しました。\n理由: `${bannedWord.word}` (${bannedWord.category}) を含む元ツイートに対するリプライであるため。" }
                            continue
                        }

                        val replyText = ScheduledYannaiyo.choosePattern(previousWord).first
                        try {
                            client.status.update(replyText, inReplyToStatusId = status.id).await()
                            logger.info { "@${status.user.screenName} へのリプライ: $replyText" }
                        } catch (e: PenicillinException) {
                            if (e.error == TwitterErrorMessage.StatusIsADuplicate) {
                                logger.error { "リプライが重複しました: $replyText" }
                            } else {
                                logger.error(e) { "リプライ中にエラーが発生しました." }
                            }
                        }
                    }
                }

                replyHistories.insertOne("id" to status.id, "user_id" to status.user.id, "text" to text)
            }

            replySinceId = mentions.first().id
        }
    }

    private object Cooldowns {
        private val data = MongoConfigMap<Long, Long?>("MiriaYannaiyoReplyCooldown") { null }
        private const val thresholdSeconds = 300

        suspend fun update(id: Long) {
            data[id] = Date().time
        }

        fun remainingSeconds(id: Long): Int {
            val previous = data[id] ?: return 0
            return (thresholdSeconds - (Date().time - previous) / 1000.0).roundToInt()
        }
    }
}
