package jp.nephy.glados.plugins.miria.web

import com.mongodb.client.model.Sorts
import jp.nephy.glados.core.extensions.collection
import jp.nephy.glados.core.extensions.findAndParse
import jp.nephy.glados.core.extensions.web.intQuery
import jp.nephy.glados.core.extensions.web.respondJsonArray
import jp.nephy.glados.core.plugins.Plugin
import jp.nephy.glados.mongodb
import jp.nephy.jsonkt.*
import org.bson.conversions.Bson

object MiriaHistoryAPI: Plugin() {
    @Web.Page("/v1/miria/history", "api.nephy.jp")
    override suspend fun onAccess(event: Web.AccessEvent) {
        val pageIndex by event.intQuery("page") { 0 }
        val count by event.intQuery { 25 }

        event.call.respondJsonArray {
            list(pageIndex, count).toImmutableJsonArray()
        }
    }

    fun list(pageIndex: Int, count: Int, filter: Bson? = null): List<ImmutableJsonObject> {
        return mongodb.collection("MiriaYannaiyo").findAndParse<MorphologicalAnalysisResult>(filter) {
            sort(Sorts.descending("_id")).skip(pageIndex * count).limit(count)
        }.map {
            immutableJsonObjectOf(
                "process_sec" to it.sec, "random" to it.r, "date" to it.datetime,

                "original" to immutableJsonObjectOf(
                    "url" to it.tweetLink, "text" to it.original, "via" to it.via
                ), "tweet" to immutableJsonObjectOf(
                    "url" to it.url, "text" to it.chose
                ),

                "words" to it.words, "nodes" to immutableJsonArrayOf(*it.node.map {
                    immutableJsonObjectOf(
                        "surface" to it.surface, "reading" to it.reading, "feature" to it.feature, "deleted" to it.deleted, "feeling" to it.feeling?.json, "imas" to if (it.category != null && it.description != null) immutableJsonObjectOf(
                            "category" to it.category, "description" to it.description
                        ) else null
                    )
                }.toTypedArray())
            )
        }
    }
}
