package jp.nephy.glados.core.api.soundcloud.param

import jp.nephy.glados.core.builder.prompt.PromptEmoji

enum class ChartType(override val emoji: String): PromptEmoji {
    Top("🌟"), Trending("🔥");

    val internalName: String
        get() = name.toLowerCase()
    override val friendlyName: String
        get() = when (this) {
            Top -> "人気順"
            Trending -> "話題順"
        }
}
