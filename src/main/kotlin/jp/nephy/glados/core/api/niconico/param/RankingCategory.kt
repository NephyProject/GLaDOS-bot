package jp.nephy.glados.core.api.niconico.param

import jp.nephy.glados.core.extensions.messages.prompt.PromptEnum

enum class RankingCategory(override val friendlyName: String, private val apiName: String? = null, val isUnited: Boolean = false): PromptEnum {
    All("カテゴリ合算", isUnited = true),

    EntertainmentParent("エンタメ・音楽", "g_ent2", true),
    Entertainment("エンターテイメント", "ent"),
    Music("音楽"),
    Sing("歌ってみた"),
    Play("演奏してみた"),
    Dance("踊ってみた"),
    Vocaloid("VOCALOID"),
    Nicoindies("ニコニコインディーズ"),

    LifeParent("生活・一般・スポ", "g_life2", true),
    Animal("動物"),
    Cooking("料理"),
    Nature("自然"),
    Travel("旅行"),
    Sport("スポーツ"),
    Lecture("ニコニコ動画講座"),
    Drive("車載動画"),
    History("歴史"),

    PoliticsParent("政治", "g_politics", true),

    ScienceParent("科学・技術", "g_tech", true),
    Science("科学"),
    Tech("ニコニコ技術部"),
    Handcraft("ニコニコ手芸部"),
    Make("作ってみた"),

    AnimeParent("アニメ・ゲーム・絵", "g_culture2", true),
    Anime("アニメ"),
    Game("ゲーム"),
    Jikkyo("実況プレイ動画"),
    Toho("東方"),
    Imas("アイドルマスター"),
    Radio("ラジオ"),
    Draw("描いてみた"),

    OtherParent("その他", "g_other", true),
    Are("例のアレ"),
    Diary("日記"),
    Other("その他");

    val internalName: String
        get() = apiName ?: name.toLowerCase()
}
