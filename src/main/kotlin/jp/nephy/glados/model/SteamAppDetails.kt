package jp.nephy.glados.model

import com.google.gson.JsonObject
import jp.nephy.jsonkt.*

data class SteamAppDetails(override val json: JsonObject): JsonModel {
    val success by json.byBool
    val data by json.byModel<Data?>()

    class Data(override val json: JsonObject): JsonModel {
        val aboutTheGame by json.byString("about_the_game")  // ""
        val background by json.byString  // ""
        val detailedDescription by json.byString("detailed_description")  // ""
        val developers by json.byStringList  // ["Valve"]
        val fullgame by json.byModel<Fullgame>()  // {...}
        val genres by json.byModelList<Genres>()  // [{"id":"1","description":"アクション"}]
        val headerImage by json.byString("header_image")  // "https://steamcdn-a.akamaihd.net/steam/apps/380/header.jpg?t=1512757763"
        val isFree by json.byBool("is_free")  // false
        val linuxRequirements by json.byJsonArray("linux_requirements")  // []
        val macRequirements by json.byJsonArray("mac_requirements")  // []
        val name by json.byString  // "Half-Life 2: Episode One Trailer"
        val packageGroups by json.byJsonArray("package_groups")  // []
        val pcRequirements by json.byJsonArray("pc_requirements")  // []
        val platforms by json.byModel<Platforms>()  // {...}
        val publishers by json.byStringList  // ["Valve"]
        val releaseDate by json.byModel<ReleaseDate>(key = "release_date")  // {...}
        val requiredAge by json.byInt("required_age")  // 0
        val shortDescription by json.byString("short_description")  // ""
        val steamAppid by json.byInt("steam_appid")  // 905
        val supportInfo by json.byModel<SupportInfo>(key = "support_info")  // {...}
        val supportedLanguages by json.byString("supported_languages")  // "英語"
        val type by json.byString  // "movie"
        val website by json.byNullableJsonElement  // null

        class Fullgame(override val json: JsonObject): JsonModel {
            val appid by json.byString  // "380"
            val name by json.byString  // "Half-Life 2: Episode One"
        }

        class Genres(override val json: JsonObject): JsonModel {
            val description by json.byString  // "アクション"
            val id by json.byString  // "1"
        }

        class Platforms(override val json: JsonObject): JsonModel {
            val linux by json.byBool  // false
            val mac by json.byBool  // false
            val windows by json.byBool  // true
        }

        class ReleaseDate(override val json: JsonObject): JsonModel {
            val comingSoon by json.byBool("coming_soon")  // false
            val date by json.byString  // "2006年3月1日"
        }

        class SupportInfo(override val json: JsonObject): JsonModel {
            val email by json.byString  // ""
            val url by json.byString  // ""
        }
    }
}
