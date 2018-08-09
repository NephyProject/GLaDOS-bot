package jp.nephy.glados.core.api.youtubedl.model

import com.google.gson.JsonObject
import jp.nephy.glados.core.api.youtubedl.model.common.Format
import jp.nephy.jsonkt.*
import kotlin.math.roundToLong

class YouTubeDLInfo(override val json: JsonObject): JsonModel {
    val title by json.byString
    val fulltitle by json.byString
    val description by json.byNullableString

    private val durationSec by json.byDouble("duration")
    val durationMs: Long
        get() = durationSec.times(1000).roundToLong()

    val thumbnailUrl by json.byNullableString("thumbnail")
    val height by json.byInt
    val width by json.byInt

    val webpageUrl by json.byString
    val uploader by json.byString
    val videoId by json.byString("id")
    val formats by json.byModelList<Format>()
    val ext by json.byString
}
