package jp.nephy.glados.plugins.miria.web

import com.mongodb.client.model.Filters
import jp.nephy.glados.core.extensions.and
import jp.nephy.glados.core.extensions.or
import jp.nephy.glados.core.extensions.web.respondJsonArray
import jp.nephy.glados.core.plugins.Plugin
import jp.nephy.jsonkt.*

object MiriaSearchAPI: Plugin() {
    @Web.Page("/v1/miria/search", "api.nephy.jp")
    override suspend fun onAccess(event: Web.AccessEvent) {
        val text = event.call.parameters["text"]
        val via = event.call.parameters["via"]
        val pageIndex = event.call.parameters["page"]?.toIntOrNull() ?: 0
        val count = event.call.parameters["count"]?.toIntOrNull() ?: 25

        event.call.respondJsonArray {
            val filter = Filters.regex("via", via.orEmpty(), "im") and (Filters.regex("chose", text.orEmpty(), "im") or Filters.regex("original", text.orEmpty(), "im"))

            MiriaHistoryAPI.list(pageIndex, count, filter).toImmutableJsonArray()
        }
    }
}
