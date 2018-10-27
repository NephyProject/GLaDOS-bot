package jp.nephy.glados.features

import jp.nephy.glados.core.builder.message
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Event
import jp.nephy.glados.core.feature.withTextChannel
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent

class Greeting: BotFeature() {
    @Event
    override suspend fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        event.guild.withTextChannel("default") { defaultChannel ->
            event.guild.withTextChannel("rules") { rulesChannel ->
                defaultChannel.message {
                    embed {
                        title("${event.guild.name} にようこそ！")
                        description { "${event.member.asMention} さんようこそ。\n参加にあたって ${rulesChannel.asMention} を確認してください。" }
                        footer("#${defaultChannel.name} ${event.guild.name}", event.guild.iconUrl)
                        timestamp()
                    }
                }
            }
        }
    }
}
