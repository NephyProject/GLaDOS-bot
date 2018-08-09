package jp.nephy.glados.core.builder.prompt

enum class YesNoEmoji(override val emoji: String): PromptEmoji {
    Yes("👌"), No("👋");

    override val friendlyName: String
        get() = when (this) {
            Yes -> "はい"
            No -> "いいえ"
        }
}
