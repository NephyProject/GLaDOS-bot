package jp.nephy.glados.features

import jp.nephy.glados.config
import jp.nephy.glados.core.builder.message
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Listener
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent

class Greeting: BotFeature() {
    @Listener
    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        val guildConfig = config.forGuild(event.guild)
        val defaultChannel = guildConfig?.textChannel("default") ?: return
        val rulesChannel = guildConfig.textChannel("rules") ?: return

        defaultChannel.message {
            embed {
                title("${event.guild.name} にようこそ！")
                description { "${event.member.asMention} さんようこそ。\n参加に際しては ${rulesChannel.asMention} の内容を確認してください。" }
            }
        }.queue()
    }
}
