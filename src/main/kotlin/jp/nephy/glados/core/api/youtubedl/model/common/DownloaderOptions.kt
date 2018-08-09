package jp.nephy.glados.core.api.youtubedl.model.common

import com.google.gson.JsonObject
import jp.nephy.jsonkt.JsonModel
import jp.nephy.jsonkt.byInt


class DownloaderOptions(override val json: JsonObject): JsonModel {
    val httpChunkSize by json.byInt("http_chunk_size")  // 10485760
}
