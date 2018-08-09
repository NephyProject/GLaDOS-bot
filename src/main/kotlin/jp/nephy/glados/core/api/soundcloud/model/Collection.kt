package jp.nephy.glados.core.api.soundcloud.model

import com.google.gson.JsonObject
import jp.nephy.jsonkt.JsonModel
import jp.nephy.jsonkt.byFloat
import jp.nephy.jsonkt.byModel


class Collection(override val json: JsonObject): JsonModel {
    val score by json.byFloat  // 4175197.0
    val track by json.byModel<Track>()  // {...}
}
