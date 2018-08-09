package jp.nephy.glados.core.api.soundcloud.model

import com.google.gson.JsonObject
import jp.nephy.jsonkt.JsonModel
import jp.nephy.jsonkt.byBool
import jp.nephy.jsonkt.byInt
import jp.nephy.jsonkt.byString


class PublisherMetadata(override val json: JsonObject): JsonModel {
    val albumTitle by json.byString("album_title")  // "Day69"
    val artist by json.byString  // "6ix9ine"
    val explicit by json.byBool  // true
    val id by json.byInt  // 403657665
    val isrc by json.byString  // "QMEU31802751"
    val iswc by json.byString  // ""
    val pLine by json.byString("p_line")  // ""
    val pLineForDisplay by json.byString("p_line_for_display")  // "℗ "
    val publisher by json.byString  // "TenThousand Projects, LLC"
    val releaseTitle by json.byString("release_title")  // ""
    val upcOrEan by json.byString("upc_or_ean")  // ""
    val urn by json.byString  // "soundcloud:queue:403657665"
    val writerComposer by json.byString("writer_composer")  // ""
}
