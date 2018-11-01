package jp.nephy.glados.core.api.niconico.param

import jp.nephy.glados.core.extensions.messages.prompt.PromptEmoji

enum class RankingType(override val emoji: String, override val friendlyName: String, val internalName: String): PromptEmoji {
    Top("🌟", "総合", "fav"),
    View("📺", "再生", "view"),
    Comment("💬", "コメント", "res"),
    MyList("📂", "マイリスト", "mylist")
}
