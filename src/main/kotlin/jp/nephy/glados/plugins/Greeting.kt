package jp.nephy.glados.plugins

import jp.nephy.glados.core.extensions.message
import jp.nephy.glados.core.extensions.withTextChannel
import jp.nephy.glados.core.plugins.Plugin
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent

object Greeting: Plugin(description = "新規メンバーがサーバに参加した場合, ウェルカムメッセージを投稿します。") {
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
