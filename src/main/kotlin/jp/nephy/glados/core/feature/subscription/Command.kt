package jp.nephy.glados.core.feature.subscription

import jp.nephy.glados.config
import jp.nephy.glados.core.*
import jp.nephy.glados.core.audio.music.player
import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.deleteQueue
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.jda
import jp.nephy.utils.stackTraceString
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.GenericMessageEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.MessageUpdateEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KFunction

private val logger = Logger("GLaDOS.Command")
private val spaceRegex = "\\s+".toRegex()

data class CommandSubscription(
        override val annotation: Command,
        override val instance: BotFeature,
        override val function: KFunction<*>,
        override val targetGuilds: List<GLaDOSConfig.GuildConfig>
): GuildSpecificSubscription<Command> {

    private val prefix: String
        get() = if (annotation.prefix.isNotBlank()) {
            annotation.prefix
        } else {
            config.prefix
        }

    val name: String
        get() = if (annotation.command.isNotBlank()) {
            annotation.command
        } else {
            function.name
        }
    private val names: List<String>
        get() = listOf(name) + annotation.aliases
    private val commandSyntaxes: List<String>
        get() = names.map { "$prefix$it" }
    val primaryCommandSyntax: String
        get() = commandSyntaxes.first()

    val category: String?
        get() = if (annotation.category.isNotBlank()) {
            annotation.category
        } else {
            null
        }

    fun satisfyTargetGuildRequirement(guild: Guild?): Boolean {
        return targetGuilds.isEmpty() || (guild != null && targetGuilds.any { it.id == guild.idLong })
    }

    fun satisfyChannelTypeRequirement(type: ChannelType): Boolean {
        return type in annotation.channelType.correspondings
    }

    fun parseArgs(text: String): String? {
        return when (annotation.case) {
            CommandCasePolicy.Strict -> {
                commandSyntaxes.asSequence().filter {
                    text.split(spaceRegex).first() == it
                }.sortedByDescending { it.length }.map {
                    text.removePrefix(it).trim()
                }.firstOrNull()
            }
            CommandCasePolicy.Ignore -> {
                commandSyntaxes.asSequence().filter {
                    text.split(spaceRegex).first().equals(it, true)
                }.sortedByDescending { it.length }.map {
                    "^$it".toRegex(RegexOption.IGNORE_CASE).replace(text, "")
                }.firstOrNull()
            }
        }?.trim()
    }

    fun satisfyArgumentsRequirement(args: List<String>): Boolean {
        return !annotation.checkArgsCount || annotation.args.size == args.size
    }

    fun satisfyCommandAvailabilityForGuildRequirement(guild: Guild?): Boolean {
        return guild == null || config.forGuild(guild)?.boolOption("enable_command") == true
    }

    fun satisfyCommandConditionOfWhileInAnyVoiceChannel(voiceState: GuildVoiceState?): Boolean {
        return annotation.condition != CommandCondition.WhileInAnyVoiceChannel || voiceState?.inVoiceChannel() == true
    }

    fun satisfyCommandConditionOfWhileInSameVoiceChannel(member: Member?): Boolean {
        return annotation.condition != CommandCondition.WhileInSameVoiceChannel || member?.guild?.player?.currentVoiceChannel == member?.voiceState?.channel
    }

    fun satisfyCommandPermissionOfAdminOnly(event: Event): Boolean {
        val isAdmin = when (event) {
            is MessageReceivedEvent -> {
                event.member?.isAdmin()
            }
            is MessageUpdateEvent -> {
                event.member?.isAdmin()
            }
            else -> null
        } ?: false
        return annotation.permission != CommandPermission.AdminOnly || isAdmin
    }

    fun satisfyCommandPermissionOfOwnerOnly(event: Event): Boolean {
        val isGLaDOSOwner = when (event) {
            is MessageReceivedEvent -> {
                event.author.isGLaDOSOwner()
            }
            is MessageUpdateEvent -> {
                event.author.isGLaDOSOwner()
            }
            else -> null
        } ?: false
        return annotation.permission != CommandPermission.OwnerOnly || isGLaDOSOwner
    }
}

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
        val args: Array<String> = [],
        val checkArgsCount: Boolean = true,
        val prefix: String = "",
        val category: String = ""
)

// TODO: nest
enum class CommandPermission {

    Anyone, AdminOnly, OwnerOnly
}

enum class CommandChannelType(vararg val correspondings: ChannelType) {
    Any(ChannelType.TEXT, ChannelType.PRIVATE),
    TextChannel(ChannelType.TEXT),
    PrivateMessage(ChannelType.PRIVATE)
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
        if (event.author.isBotOrSelfUser) {
            return
        }

