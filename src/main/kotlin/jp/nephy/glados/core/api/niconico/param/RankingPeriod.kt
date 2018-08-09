package jp.nephy.glados.core.api.niconico.param

import jp.nephy.glados.core.builder.prompt.PromptEmoji

enum class RankingPeriod(override val emoji: String, override val friendlyName: String): PromptEmoji {
    Hourly("🕒", "毎時"),
    Daily("⏰", "24時間"),
    Weekly("⏳", "週間"),
    Monthly("📅", "月間"),
    Total("📊", "合計")
}
