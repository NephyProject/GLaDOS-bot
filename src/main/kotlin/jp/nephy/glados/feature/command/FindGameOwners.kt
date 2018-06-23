package jp.nephy.glados.feature.command

import com.jagrosh.jdautilities.command.CommandEvent
import com.lukaspradel.steamapi.data.json.ownedgames.GetOwnedGames
import com.lukaspradel.steamapi.webapi.request.builders.SteamWebApiRequestFactory
import jp.nephy.glados.component.helper.Color
import jp.nephy.glados.component.helper.EmbedBuilder
import jp.nephy.glados.feature.CommandFeature
import jp.nephy.glados.feature.listener.kaigen.SteamGameRole
import jp.nephy.glados.model.SteamAppDetails
import jp.nephy.jsonkt.JsonKt
import jp.nephy.jsonkt.toJsonObject
import jp.nephy.jsonkt.values
import okhttp3.OkHttpClient
import okhttp3.Request

class FindGameOwners: CommandFeature() {
    init {
        name = "find"
        help = "指定されたSteamゲームを所有しているメンバーにメンションを飛ばします。"
        arguments = "<App ID>"
        guildOnly = true
    }

    private val httpClient = OkHttpClient()

    override fun executeCommand(event: CommandEvent) {
        val appId = event.args.toIntOrNull() ?: return event.reply(
                EmbedBuilder.build {
                    title("AppIDは数値のみ入力可能です。")
                    color(Color.Bad)
                    timestamp()
                }
        )

        val model = try {
            val content = httpClient.newCall(Request.Builder().url("https://store.steampowered.com/api/appdetails/?appids=${appId}").build()).execute().body()?.string().orEmpty()
            val json = JsonKt.toJsonObject(content)
            JsonKt.parse<SteamAppDetails>(json.values.first().toJsonObject())
        } catch (e: Exception) {
            return event.reply(
                    EmbedBuilder.build {
                        title("AppID: ${appId}のゲームの検索に失敗しました。")
                        color(Color.Bad)
                        timestamp()
                    }
            )
        }
        if (! model.success || model.data?.name == null) {
            return event.reply(
                    EmbedBuilder.build {
                        title("AppID: ${appId}のゲームは見つかりませんでした。")
                        color(Color.Bad)
                        timestamp()
                    }
            )
        }

        event.reply(
                EmbedBuilder.build {
                    title("${model.data!!.name}を所有しているメンバーを検索しています...")
                    color(Color.Plain)
                    timestamp()
                }
        ) {
            val guild = event.jda.getGuildById(event.guild.idLong)
            val members = SteamGameRole.profileCache[event.guild.idLong].orEmpty()
            if (members.isEmpty()) {
                return@reply it.editMessage(EmbedBuilder.build {
                    title("${model.data!!.name}を所有しているメンバーは見つかりませんでした。")
                    color(Color.Bad)
                    timestamp()
                }).queue()
            }

            val players = members.mapNotNull {
                val (id, profile) = it
                val steam = profile.connectedAccounts.find { it.type == "steam" } ?: return@mapNotNull null
                val request = SteamWebApiRequestFactory.createGetOwnedGamesRequest(steam.id, true, true, listOf(appId))
                val response = bot.apiClient.steam.processRequest<GetOwnedGames>(request).response
                if (response.games.count { it.appid == appId } == 0) {
                    return@mapNotNull null
                }

                val member = guild.getMemberById(id) ?: return@mapNotNull null
                member.asMention
            }

            it.editMessage(EmbedBuilder.build {
                title("${model.data!!.name}を所有しているメンバーは${players.size}人見つかりました。")
                description { players.joinToString(" ") }
                color(Color.Good)
                timestamp()
            }).queue()
        }
    }
}
