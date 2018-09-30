package jp.nephy.glados.features

import jp.nephy.glados.core.builder.message
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Listener
import jp.nephy.glados.core.feature.textChannelOf
import jp.nephy.glados.core.feature.textChannelsLazy
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent

class Greeting: BotFeature() {
    private val defaultTextChannels by textChannelsLazy("default")
    private val rulesTextChannels by textChannelsLazy("rules")

    @Listener
    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        defaultTextChannels.textChannelOf(event.guild) { defaultChannel ->
            rulesTextChannels.textChannelOf(event.guild) { rulesChannel ->
                defaultChannel.message {
                    embed {
                        title("${event.guild.name} にようこそ！")
                        description { "${event.member.asMention} さんようこそ。\n参加に際しては ${rulesChannel.asMention} の内容を確認してください。" }
                        footer("#${defaultChannel.name} ${event.guild.name}", event.guild.iconUrl)
                        timestamp()
                    }
                }.queue()
            }
        }
    }
}
