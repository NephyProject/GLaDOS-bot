package jp.nephy.glados.feature.listener.kaigen

import com.lukaspradel.steamapi.data.json.ownedgames.GetOwnedGames
import com.lukaspradel.steamapi.webapi.request.builders.SteamWebApiRequestFactory
import jp.nephy.glados.component.helper.hasRole
import jp.nephy.glados.component.helper.profile.UserProfile
import jp.nephy.glados.feature.ListenerFeature
import jp.nephy.glados.logger
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.ReadyEvent
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


class SteamGameRole: ListenerFeature() {
    companion object {
        val profileCache = mutableMapOf<Long, MutableMap<Long, UserProfile>>()
    }

    override fun onReady(event: ReadyEvent) {
        thread(name = "Steam Game Role Synchronizer") {
            while (true) {
                try {
                    synchronize(event)
                } catch (e: Exception) {
                    logger.error(e) { "ゲームロールの同期中にエラーが発生しました." }
                }
                TimeUnit.MINUTES.sleep(5)
                profileCache.clear()
            }
        }
    }

    private fun synchronize(event: ReadyEvent) {
        event.jda.guilds.forEach { guild ->
            if (! profileCache.containsKey(guild.idLong)) {
                profileCache[guild.idLong] = mutableMapOf()
            }

            val config = bot.config.getGuildConfig(guild)
            if (config.option.steamGameRoles.isEmpty() || config.option.clientToken == null) {
                return@forEach
            }

            guild.members.forEach memberLoop@{ member ->
                if (member.user.isBot) {
                    return@memberLoop
                }

                val profile = profileCache[guild.idLong]!!.getOrPut(member.user.idLong) {
                    helper.getUserProfile(member) ?: return@memberLoop
                }
                val steam = profile.connectedAccounts.find { it.type == "steam" } ?: return@memberLoop

                val request = SteamWebApiRequestFactory.createGetOwnedGamesRequest(steam.id, true, true, config.option.steamGameRoles.map { it.appId })
                val response = bot.apiClient.steam.processRequest<GetOwnedGames>(request).response
                for (game in response.games) {
                    val gameConfig = config.option.steamGameRoles.find { it.appId == game.appid } ?: continue

                    if (! guild.selfMember.hasPermission(Permission.MANAGE_ROLES)) {
                        logger.warn { "サーバ: ${guild.name} でロールの管理権限がありません." }
                        continue
                    }

                    val role = guild.getRolesByName(gameConfig.name, false).orEmpty().firstOrNull()
                    if (role == null) {
                        val create = guild.controller.createRole()
                                .setName(gameConfig.name)
                                .setMentionable(true)
                                .setHoisted(true)
                        if (gameConfig.colorHex != null) {
                            create.setColor(gameConfig.colorHex.toInt(16))
                        }

                        create.queue {
                            guild.controller.addSingleRoleToMember(member, it).queue()
                        }
                    } else if (! member.hasRole(role.idLong)) {
                        guild.controller.addSingleRoleToMember(member, role).queue()
                    }
                }
            }
        }
    }
}
