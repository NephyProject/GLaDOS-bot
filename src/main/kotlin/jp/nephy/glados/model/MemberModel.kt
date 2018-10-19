package jp.nephy.glados.model

import com.google.gson.JsonObject
import jp.nephy.jsonkt.*

data class MemberModel(override val json: JsonObject): JsonModel {
    val index by json.byInt
    val importFrom by json.byNullableString("import_from")
    val id by json.byString
    val name by json.byString
    val nickname by json.byNullableString
    val effectiveName by lazy { nickname ?: name }
    val alias by json.byStringList
    val roles by json.byEnumList(Role::class, unknown = Role.Undefined)
    val profile by json.byModel<Profile>()
    val skills by json.byModelList<Skill>()
    val accounts by json.byModel<Accounts>()

    enum class Role(override val value: String, val description: String): JsonEnum<String> {
        InitialKaigen("InitialKaigen", "初期かいげんメンバー"),

        ProjectManager("ProjectManager", "プロジェクトマネージャー"),
        Contributor("Contributor", "プロジェクト支援者"),
        Developer("Developer", "デベロッパー"),

        DiscordAdmin("DiscordAdmin", "Discord管理者"),

        Undefined("Undefined", "未定義")
    }

    data class Profile(override val json: JsonObject): JsonModel {
        val bio by json.byNullableString
        val birthday by json.byModel<Birthday>()
        val email by json.byStringList
        val iconUrl by json.byString("icon_url")
        val location by json.byNullableString
        val prefecture by json.byString
        val url by json.byStringList
        val amazonWishlistUrl by json.byNullableString("amazon_wishlist_url")

        data class Birthday(override val json: JsonObject): JsonModel {
            val day by json.byNullableInt
            val month by json.byNullableInt
            val year by json.byNullableInt
        }
    }

    data class Skill(override val json: JsonObject): JsonModel {
        val degree by json.byInt
        val name by json.byString
    }

    data class Accounts(override val json: JsonObject): JsonModel {
        val twitter by json.byModelList<Twitter>()
        val steam by json.byModelList<Steam>()
        val discord by json.byModelList<Discord>()
        val github by json.byModelList<GitHub>()
        val twitch by json.byModelList<Twitch>()
        val instagram by json.byModelList<Instagram>()
        val niconico by json.byModelList<Niconico>()
        val niconicoCommunity by json.byModelList<NiconicoCommunity>(key = "niconico_community")
        val youtube by json.byModelList<YouTube>()
        val flickr by json.byModelList<Flickr>()
        val minecraft by json.byModelList<Minecraft>()
        val ff14 by json.byModelList<FF14>()
        val spotify by json.byModelList<Spotify>()
        val vrchat by json.byModelList<VRChat>()
        val psn by json.byModelList<PlayStationNetwork>()
        val qiita by json.byModelList<Qiita>()
        val blizzard by json.byModelList<Blizzard>()
        val other by json.byModelList<Other>()

        abstract class Account(json: JsonObject): JsonModel {
            val private by json.byBool { false }
            val internal by json.byBool { false }
        }

        interface WithProfileUrl {
            val profileUrl: String
        }

        interface WithTag {
            val tag: String
            val username: String
                get() = tag.split("#").first()
            val discriminator: String
                get() = tag.split("#").last()
        }

        data class Twitter(override val json: JsonObject): Account(json), WithProfileUrl {
            val id by json.byLong
            val sn by json.byString
            override val profileUrl: String
                get() = "https://twitter.com/$sn"
        }

        data class Discord(override val json: JsonObject): Account(json), WithTag {
            override val tag by json.byString
        }

        data class Steam(override val json: JsonObject): Account(json), WithProfileUrl {
            val familySharing by json.byLongList("family_sharing")
            val id by json.byLong
            val username by json.byNullableString

            override val profileUrl: String
                get() = if (username != null) {
                    "https://steamcommunity.com/id/$username"
                } else {
                    "https://steamcommunity.com/profiles/$id"
                }
        }

        data class GitHub(override val json: JsonObject): Account(json), WithProfileUrl {
            val username by json.byString
            override val profileUrl: String
                get() = "https://github.com/$username"
        }

        data class Minecraft(override val json: JsonObject): Account(json), WithProfileUrl {
            val id by json.byNullableString
            val uuid by json.byString
            override val profileUrl: String
                get() = "https://ja.namemc.com/profile/$uuid"
        }

        data class Twitch(override val json: JsonObject): Account(json), WithProfileUrl {
            val id by json.byString
            override val profileUrl: String
                get() = "https://twitch.tv/$id"
        }

        data class Instagram(override val json: JsonObject): Account(json), WithProfileUrl {
            val id by json.byString
            override val profileUrl: String
                get() = "https://www.instagram.com/$id"
        }

        data class Niconico(override val json: JsonObject): Account(json), WithProfileUrl {
            val id by json.byLong
            override val profileUrl: String
                get() = "http://www.nicovideo.jp/user/$id"
        }

        data class NiconicoCommunity(override val json: JsonObject): Account(json), WithProfileUrl {
            val id by json.byString
            override val profileUrl: String
                get() = "http://com.nicovideo.jp/community/$id"
        }

        data class YouTube(override val json: JsonObject): Account(json), WithProfileUrl {
            val id by json.byString
            override val profileUrl: String
                get() = "https://www.youtube.com/channel/$id"
        }

        data class Flickr(override val json: JsonObject): Account(json), WithProfileUrl {
            val id by json.byString
            val username by json.byString
            override val profileUrl: String
                get() = "https://www.flickr.com/photos/$id"
        }

        data class FF14(override val json: JsonObject): Account(json), WithProfileUrl {
            val id by json.byLong
            override val profileUrl: String
                get() = "https://jp.finalfantasyxiv.com/lodestone/character/$id"
        }

        data class VRChat(override val json: JsonObject): Account(json), WithProfileUrl {
            val id by json.byString
            val username by json.byString
            override val profileUrl: String
                get() = "https://www.vrchat.net/home/user/$id"
        }

        data class Spotify(override val json: JsonObject): Account(json), WithProfileUrl {
            val id by json.byString
            override val profileUrl: String
                get() = "https://open.spotify.com/user/$id"
        }

        data class PlayStationNetwork(override val json: JsonObject): Account(json), WithProfileUrl {
            val id by json.byString
            override val profileUrl: String
                get() = "https://my.playstation.com/profile/$id"
        }

        data class Qiita(override val json: JsonObject): Account(json), WithProfileUrl {
            val id by json.byString
            override val profileUrl: String
                get() = "https://qiita.com/$id"
        }

        data class Blizzard(override val json: JsonObject): Account(json) {
            val tag by json.byString
        }

        data class Other(override val json: JsonObject): Account(json), WithProfileUrl {
            val id by json.byNullableString
            override val profileUrl by json.byString("profile_url")
            val service by json.byModel<UnknownService>()
            val username by json.byNullableString

            data class UnknownService(override val json: JsonObject): JsonModel {
                val name by json.byString
                val url by json.byString
            }
        }
    }
}
