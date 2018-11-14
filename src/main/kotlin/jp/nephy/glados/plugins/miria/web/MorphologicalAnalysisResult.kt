package jp.nephy.glados.plugins.miria.web

import jp.nephy.jsonkt.*
import jp.nephy.jsonkt.delegation.*

data class MorphologicalAnalysisResult(override val json: ImmutableJsonObject): JsonModel {
    val sec by double
    val datetime by string
    val tweetLink by string
    val r by double
    val chose by string
    val words by stringList
    val via by string
    val original by string
    val url by nullableString
    val node by modelList<Node>()
    val deletedNode by modelList<Node>()

    data class Node(override val json: ImmutableJsonObject): JsonModel {
        val surface by nullableString
        val reading by nullableString
        val feature by nullableString
        val deleted by nullableBoolean
        val feeling by model<Feeling?>()
        val description by nullableString
        val category by nullableString

        data class Feeling(override val json: ImmutableJsonObject): JsonModel {
            val scores by model<Score>()
            val active by string

            data class Score(override val json: ImmutableJsonObject): JsonModel {
                val positive by int
                val negative by int
                val neutral by int
                val total by int
                val negativePercent by int("negative_percent")
                val neutralPercent by int("neutral_percent")
                val positivePercent by int("positive_percent")
            }
        }
    }
}
