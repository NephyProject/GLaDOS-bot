package jp.nephy.glados.core.api.niconico.model

import com.google.gson.JsonObject
import jp.nephy.jsonkt.JsonModel
import jp.nephy.jsonkt.byInt
import jp.nephy.jsonkt.byString

class SearchMeta(override val json: JsonObject): JsonModel {
    val id by json.byString  // "b2be2ba6-6dd6-4f4b-9417-133d10a668f6"
    val status by json.byInt  // 200
    val totalCount by json.byInt  // 154303
}
