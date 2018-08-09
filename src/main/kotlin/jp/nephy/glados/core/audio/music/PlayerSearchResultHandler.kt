package jp.nephy.glados.core.audio.music

import jp.nephy.glados.core.api.niconico.model.SearchResult

interface PlayerSearchResultHandler {
    fun onFoundNiconicoResult(result: SearchResult) {}
    fun onFoundYouTubeResult(result: List<com.google.api.services.youtube.model.SearchResult>) {}
    fun onNoResult() {}
}
