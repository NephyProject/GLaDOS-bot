package jp.nephy.glados.core.extensions.web

object TwitterCard {
    enum class Type(val tagName: String) {
        Summary("summary"), SummaryLargeImage("summary_large_image"), App("app"), Player("player")
    }
}
