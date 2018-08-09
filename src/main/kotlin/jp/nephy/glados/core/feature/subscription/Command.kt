package jp.nephy.glados.core.feature.subscription

import jp.nephy.glados.config
import jp.nephy.glados.core.GLaDOSConfig
import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.deleteQueue
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.isAdmin
import jp.nephy.glados.core.isGLaDOSOwner
import jp.nephy.glados.jda
import jp.nephy.glados.logger
import jp.nephy.glados.player
import jp.nephy.utils.stackTraceString
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.GenericMessageEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.MessageUpdateEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.lang.reflect.Method
import java.util.concurrent.CopyOnWriteArrayList

data class CommandSubscription(
        override val annotation: Command,
        override val instance: BotFeature,
        override val method: Method,
        override val targetGuilds: List<GLaDOSConfig.GuildConfig>
): GuildSpecificSubscription<Command>

@Target(AnnotationTarget.FUNCTION)
annotation class Command(
        val command: String = "",
        val aliases: Array<String> = [],
        val guilds: Array<String> = [],
        val priority: Priority = Priority.Normal,
        val permission: CommandPermission = CommandPermission.Anyone,
        val channelType: CommandChannelType = CommandChannelType.Any,
        val case: CommandCasePolicy = CommandCasePolicy.Strict,
        val condition: CommandCondition = CommandCondition.Anytime,
        val description: String = "",
        val args: String = "",
        val prefix: String = ""
)

enum class CommandPermission {
    Anyone, AdminOnly, OwnerOnly
}

enum class CommandChannelType(val correspondings: Array<ChannelType>) {
    Any(arrayOf(ChannelType.TEXT, ChannelType.PRIVATE)),
    TextChannel(arrayOf(ChannelType.TEXT)),
    PrivateMessage(arrayOf(ChannelType.PRIVATE))
}

enum class CommandCasePolicy {
    Strict, Ignore
}

enum class CommandCondition {
    Anytime, WhileInAnyVoiceChannel, WhileInSameVoiceChannel
}

class CommandSubscriptionClient: SubscriptionClient<Command>, ListenerAdapter() {
    override val subscriptions = CopyOnWriteArrayList<GuildSpecificSubscription<Command>>()

    override fun onReady() {
        subscriptions.sortBy { it.annotation.priority }
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        handleMessage(event, event.message, event.channelType)
    }

    override fun onMessageUpdate(event: MessageUpdateEvent) {
        handleMessage(event, event.message, event.channelType)
    }

