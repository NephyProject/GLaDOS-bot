package jp.nephy.glados.features

import jp.nephy.glados.config
import jp.nephy.glados.core.builder.deleteQueue
import jp.nephy.glados.core.builder.edit
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.*
import jp.nephy.glados.core.fetchMessages
import jp.nephy.glados.core.fullNameWithoutGuild
import jp.nephy.glados.core.isSelfUser
import jp.nephy.glados.core.tmpFile
import jp.nephy.glados.isDebugMode
import net.dv8tion.jda.core.entities.MessageType
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.utils.MiscUtil

class DeveloperResource: BotFeature() {
    @Listener
    fun checkGuild(event: ReadyEvent) {
        if (event.jda.guilds.isEmpty()) {
            logger.error { "このBotはどのサーバにも所属していません. 終了します." }
            event.jda.shutdown()
        }
    }

    @Listener(priority = Priority.Lowest)
    fun dumpIds(event: ReadyEvent) {
        event.jda.guilds.forEach {
            tmpFile("ids_${it.id}.txt") {
                writeText(
                        buildString {
                            appendln(it.name)

                            appendln("  ロールID一覧")
                            it.roles.forEach {
                                appendln("    ${it.name}")
                                appendln("      ${it.id}")
                            }

                            appendln("\n  テキストチャンネルID一覧")
                            it.textChannels.forEach {
                                appendln("    ${it.name}")
                                appendln("      ${it.id}")
                            }

                            appendln("\n  ボイスチャンネルID一覧")
                            it.voiceChannels.forEach {
                                appendln("    ${it.name}")
                                appendln("      ${it.id}")
                            }

                            appendln("\n  カテゴリーID一覧")
                            it.categories.forEach {
                                appendln("    ${it.name}")
                                appendln("      ${it.id}")
                            }

                            appendln("\n  絵文字ID一覧")
                            it.emotes.forEach {
                                appendln("    ${it.name}")
                                appendln("      ${it.id}")
                            }

                            appendln("\n  メンバーID一覧")
                            it.members.forEach {
                                appendln("    ${it.fullNameWithoutGuild}")
                                appendln("      ${it.user.id}")
                            }
                        }
                )

                logger.info { "サーバ ${it.name}のID一覧を ${toPath().toAbsolutePath()} に書き出しました." }
            }
        }
    }

    @Listener(priority = Priority.Lowest)
    fun purgeMessages(event: ReadyEvent) {
        if (isDebugMode) {
            logger.info { "メッセージのクリーンアップを開始します." }

            event.jda.guilds.forEach {
                val config = config.forGuild(it)
                val channel = config?.textChannel("bot") ?: return@forEach

                if (channel.hasLatestMessage()) {
                    val twoWeeksAgo = (System.currentTimeMillis() - 14 * 24 * 60 * 60 * 1000 - MiscUtil.DISCORD_EPOCH) shl MiscUtil.TIMESTAMP_OFFSET.toInt()

                    val messages = channel.fetchMessages(100).filter { it.author.isSelfUser && it.type != MessageType.GUILD_MEMBER_JOIN && MiscUtil.parseSnowflake(it.id) <= twoWeeksAgo }

                    if (messages.isEmpty()) {
                        logger.debug { "テキストチャンネル: #${channel.name}(${channel.guild.name}) は空でした." }
                    } else {
                        if (messages.size == 1) {
                            channel.deleteMessageById(messages.first().idLong)
                        } else {
                            channel.deleteMessages(messages)
                        }.queue {
                            logger.debug { "テキストチャンネル: #${channel.name}(${channel.guild.name}) のクリーンアップが完了しました." }
                        }
                    }
                }
            }
        }
    }

    @Command(channelType = CommandChannelType.TextChannel, permission = CommandPermission.AdminOnly, description = "指定されたチャンネルのメッセージをクリーンアップします。")
    fun purge(event: CommandEvent) {
        event.reply {
            message {
                append("クリーンアップしています...")
            }
        }.queue {
            try {
                val messages = event.textChannel?.iterableHistory?.cache(false)?.take(100).orEmpty().filter { it.author.isBot }
                if (messages.isNotEmpty()) {
                    if (messages.size == 1) {
                        event.textChannel?.deleteMessageById(messages.first().idLong)
                    } else {
                        event.textChannel?.deleteMessages(messages)
                    }?.queue()
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
            }.deleteQueue(10)
        }
    }
}
