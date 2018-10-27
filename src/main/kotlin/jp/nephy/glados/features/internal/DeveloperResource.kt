package jp.nephy.glados.features.internal

import jp.nephy.glados.core.*
import jp.nephy.glados.core.builder.edit
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Command
import jp.nephy.glados.core.feature.subscription.CommandEvent
import jp.nephy.glados.core.feature.subscription.Event
import jp.nephy.glados.core.feature.subscription.Priority
import net.dv8tion.jda.core.events.ReadyEvent
import java.util.concurrent.TimeUnit

class DeveloperResource: BotFeature() {
    @Event(priority = Priority.Highest)
    fun checkGuild(event: ReadyEvent) {
        if (event.jda.guilds.isEmpty()) {
            logger.error { "このBotはどのサーバにも所属していません. 終了します。" }
            event.jda.shutdown()
        }
    }

    @Event(priority = Priority.Lowest)
    fun dumpIds(event: ReadyEvent) {
        for (guild in event.jda.guilds) {
            tmpFile("${guild.id}.txt") {
                writeText(
                        buildString {
                            appendln(guild.name)

                            appendln("  ロールID一覧")
                            for (role in guild.roles) {
                                appendln("    ${role.name}")
                                appendln("      ${role.id}")
                            }

                            appendln("\n  テキストチャンネルID一覧")
                            for (textChannel in guild.textChannels) {
                                appendln("    ${textChannel.name}")
                                appendln("      ${textChannel.id}")
                            }

                            appendln("\n  ボイスチャンネルID一覧")
                            for (voiceChannel in guild.voiceChannels) {
                                appendln("    ${voiceChannel.name}")
                                appendln("      ${voiceChannel.id}")
                            }

                            appendln("\n  カテゴリーID一覧")
                            for (category in guild.categories) {
                                appendln("    ${category.name}")
                                appendln("      ${category.id}")
                            }

                            appendln("\n  絵文字ID一覧")
                            for (emote in guild.emotes) {
                                appendln("    ${emote.name}")
                                appendln("      ${emote.id}")
                            }

                            appendln("\n  メンバーID一覧")
                            for (member in guild.members) {
                                appendln("    ${member.fullNameWithoutGuild}")
                                appendln("      ${member.user.id}")
                            }
                        }
                )

                logger.info { "サーバ ${guild.name}のID一覧を ${toPath().toAbsolutePath()} に書き出しました。" }
            }
        }
    }

    @Command(channelType = Command.ChannelType.TextChannel, permission = Command.Permission.AdminOnly, description = "指定されたチャンネルのメッセージをクリーンアップします。", category = "システム")
    fun purge(event: CommandEvent) {
        event.reply {
            message {
                append("クリーンアップしています...")
            }
        }.launch {
            try {
                val messages = event.textChannel?.iterableHistory?.cache(false)?.take(100).orEmpty().filter { it.author.isBotOrSelfUser }
                if (messages.isNotEmpty()) {
                    if (messages.size == 1) {
                        event.textChannel?.deleteMessageById(messages.first().idLong)
                    } else {
                        event.textChannel?.deleteMessages(messages)
                    }?.await()
                }

                it.edit {
                    message {
                        append("${event.textChannel?.asMention} をクリーンアップしました。")
                    }
                }
            } catch (e: Exception) {
                it.edit {
                    message {
                        append("${event.textChannel?.asMention} のクリーンアップに失敗しました。")
                    }
                }
            }.awaitAndDelete(10, TimeUnit.SECONDS)
        }
    }
}
