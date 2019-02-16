package jp.nephy.glados.core.plugins.extensions.jda.messages.prompt

enum class YesNoEmoji(override val symbol: String, override val promptTitle: String, override val promptDescription: String): EmojiEnum {
    Yes("👌", "はい", "Yes"), No("👋", "いいえ", "No")
}
