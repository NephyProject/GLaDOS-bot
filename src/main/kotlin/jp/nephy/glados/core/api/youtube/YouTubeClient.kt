package jp.nephy.glados.core.api.youtube

import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.SearchResult


class YouTubeClient(private val apiKey: String) {
    private val httpTransport = NetHttpTransport()
    private val jsonFactory = JacksonFactory()
    private val youtube = YouTube.Builder(httpTransport, jsonFactory, HttpRequestInitializer { })
            .setApplicationName("GLaDOS")
            .build()

    fun search(query: String, limit: Int = 10): List<SearchResult> {
        val result = youtube.search().list("id,snippet").apply {
            key = apiKey
            q = query
            type = "video"
            fields = "items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)"
            maxResults = limit.toLong()
        }.execute()

        return result.items ?: return emptyList()
    }
}
