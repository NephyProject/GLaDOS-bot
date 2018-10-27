package jp.nephy.glados.core.wui

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.userAgent
import io.ktor.sessions.CurrentSession
import io.ktor.sessions.clear
import io.ktor.sessions.get
import jp.nephy.glados.httpClient
import jp.nephy.glados.userAgent
import jp.nephy.jsonkt.ImmutableJsonObject
import jp.nephy.jsonkt.delegation.JsonModel
import jp.nephy.jsonkt.delegation.boolean
import jp.nephy.jsonkt.delegation.int
import jp.nephy.jsonkt.delegation.string
import jp.nephy.jsonkt.parse
import jp.nephy.jsonkt.parseList
import kotlinx.coroutines.runBlocking

data class CallbackSession(val path: String?) {
    val fullUrl: String
        get() = "https://glados.nephy.jp${path ?: "/"}"
}

data class DiscordOAuth2Session(val at: String, val tokenType: String, val expiresIn: Long, val scope: String, val refreshToken: String)

val DiscordOAuth2Session.user: DiscordUserObject
    get() {
        val json = runBlocking {
            httpClient.get<String>("https://discordapp.com/api/v6/users/@me") {
                userAgent(userAgent)
                header(HttpHeaders.Authorization, "$tokenType $at")
            }
        }
        return json.parse()
    }

val DiscordOAuth2Session.guilds: List<DiscordGuildObject>
    get() {
        val json = runBlocking {
            httpClient.get<String>("https://discordapp.com/api/v6/users/@me/guilds") {
                userAgent(userAgent)
                header(HttpHeaders.Authorization, "$tokenType $at")
            }
        }
        return json.parseList()
    }

val CurrentSession.callback: CallbackSession
    get() = get<CallbackSession>().apply { clear<CallbackSession>() } ?: CallbackSession(null)

data class DiscordUserObject(override val json: ImmutableJsonObject): JsonModel {
    val id by string
    val username by string
    val discriminator by string
}

data class DiscordGuildObject(override val json: ImmutableJsonObject): JsonModel {
    val id by string
    val name by string
    val icon by string
    val owner by boolean
    val permissions by int
}