        launch {
            handleMessage(event, event.message, event.channelType)
        }
    }

    override fun onMessageUpdate(event: MessageUpdateEvent) {
        if (event.author.isBotOrSelfUser) {
            return
        }

        launch {
            handleMessage(event, event.message, event.channelType)
        }
    }

    private suspend fun handleMessage(event: GenericMessageEvent, message: Message, channelType: ChannelType) {
        val text = message.contentDisplay.trim()

        for (subscription in subscriptions) {
            subscription as CommandSubscription

            val args = subscription.parseArgs(text) ?: continue
            val commandEvent = when (event) {
                is MessageReceivedEvent -> CommandEvent(args, event)
                is MessageUpdateEvent -> CommandEvent(args, event)
                else -> throw UnsupportedOperationException("Unknown event: ${event.javaClass.canonicalName}.")
            }

            when {
                !subscription.satisfyTargetGuildRequirement(event.guild) -> {
                    logger.trace { "\"$text\": サーバが対象外のため実行されませんでした. (${commandEvent.authorName})" }
                }
                !subscription.satisfyChannelTypeRequirement(channelType) -> {
                    logger.trace { "\"$text\": チャンネルタイプが対象外のため実行されませんでした. (${commandEvent.authorName})" }
                }
                !subscription.satisfyCommandAvailabilityForGuildRequirement(event.guild) -> {
                    commandEvent.reply {
                        embed {
                            title("コマンドエラー: `$text`")
                            description { "サーバ ${event.guild.name} ではGLaDOSのコマンド機能は利用できません。サーバ管理者またはGLaDOS開発者にご連絡ください。" }
                            color(Color.Bad)
                            timestamp()
                        }
                    }.deleteQueue(30)
                    logger.warn { "\"$text\": サーバ ${event.guild.name} ではコマンド機能が無効なので実行されませんでした. (${commandEvent.authorName})" }
                }
                !subscription.satisfyArgumentsRequirement(commandEvent.argList) -> {
                    commandEvent.reply {
                        embed {
                            title("コマンドエラー: `$text`")
                            descriptionBuilder {
                                appendln("コマンドの引数の数が一致しません。`!help`コマンドも必要に応じてご確認ください。")
                                append("実行例: `${subscription.primaryCommandSyntax} ${subscription.annotation.args.joinToString(" ") { "<$it>" }}`")
                            }
                            color(Color.Bad)
                            timestamp()
                        }
                    }.deleteQueue(30)
                    logger.warn { "\"$text\": コマンドの引数が足りません. (${commandEvent.authorName})" }
                }
                !subscription.satisfyCommandConditionOfWhileInAnyVoiceChannel(message.member?.voiceState) -> {
                    commandEvent.reply {
                        embed {
                            title("コマンドエラー: `$text`")
                            description { "このコマンドはボイスチャンネルに参加中のみ実行できます。" }
                            color(Color.Bad)
                            timestamp()
                        }
                    }.deleteQueue(30)
                    logger.warn { "\"$text\": コマンド実行の要件(WhileInAnyVoiceChannel)が足りません. (${commandEvent.authorName})" }
                }
                !subscription.satisfyCommandConditionOfWhileInSameVoiceChannel(message.member) -> {
                    commandEvent.reply {
                        embed {
                            title("コマンドエラー: `$text`")
                            description { "このコマンドはGLaDOSと同じボイスチャンネルに参加中のみ実行できます。" }
                            color(Color.Bad)
                            timestamp()
                        }
                    }.deleteQueue(30)
                    logger.warn { "\"$text\": コマンド実行の要件(WhileInSameVoiceChannel)が足りません. (${commandEvent.authorName})" }
                }
                !subscription.satisfyCommandPermissionOfAdminOnly(event) -> {
                    if (event.channelType == ChannelType.TEXT) {
                        commandEvent.reply {
                            embed {
                                title("コマンドエラー: `$text`")
                                description { "このコマンドは`${event.guild.name}`の管理者ロールが付与されているメンバーのみが実行できます。判定に問題がある場合はサーバのオーナーにご連絡ください。" }
                                color(Color.Bad)
                                timestamp()
                            }
                        }.deleteQueue(30)
                        logger.warn { "\"$text\": 管理者ロールがないため実行されませんでした. (${commandEvent.authorName})" }
                    } else {
                        commandEvent.reply {
                            embed {
                                title("コマンドエラー: `$text`")
                                description { "このコマンドは管理者ロールが必要であるため, DMでは実行できません。" }
                                color(Color.Bad)
                                timestamp()
                            }
                        }.deleteQueue(30)
                        logger.warn { "\"$text\": 管理者ロールがないため(DM)実行されませんでした. (${commandEvent.authorName})" }
                    }
                }
                !subscription.satisfyCommandPermissionOfOwnerOnly(event) -> {
                    commandEvent.reply {
                        embed {
                            title("コマンドエラー: `$text`")
                            description { "このコマンドはGLaDOSのオーナーのみが実行できます。" }
                            color(Color.Bad)
                            timestamp()
                        }
                    }.deleteQueue(30)
                    logger.warn { "\"$text\": オーナーではないため実行されませんでした. (${commandEvent.authorName})" }
                }
                else -> {
                    try {
                        subscription.invoke(commandEvent)
                        logger.trace { "${subscription.instance.javaClass.simpleName}#${subscription.function.name} が実行されました. (${commandEvent.authorName})" }
                    } catch (e: Exception) {
                        val exception = e.invocationException

                        commandEvent.reply {
                            embed {
                                title("`$text` の実行中に例外が発生しました。")
                                description { "ご不便をお掛けしています。この問題が何度も発生する場合は開発者にご連絡ください。" }
                                field("スタックトレース") { "${exception.stackTraceString.take(300)}..." }
                                color(Color.Bad)
                                timestamp()
                            }
                        }.deleteQueue(30)

                        logger.error(exception) { "\"$text\" の実行中に例外が発生しました." }
                    }
                }
            }
        }
    }
}

class CommandEvent private constructor(val args: String, val message: Message, val user: User, val member: Member?, val guild: Guild?, val textChannel: TextChannel?, val privateChannel: PrivateChannel?, val channel: MessageChannel): Event(jda, jda.responseTotal) {
    constructor(args: String, event: MessageReceivedEvent): this(args, event.message, event.author, event.member, event.guild, event.textChannel, event.privateChannel, event.channel)
    constructor(args: String, event: MessageUpdateEvent): this(args, event.message, event.author, event.member, event.guild, event.textChannel, event.privateChannel, event.channel)

    val argList: List<String>
        get() = if (args.isNotBlank()) {
            args.split(spaceRegex)
        } else {
            emptyList()
        }
    val authorName: String
        get() = member?.fullName ?: user.displayName
}
