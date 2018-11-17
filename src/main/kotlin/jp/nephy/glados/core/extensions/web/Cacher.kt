package jp.nephy.glados.core.extensions.web

import io.ktor.http.content.OutgoingContent
import jp.nephy.glados.core.extensions.resourcePath
import java.util.concurrent.ConcurrentHashMap

// TODO
object Cacher {
    private val assetsCache = ConcurrentHashMap<String, ByteArray?>()
    private fun getAssetAsByteArray(assetPath: String): ByteArray? {
        val plainPath = assetPath.removePrefix("/").removeSuffix("/")
        val paths = assetPath.removePrefix("/").removeSuffix("/").split("/")
        return assetsCache.getOrPut(plainPath) {
            runCatching {
                resourcePath("static", *paths.toTypedArray()).toFile().readBytes()
            }.getOrNull()
        }
    }

    fun getAssetAsString(assetPath: String): String? {
        return getAssetAsByteArray(assetPath)?.run { String(this) }
    }

    private val contentCache = ConcurrentHashMap<String, String>()
    fun getOrPut(key: String, default: () -> String): String {
        return contentCache.getOrPut(key, default)
    }

    private val resourcesCache = ConcurrentHashMap<String, OutgoingContent?>()
    fun resource(key: String, default: () -> OutgoingContent?): OutgoingContent? {
        return resourcesCache.getOrPut(key) {
            try {
                default()
            } catch (e: Exception) {
                null
            }
        }
    }
}
