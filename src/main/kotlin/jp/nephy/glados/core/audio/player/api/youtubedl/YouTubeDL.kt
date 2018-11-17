package jp.nephy.glados.core.audio.player.api.youtubedl

import jp.nephy.jsonkt.*
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class YouTubeDL(private val videoUrl: String) {
    private var cachedVideoInfo: YouTubeDLModel? = null

    private fun getResultStream(vararg args: String): InputStream? {
        val process = ProcessBuilder("youtube-dl", *args, videoUrl).start()
        process.waitFor(1, TimeUnit.SECONDS)
        return process.inputStream ?: null
    }

    private fun getResultAsString(vararg args: String): String? {
        val stream = getResultStream(*args) ?: return null
        return InputStreamReader(stream).use { it.readText() }.trim()
    }

    val info: YouTubeDLModel?
        get() {
            return cachedVideoInfo ?: try {
                val result = getResultAsString("-j") ?: return null
                result.parse<YouTubeDLModel>().also {
                    cachedVideoInfo = it
                }
            } catch (e: Throwable) {
                null
            }
        }
}
