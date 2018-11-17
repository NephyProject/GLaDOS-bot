package jp.nephy.glados.core.audio.player.api.niconico.model

import jp.nephy.jsonkt.*
import jp.nephy.jsonkt.delegation.*

data class SearchResult(override val json: JsonObject): JsonModel {
    val data by modelList<SearchData>()  // [...]
    val meta by model<SearchMeta>()  // {...}

    data class SearchData(override val json: JsonObject): JsonModel {
        val categoryTags by string  // "音楽"
        val commentCounter by int  // 2827857
        val contentId by string  // "sm1097445"
        val description by string  // "おまえら、みっくみくにしてやんよ。歌詞はhttp://ikamo.hp.infoseek.co.jp/mikumiku.txt（9/20 1:55修正）。上げている他のもの→mylist/70391/1450136"
        val lengthSeconds by int  // 98
        val mylistCounter by int  // 217143
        val tags by string  // "音楽 初音ミク みくみくにしてあげる♪ VOCALOID ika ミクオリジナル曲 VOCALOID神話入り 初音ミク名曲リンク DAM&JOY配信中 みんなのミクうた 深夜みっく"
        val title by string  // "【初音ミク】みくみくにしてあげる♪【してやんよ】"
        val viewCounter by int  // 13287007
    }

    data class SearchMeta(override val json: JsonObject): JsonModel {
        val id by string  // "b2be2ba6-6dd6-4f4b-9417-133d10a668f6"
        val status by int  // 200
        val totalCount by int  // 154303
    }
}
