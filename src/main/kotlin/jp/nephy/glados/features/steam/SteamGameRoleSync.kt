package jp.nephy.glados.features.steam

import com.lukaspradel.steamapi.data.json.ownedgames.GetOwnedGames
import com.lukaspradel.steamapi.webapi.client.SteamWebApiClient
import com.lukaspradel.steamapi.webapi.request.builders.SteamWebApiRequestFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import io.ktor.client.request.header
import jp.nephy.glados.config
import jp.nephy.glados.core.*
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Event
import jp.nephy.glados.dispatcher
import jp.nephy.glados.secret
import jp.nephy.jsonkt.ImmutableJsonObject
import jp.nephy.jsonkt.delegation.*
import jp.nephy.jsonkt.parse
import jp.nephy.jsonkt.parseList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.ReadyEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.collections.set

class SteamGameRoleSync: BotFeature() {
    companion object {
        val steamCient = SteamWebApiClient.SteamWebApiClientBuilder(secret.forKey("steam_api_key")).build()!!
        val profileCache = ConcurrentHashMap<Long, ConcurrentHashMap<Long, DiscordUserProfile>>()
    }

    @Event
    override suspend fun onReady(event: ReadyEvent) {
        GlobalScope.launch(dispatcher) {
            while (true) {
                try {
                    synchronize(event)
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    logger.error(e) { "ゲームロールの同期中にエラーが発生しました。" }
                }

                try {
                    delay(TimeUnit.MINUTES.toMillis(3))
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
                profileCache[guild.idLong] = ConcurrentHashMap()
            }

            val guildConfig = config.forGuild(guild) ?: return@forEach
            val steamGameRoles = guildConfig.option("sync_steam_games") {
                it.asImmutableJsonArray().parseList<SteamGameRole>()
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
                        logger.warn { "サーバ: ${guild.name} でロールの管理権限がありません。" }
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

                        create.launch {
                            member.addRole(it)
                        }
                    } else if (!member.hasRole(role.idLong)) {
                        member.addRole(role)
                    }
                }
            }
        }
    }

    private val httpClient = HttpClient(Apache)

    private suspend fun Member.getUserProfile(): DiscordUserProfile? {
        val token = config.forGuild(guild)?.stringOption("client_token", "")
        if (token.isNullOrBlank()) {
            return null
        }

        return try {
            val response = httpClient.get<String>("https://discordapp.com/api/v6/users/${user.id}/profile") {
                header("Authorization", token)
            }
            return response.parse()
        } catch (e: Exception) {
            logger.error(e) { "プロフィールの取得に失敗しました" }
            null
        }
    }
}

data class SteamGameRole(override val json: ImmutableJsonObject): JsonModel {
    val appId by int("appid")
    val name by string
    val color by nullableString("color")
}

data class DiscordUserProfile(override val json: ImmutableJsonObject): JsonModel {
    val connectedAccounts by modelList<ConnectedAccounts>(key = "connected_accounts")  // [...]
    val mutualGuilds by modelList<MutualGuilds>(key = "mutual_guilds")  // [{"nick":"?","id":"187578406940966912"}, ...]
    val premiumSince by nullableString("premium_since")  // "2017-03-23T11:02:27+00:00"
    val user by model<User>()  // {...}

    data class ConnectedAccounts(override val json: ImmutableJsonObject): JsonModel {
        val id by string  // "123775508"
        val name by string  // "slashnephy"
        val type by string  // "twitch"
        val verified by boolean  // true
    }

    data class MutualGuilds(override val json: ImmutableJsonObject): JsonModel {
        val id by string  // "187578406940966912"
        val nick by nullableString  // "?" or null
    }

    data class User(override val json: ImmutableJsonObject): JsonModel {
        val avatar by string  // "505575f2ba030f7f29d190a2afe25d30"
        val discriminator by string  // "6666"
        val flags by int  // 4
        val id by string  // "189327611829288960"
        val username by string  // "razlite"
    }
}