    private val space = "\\s".toRegex()
    private fun handleMessage(event: GenericMessageEvent, message: Message, channelType: ChannelType) {
        val text = message.contentDisplay
        if (message.guild != null && config.forGuild(message.guild)?.boolOption("enable_command", false) != true) {
            message.reply {
                embed {
                    title("コマンドエラー: $text")
                    description { "サーバ ${event.guild.name} ではGLaDOSのコマンド機能は利用できません。サーバ管理者またはGLaDOS開発者にご連絡ください。" }
                    color(Color.Bad)
                    timestamp()
                }
            }.deleteQueue(30)
            return
        }

        loop@ for (subscription in subscriptions) {
            if (event.guild != null && subscription.targetGuilds.isNotEmpty() && subscription.targetGuilds.all { it.id != event.guild.idLong }) {
                continue@loop
            }
            // チャンネル判別
            if (channelType !in subscription.annotation.channelType.correspondings) {
                continue@loop
            }

            // 引数解析
            val prefix = if (subscription.annotation.prefix.isNotBlank()) {
                subscription.annotation.prefix
            } else {
                config.prefix
            }
            val names = arrayOf(if (subscription.annotation.command.isNotBlank()) {
                subscription.annotation.command
            } else {
                subscription.method.name
            }) + subscription.annotation.aliases
            val commandNames = names.map { "$prefix$it" }
            val primaryCommandName = commandNames.first()
            val args = when (subscription.annotation.case) {
                CommandCasePolicy.Strict -> {
                    commandNames.filter {
                        text.split(space).first() == it
                    }.sortedByDescending { it.length }.map {
                        text.removePrefix(it).trim()
                    }.firstOrNull()
                }
                CommandCasePolicy.Ignore -> {
                    commandNames.filter {
                        text.split(space).first().equals(it, true)
                    }.sortedByDescending { it.length }.map {
                        "^$it".toRegex(RegexOption.IGNORE_CASE).replace(text, "")
                    }.firstOrNull()
                }
            } ?: continue@loop

            val commandEvent = when (event) {
                is MessageUpdateEvent -> CommandEvent(args, event)
                is MessageReceivedEvent -> CommandEvent(args, event)
                else -> throw UnsupportedOperationException("Unknown event: ${event.javaClass.canonicalName}.")
            }

            if (subscription.annotation.args.isNotBlank() && args.isBlank()) {
                commandEvent.reply {
                    embed {
                        title("コマンドエラー: $text")
                        descriptionBuilder {
                            appendln("コマンドの引数が足りません。")
                            append("実行例: `$primaryCommandName ${subscription.annotation.args}`")
                        }
                        color(Color.Bad)
                        timestamp()
                    }
                }.queue()
                return
            }

            when (subscription.annotation.condition) {
                CommandCondition.Anytime -> {
                }
                CommandCondition.WhileInAnyVoiceChannel -> {
                    if (message.member?.voiceState?.inVoiceChannel() != true) {
                        commandEvent.reply {
                            embed {
                                title("`$text` はボイスチャンネルに参加中のみ実行できます。")
                                description { "このコマンドはボイスチャンネルに参加中のみ実行できます。" }
                                color(Color.Bad)
                                timestamp()
                            }
                        }.deleteQueue(30)
                        continue@loop
                    }
                }
                CommandCondition.WhileInSameVoiceChannel -> {
                    if (message.guild?.player?.currentVoiceChannel?.idLong != message.member?.voiceState?.channel?.idLong) {
                        commandEvent.reply {
                            embed {
                                title("`$text` はGLaDOSと同じボイスチャンネルに参加中のみ実行できます。")
                                description { "このコマンドはGLaDOSと同じボイスチャンネルに参加中のみ実行できます。" }
                                color(Color.Bad)
                                timestamp()
                            }
                        }.deleteQueue(30)
                        continue@loop
                    }
                }
            }

            // セキュリティチェック
            when (subscription.annotation.permission) {
                CommandPermission.Anyone -> {
                }
                CommandPermission.OwnerOnly -> {
                    val isGLaDOSOwner = when (event) {
                        is MessageReceivedEvent -> {
                            event.author.isGLaDOSOwner()
                        }
                        is MessageUpdateEvent -> {
                            event.author.isGLaDOSOwner()
                        }
                        else -> throw IllegalArgumentException("Unknown event: ${event.javaClass.canonicalName}.")
                    }
                    if (!isGLaDOSOwner) {
                        commandEvent.reply {
                            embed {
                                title("`$text` はGLaDOSのオーナーのみが実行できます。")
                                description { "このコマンドはGLaDOSのオーナーのみが実行できます。" }
                                color(Color.Bad)
                                timestamp()
                            }
                        }.deleteQueue(30)
                        logger.error { "Command: オーナーではないため \"$text\" は実行されませんでした." }
                        return
                    }
                }
                CommandPermission.AdminOnly -> {
                    val guild = event.guild ?: continue@loop
                    val isAdmin = when (event) {
                        is MessageReceivedEvent -> {
                            event.member?.isAdmin()
                        }
                        is MessageUpdateEvent -> {
                            event.member?.isAdmin()
                        }
                        else -> throw IllegalArgumentException("Unknown event: ${event.javaClass.canonicalName}.")
                    } ?: continue@loop

                    if (!isAdmin) {
                        commandEvent.reply {
                            embed {
                                title("`$text` はサーバ管理者のみが実行できます。")
                                description { "このコマンドは`${guild.name}`の管理者ロールが付与されているメンバーのみが実行できます。判定に問題がある場合はサーバのオーナーにご連絡ください。" }
                                color(Color.Bad)
                                timestamp()
                            }
                        }.deleteQueue(30)
                        logger.error { "Command: 管理者ロールがないため \"$text\" は実行されませんでした." }
                        return
                    }
                }
            }

            try {
                subscription.execute(commandEvent)
                return
            } catch (e: Exception) {
                commandEvent.reply {
                    embed {
                        title("`$text` の実行中に例外が発生しました。")
                        description { "ご不便をお掛けしています。この問題が何度も発生する場合は開発者にご連絡ください。" }
                        field("スタックトレース") { "${e.stackTraceString.take(300)}..." }
                        color(Color.Bad)
                        timestamp()
                    }
                }.deleteQueue(30)
                logger.error(e) { "Command: \"$text\" の実行中に例外が発生しました." }
                return
            }
        }
    }
}

class CommandEvent private constructor(val args: String, val message: Message, val user: User, val member: Member?, val guild: Guild?, val textChannel: TextChannel?, val privateChannel: PrivateChannel?, val channel: MessageChannel): Event(jda, jda.responseTotal) {
    constructor(args: String, event: MessageReceivedEvent): this(args, event.message, event.author, event.member, event.guild, event.textChannel, event.privateChannel, event.channel)
    constructor(args: String, event: MessageUpdateEvent): this(args, event.message, event.author, event.member, event.guild, event.textChannel, event.privateChannel, event.channel)
}
