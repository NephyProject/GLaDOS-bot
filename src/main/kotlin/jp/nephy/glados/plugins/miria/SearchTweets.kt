package jp.nephy.glados.plugins.miria

import jp.nephy.glados.config
import jp.nephy.glados.core.extensions.collection
import jp.nephy.glados.core.extensions.contains
import jp.nephy.glados.core.extensions.eq
import jp.nephy.glados.core.plugins.Plugin
import jp.nephy.glados.mongodb
import jp.nephy.glados.slack
import jp.nephy.jsonkt.*
import jp.nephy.penicillin.endpoints.parameters.SearchResultType
import org.litote.kmongo.insertOne
import java.util.concurrent.TimeUnit

object SearchTweets: Plugin() {
    private val account = config.twitterAccount("MiriaYannaiyo_Official")
    private val searches = mongodb.collection("EgoSearchMiria")

    private const val searchQuery = "\"みりあやんないよ\" OR miriayannaiyo OR \"やんないよbot\" -from:MiriaYannaiyo -to:MiriaYannaiyo -from:Miria_Feedback -to:Miria_Feedback -from:CocoaYannaiyo -from:MIRIaYaNNAIYo__ -to:MIRIaYaNNAIYo__"
    private val replyRegex = "@MiriaYannaiyo\\s".toRegex(RegexOption.IGNORE_CASE)

    @Loop(30, TimeUnit.SECONDS)
    suspend fun search() {
        val searchResult = account.officialClient.use {
            it.search.searchUniversal(q = searchQuery, modules = "tweet", resultType = SearchResultType.Recent).await()
        }

        for (it in searchResult.result.statuses) {
            val tweet = it.data
            if (searches.contains("id" eq tweet.id)) {
                continue
            }
            val text = tweet.fullText()
            if (text.contains(replyRegex)) {
                continue
            }

            if ("みりあやんないよ" in tweet.user.name || BannedCollection.checkClientRules(tweet.source.name) != null) {
                continue
            }

            slack.message("#search-miria") {
                username("${tweet.user.name} @${tweet.user.screenName}")
                icon(tweet.user.profileImageUrlHttps)
                textBuilder {
                    appendln(text)
                    appendln("<https://twitter.com/${tweet.user.screenName}/status/${tweet.id}|${tweet.source.name}>")
                }
            }

            searches.insertOne(tweet.toJsonString())
        }
    }
}
