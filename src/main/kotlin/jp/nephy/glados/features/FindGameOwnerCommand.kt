package jp.nephy.glados.features

import com.google.gson.JsonObject
import com.lukaspradel.steamapi.data.json.ownedgames.GetOwnedGames
import com.lukaspradel.steamapi.webapi.request.builders.SteamWebApiRequestFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.deleteQueue
import jp.nephy.glados.core.builder.edit
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Command
import jp.nephy.glados.core.feature.subscription.CommandChannelType
import jp.nephy.glados.core.feature.subscription.CommandEvent
import jp.nephy.jsonkt.*

class FindGameOwnerCommand: BotFeature() {
    private val httpClient = HttpClient(Apache)

    @Command(channelType = CommandChannelType.TextChannel, description = "指定されたSteamゲームを所有しているメンバーにメンションを飛ばします。", args = ["App ID"])
    suspend fun find(event: CommandEvent) {
        val appId = event.args.toIntOrNull() ?: return event.reply {
            embed {
                title("AppIDは数値のみ入力可能です。")
                color(Color.Bad)
                timestamp()
            }
        }.deleteQueue(30)

        val model = try {
            val response = httpClient.get<String>("https://store.steampowered.com/api/appdetails/?appids=$appId")
            response.toJsonObject().values.first().toJsonObject().parse<SteamAppDetails>()
        } catch (e: Exception) {
            return event.reply {
                embed {
                    title("AppID: ${appId}のゲームの検索に失敗しました。")
                    color(Color.Bad)
                    timestamp()
                }
            }.deleteQueue(30)
        }
        if (!model.success || model.data?.name == null) {
            return event.reply {
                embed {
                    title("AppID: ${appId}のゲームは見つかりませんでした。")
                    color(Color.Bad)
                    timestamp()
                }
            }.deleteQueue(30)
        }

        event.reply {
            embed {
                title("${model.data!!.name}を所有しているメンバーを検索しています...")
                color(Color.Plain)
                timestamp()
            }
        }.queue {
            val guild = event.jda.getGuildById(event.guild!!.idLong)
            val members = SteamGameRoleSync.profileCache[event.guild.idLong].orEmpty()
            if (members.isEmpty()) {
                return@queue it.edit {
                    embed {
                        title("${model.data!!.name}を所有しているメンバーは見つかりませんでした。")
                        color(Color.Bad)
                        timestamp()
                    }
                }.queue()
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
            }.queue()
        }
    }
}

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
