package jp.nephy.glados.core.wui

import com.google.gson.JsonObject
import io.ktor.sessions.CurrentSession
import io.ktor.sessions.clear
import io.ktor.sessions.get
import jp.nephy.jsonkt.*
import okhttp3.OkHttpClient
import okhttp3.Request

data class CallbackSession(val path: String?) {
    val fullUrl: String
        get() = "https://glados.nephy.jp${path ?: "/"}"
}

data class DiscordOAuth2Session(val at: String, val tokenType: String, val expiresIn: Long, val scope: String, val refreshToken: String)

val DiscordOAuth2Session.user
    get() = OkHttpClient().newCall(
            Request.Builder()
                    .url("https://discordapp.com/api/v6/users/@me")
                    .header("Authorization", "$tokenType $at")
                    .header("User-Agent", userAgent)
                    .get()
                    .build()
    ).execute().body()!!.string().parse<DiscordUserObject>()

val DiscordOAuth2Session.guilds
    get() = OkHttpClient().newCall(
            Request.Builder()
                    .url("https://discordapp.com/api/v6/users/@me/guilds")
                    .header("Authorization", "$tokenType $at")
                    .header("User-Agent", userAgent)
                    .get()
                    .build()
    ).execute().body()!!.string().parseList<DiscordGuildObject>()

val CurrentSession.callback: CallbackSession
    get() = get<CallbackSession>().apply { clear<CallbackSession>() } ?: CallbackSession(null)

data class DiscordUserObject(override val json: JsonObject): JsonModel {
    val id by json.byString
    val username by json.byString
    val discriminator by json.byString
}

data class DiscordGuildObject(override val json: JsonObject): JsonModel {
    val id by json.byString
    val name by json.byString
    val icon by json.byString
    val owner by json.byBool
    val permissions by json.byInt
}
