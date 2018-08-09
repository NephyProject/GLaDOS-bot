package jp.nephy.glados.core.api.niconico.model

import com.google.gson.JsonObject
import jp.nephy.jsonkt.JsonModel
import jp.nephy.jsonkt.byModel
import jp.nephy.jsonkt.byModelList

class SearchResult(override val json: JsonObject): JsonModel {
    val data by json.byModelList<SearchData>()  // [...]
    val meta by json.byModel<SearchMeta>()  // {...}
}
