package jp.nephy.glados.core.api.soundcloud.model

import com.google.gson.JsonObject
import jp.nephy.jsonkt.JsonModel
import jp.nephy.jsonkt.byModelList
import jp.nephy.jsonkt.byString


class Charts(override val json: JsonObject): JsonModel {
    val collection by json.byModelList<Collection>()  // [...]
    val genre by json.byString  // "soundcloud:genres:all-music"
    val kind by json.byString  // "top"
    val lastUpdated by json.byString("last_updated")  // "2018-03-13T06:23:53Z"
    val nextHref by json.byString("next_href")  // "https://api-v2.soundcloud.com/charts?genre=soundcloud%3Agenres%3Aall-music&query_urn=soundcloud%3Acharts%3A9e5b391ed3284f3c9016bf63c038bfb7&offset=20&high_tier_only=false&kind=top&limit=20"
    val queryUrn by json.byString("query_urn")  // "soundcloud:charts:9e5b391ed3284f3c9016bf63c038bfb7"
}
