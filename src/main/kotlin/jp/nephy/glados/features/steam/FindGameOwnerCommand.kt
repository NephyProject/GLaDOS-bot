package jp.nephy.glados.features.steam

import com.lukaspradel.steamapi.data.json.ownedgames.GetOwnedGames
import com.lukaspradel.steamapi.webapi.request.builders.SteamWebApiRequestFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.edit
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Command
import jp.nephy.glados.core.feature.subscription.CommandEvent
import jp.nephy.glados.core.launch
import jp.nephy.glados.core.launchAndDelete
import jp.nephy.jsonkt.ImmutableJsonObject
import jp.nephy.jsonkt.delegation.*
import jp.nephy.jsonkt.parse
import jp.nephy.jsonkt.toJsonObject
import java.util.concurrent.TimeUnit

class FindGameOwnerCommand: BotFeature() {
    private val httpClient = HttpClient(Apache)

    @Command(channelType = Command.ChannelType.TextChannel, description = "指定されたSteamゲームを所有しているメンバーにメンションを飛ばします。", args = ["App ID"])
    suspend fun find(event: CommandEvent) {
        val appId = event.args.toIntOrNull()
        if (appId == null) {
            event.reply {
                embed {
                    title("AppIDは数値のみ入力可能です。")
                    color(Color.Bad)
                    timestamp()
                }
            }.launchAndDelete(30, TimeUnit.SECONDS)
            return
        }

        val model = try {
            val response = httpClient.get<String>("https://store.steampowered.com/api/appdetails/?appids=$appId")
            response.toJsonObject().values.first().asImmutableJsonObject().parse<SteamAppDetails>()
        } catch (e: Exception) {
            event.reply {
                embed {
                    title("AppID: ${appId}のゲームの検索に失敗しました。")
                    color(Color.Bad)
                    timestamp()
                }
            }.launchAndDelete(30, TimeUnit.SECONDS)
            return
        }
        if (!model.success || model.data?.name == null) {
            event.reply {
                embed {
                    title("AppID: ${appId}のゲームは見つかりませんでした。")
                    color(Color.Bad)
                    timestamp()
                }
            }.launchAndDelete(30, TimeUnit.SECONDS)
            return
        }

        event.reply {
            embed {
                title("${model.data!!.name}を所有しているメンバーを検索しています...")
                color(Color.Plain)
                timestamp()
            }
        }.launch {
            val guild = event.guild!!
            val members = SteamGameRoleSync.profileCache[guild.idLong].orEmpty()
            if (members.isEmpty()) {
                it.edit {
                    embed {
                        title("${model.data!!.name}を所有しているメンバーは見つかりませんでした。")
                        color(Color.Bad)
                        timestamp()
                    }
                }.launch()
                return@launch
            }

            val players = members.mapNotNull {
                val (id, profile) = it
                val steam = profile.connectedAccounts.find { it.type == "steam" } ?: return@mapNotNull null
                val request = SteamWebApiRequestFactory.createGetOwnedGamesRequest(steam.id, true, true, listOf(appId))
                val response = SteamGameRoleSync.steamCient.processRequest<GetOwnedGames>(request).response
                if (response.games.count { it.appid == appId } == 0) {
                    return@mapNotNull null
                }

                val member = guild.getMemberById(id) ?: return@mapNotNull null
                member.asMention
            }

            it.edit {
                embed {
                    title("${model.data!!.name}を所有しているメンバーは${players.size}人見つかりました。")
                    description { players.joinToString(" ") }
                    color(Color.Good)
                    timestamp()
                }
            }.launch()
        }
    }
}

data class SteamAppDetails(override val json: ImmutableJsonObject): JsonModel {
    val success by boolean
    val data by model<Data?>()

    data class Data(override val json: ImmutableJsonObject): JsonModel {
        val aboutTheGame by string("about_the_game")  // ""
        val background by string  // ""
        val detailedDescription by string("detailed_description")  // ""
        val developers by stringList  // ["Valve"]
        val fullgame by model<Fullgame>()  // {...}
        val genres by modelList<Genres>()  // [{"id":"1","description":"アクション"}]
        val headerImage by string("header_image")  // "https://steamcdn-a.akamaihd.net/steam/apps/380/header.jpg?t=1512757763"
        val isFree by boolean("is_free")  // false
        val linuxRequirements by immutableJsonArray("linux_requirements")  // []
        val macRequirements by immutableJsonArray("mac_requirements")  // []
        val name by string  // "Half-Life 2: Episode One Trailer"
        val packageGroups by immutableJsonArray("package_groups")  // []
        val pcRequirements by immutableJsonArray("pc_requirements")  // []
        val platforms by model<Platforms>()  // {...}
        val publishers by stringList  // ["Valve"]
        val releaseDate by model<ReleaseDate>(key = "release_date")  // {...}
        val requiredAge by int("required_age")  // 0
        val shortDescription by string("short_description")  // ""
        val steamAppid by int("steam_appid")  // 905
        val supportInfo by model<SupportInfo>(key = "support_info")  // {...}
        val supportedLanguages by string("supported_languages")  // "英語"
        val type by string  // "movie"

        data class Fullgame(override val json: ImmutableJsonObject): JsonModel {
            val appid by string  // "380"
            val name by string  // "Half-Life 2: Episode One"
        }

        data class Genres(override val json: ImmutableJsonObject): JsonModel {
            val description by string  // "アクション"
            val id by string  // "1"
        }

        data class Platforms(override val json: ImmutableJsonObject): JsonModel {
            val linux by boolean  // false
            val mac by boolean  // false
            val windows by boolean  // true
        }

        data class ReleaseDate(override val json: ImmutableJsonObject): JsonModel {
            val comingSoon by boolean("coming_soon")  // false
            val date by string  // "2006年3月1日"
        }

        data class SupportInfo(override val json: ImmutableJsonObject): JsonModel {
            val email by string  // ""
            val url by string  // ""
        }
    }
}
