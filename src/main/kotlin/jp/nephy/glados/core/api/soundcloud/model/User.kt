package jp.nephy.glados.core.api.soundcloud.model

import com.google.gson.JsonObject
import jp.nephy.jsonkt.*


class User(override val json: JsonObject): JsonModel {
    val avatarUrl by json.byString("avatar_url")  // "https://i1.sndcdn.com/avatars-000338809424-572092-large.jpg"
    val city by json.byNullableJsonElement  // null
    val countryCode by json.byNullableJsonElement("country_code")  // null
    val firstName by json.byString("first_name")  // ""
    val fullName by json.byString("full_name")  // ""
    val id by json.byInt  // 307809061
    val kind by json.byString  // "user"
    val lastModified by json.byString("last_modified")  // "2018-02-23T20:52:14Z"
    val lastName by json.byString("last_name")  // ""
    val permalink by json.byString  // "scumgang6ix9ine"
    val permalinkUrl by json.byString("permalink_url")  // "https://soundcloud.com/scumgang6ix9ine"
    val uri by json.byString  // "https://api.soundcloud.com/users/307809061"
    val urn by json.byString  // "soundcloud:users:307809061"
    val username by json.byString  // "6IX9INE"
    val verified by json.byBool  // false
}
