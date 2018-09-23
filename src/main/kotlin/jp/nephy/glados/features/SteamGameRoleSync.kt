package jp.nephy.glados.features

import com.google.gson.JsonObject
import com.lukaspradel.steamapi.data.json.ownedgames.GetOwnedGames
import com.lukaspradel.steamapi.webapi.client.SteamWebApiClient
import com.lukaspradel.steamapi.webapi.request.builders.SteamWebApiRequestFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import io.ktor.client.request.header
import jp.nephy.glados.config
import jp.nephy.glados.core.addRole
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Listener
import jp.nephy.glados.core.hasRole
import jp.nephy.glados.core.isBotOrSelfUser
import jp.nephy.glados.core.removeRole
import jp.nephy.glados.secret
import jp.nephy.jsonkt.*
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.ReadyEvent
import java.util.concurrent.TimeUnit

class SteamGameRoleSync: BotFeature() {
    companion object {
        val steamCient = SteamWebApiClient.SteamWebApiClientBuilder(secret.forKey("steam_api_key")).build()!!
        val profileCache = mutableMapOf<Long, MutableMap<Long, DiscordUserProfile>>()
    }

    @Listener
    override fun onReady(event: ReadyEvent) {
        launch {
            while (true) {
                try {
                    synchronize(event)
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    logger.error(e) { "ゲームロールの同期中にエラーが発生しました." }
                }

                try {
                    delay(3, TimeUnit.MINUTES)
                } catch (e: CancellationException) {
                    break
                }
                profileCache.clear()
            }
        }
    }

    private suspend fun synchronize(event: ReadyEvent) {
        event.jda.guilds.forEach { guild ->
            if (!profileCache.containsKey(guild.idLong)) {
                profileCache[guild.idLong] = mutableMapOf()
            }

            val guildConfig = config.forGuild(guild) ?: return@forEach
            val steamGameRoles = guildConfig.option("sync_steam_games") {
                it.jsonArray.map { SteamGameRole(it.jsonObject) }
            }.orEmpty()
            val token = config.forGuild(guild)?.stringOption("client_token", "")
            if (steamGameRoles.isEmpty() || token.isNullOrBlank()) {
                return@forEach
            }
            val ignoreGameRole = guildConfig.role("ignore_game_role")

            val gameRoles = steamGameRoles.mapNotNull { guild.getRolesByName(it.name, false).firstOrNull() }

            guild.members.forEach memberLoop@{ member ->
                if (member.user.isBotOrSelfUser) {
                    return@memberLoop
                }

                // ignoreロールのユーザからロールを消す
                if (ignoreGameRole != null && member.hasRole(ignoreGameRole)) {
                    gameRoles.filter { member.hasRole(it.idLong) }.forEach {
                        member.removeRole(it)
                    }
                    return@memberLoop
                }

                val profile = profileCache[guild.idLong]!!.getOrPut(member.user.idLong) {
                    member.getUserProfile() ?: return@memberLoop
                }
                val steam = profile.connectedAccounts.find { it.type == "steam" } ?: return@memberLoop

                val request = SteamWebApiRequestFactory.createGetOwnedGamesRequest(steam.id, true, true, steamGameRoles.map { it.appId })
                val response = steamCient.processRequest<GetOwnedGames>(request).response
                for (game in response.games) {
                    val gameConfig = steamGameRoles.find { it.appId == game.appid } ?: continue

                    if (!guild.selfMember.hasPermission(Permission.MANAGE_ROLES)) {
                        logger.warn { "サーバ: ${guild.name} でロールの管理権限がありません." }
                        continue
                    }

                    val role = guild.getRolesByName(gameConfig.name, false).orEmpty().firstOrNull()
                    if (role == null) {
                        val create = guild.controller.createRole()
                                .setName(gameConfig.name)
                                .setMentionable(true)
                                .setHoisted(true)
                        if (gameConfig.color != null) {
                            create.setColor(gameConfig.color!!.toInt(16))
                        }

                        create.queue {
                            member.addRole(it)
                        }
                    } else if (!member.hasRole(role.idLong)) {
                        member.addRole(role)
                    }
                }
            }
        }
    }

    // TODO: OkHttp
    private val httpClient = HttpClient(Apache)

    private suspend fun Member.getUserProfile(): DiscordUserProfile? {
        val token = config.forGuild(guild)?.stringOption("client_token", "")
        if (token.isNullOrBlank()) {
            return null
        }

        return try {
            val response = httpClient.get<String>("https://discordapp.com/api/v6/users/${user.id}/profile") {
                header("Authorization", token!!)
            }
            return response.parse()
        } catch (e: Exception) {
            logger.error(e) { "プロフィールの取得に失敗しました" }
            null
        }
    }
}

class SteamGameRole(override val json: JsonObject): JsonModel {
    val appId by json.byInt("appid")
    val name by json.byString
    val color by json.byNullableString("color")
}

class DiscordUserProfile(override val json: JsonObject): JsonModel {
    val connectedAccounts by json.byModelList<ConnectedAccounts>(key = "connected_accounts")  // [...]
    val mutualGuilds by json.byModelList<MutualGuilds>(key = "mutual_guilds")  // [{"nick":"?","id":"187578406940966912"}, ...]
    val premiumSince by json.byNullableString("premium_since")  // "2017-03-23T11:02:27+00:00"
    val user by json.byModel<User>()  // {...}

    class ConnectedAccounts(override val json: JsonObject): JsonModel {
        val id by json.byString  // "123775508"
        val name by json.byString  // "slashnephy"
        val type by json.byString  // "twitch"
        val verified by json.byBool  // true
    }

    class MutualGuilds(override val json: JsonObject): JsonModel {
        val id by json.byString  // "187578406940966912"
        val nick by json.byNullableString  // "?" or null
    }

    class User(override val json: JsonObject): JsonModel {
        val avatar by json.byString  // "505575f2ba030f7f29d190a2afe25d30"
        val discriminator by json.byString  // "6666"
        val flags by json.byInt  // 4
        val id by json.byString  // "189327611829288960"
        val username by json.byString  // "razlite"
    }
}
