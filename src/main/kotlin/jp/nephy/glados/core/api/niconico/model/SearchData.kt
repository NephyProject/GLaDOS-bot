package jp.nephy.glados.core.api.niconico.model

import com.google.gson.JsonObject
import jp.nephy.jsonkt.JsonModel
import jp.nephy.jsonkt.byInt
import jp.nephy.jsonkt.byString

class SearchData(override val json: JsonObject): JsonModel {
    val categoryTags by json.byString  // "音楽"
    val commentCounter by json.byInt  // 2827857
    val contentId by json.byString  // "sm1097445"
    val description by json.byString  // "おまえら、みっくみくにしてやんよ。歌詞はhttp://ikamo.hp.infoseek.co.jp/mikumiku.txt（9/20 1:55修正）。上げている他のもの→mylist/70391/1450136"
    val lengthSeconds by json.byInt  // 98
    val mylistCounter by json.byInt  // 217143
    val tags by json.byString  // "音楽 初音ミク みくみくにしてあげる♪ VOCALOID ika ミクオリジナル曲 VOCALOID神話入り 初音ミク名曲リンク DAM&JOY配信中 みんなのミクうた 深夜みっく"
    val title by json.byString  // "【初音ミク】みくみくにしてあげる♪【してやんよ】"
    val viewCounter by json.byInt  // 13287007
}
