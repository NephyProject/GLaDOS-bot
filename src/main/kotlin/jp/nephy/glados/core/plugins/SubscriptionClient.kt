@file:Suppress("UNUSED")

package jp.nephy.glados.core.plugins

import com.sedmelluq.discord.lavaplayer.player.event.*
import jp.nephy.glados.config
import jp.nephy.glados.core.Logger
import jp.nephy.glados.core.audio.player.GuildPlayer
import jp.nephy.glados.core.audio.player.player
import jp.nephy.glados.core.extensions.*
import jp.nephy.glados.core.extensions.messages.HexColor
import jp.nephy.glados.dispatcher
import kotlinx.coroutines.*
import net.dv8tion.jda.core.audio.SpeakingMode
import net.dv8tion.jda.core.audio.hooks.ConnectionListener
import net.dv8tion.jda.core.audio.hooks.ConnectionStatus
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.message.GenericMessageEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.MessageUpdateEvent
import net.dv8tion.jda.core.hooks.SubscribeEvent
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

object SubscriptionClient {
    abstract class Client<A: Annotation, S: Subscription.Element<A>>: CoroutineScope {
        val logger = Logger("GLaDOS.SubscriptionClient.${javaClass.simpleName}")
        val subscriptions = CopyOnWriteArrayList<S>()

        private val job = Job()
        final override val coroutineContext = dispatcher + job

        fun sort() {
            subscriptions.sortBy { it.priority }
        }
    }

    object ListenerEvent: Client<Plugin.Event, Subscription.Event>(), EventListener {
        @SubscribeEvent fun onGenericEvent(event: Event) {
            launch {
                val targets = subscriptions.filter { it.matchParameters(event) }
                if (targets.isEmpty()) {
                    return@launch
                }

                val processTimeMillis = measureTimeMillis {
                    val guild = try {
                        event.javaClass.getMethod("getGuild").invoke(event) as? Guild
                    } catch (e: NoSuchMethodException) {
                        null
                    }

                    targets.forEach {
                        launch {
                            it.invoke(guild, event)
                        }
                    }
                }

                logger.trace { "${event.javaClass.canonicalName}: $processTimeMillis ms で起動しました。" }
            }
        }
    }

    object Command: Client<Plugin.Command, Subscription.Command>(), EventListener {
        @SubscribeEvent fun onMessageReceived(event: MessageReceivedEvent) {
            if (event.author.isBotOrSelfUser) {
                return
            }

            launch {
                handleMessage(event, event.message, event.channelType)
            }
        }

        @SubscribeEvent fun onMessageUpdate(event: MessageUpdateEvent) {
            if (event.author.isBotOrSelfUser) {
                return
            }

            launch {
                handleMessage(event, event.message, event.channelType)
            }
        }

        private fun handleMessage(event: GenericMessageEvent, message: Message, channelType: ChannelType) {
            val text = message.contentDisplay.trim()

            for (subscription in subscriptions) {
                val args = subscription.parseArgs(text) ?: continue
                val commandEvent = when (event) {
                    is MessageReceivedEvent -> Plugin.Command.Event(args, subscription, event)
                    is MessageUpdateEvent -> Plugin.Command.Event(args, subscription, event)
                    else -> throw UnsupportedOperationException("Unknown event: ${event.javaClass.canonicalName}.")
                }

                when {
                    !subscription.satisfyChannelTypeRequirement(channelType) -> {
                        logger.trace { "\"$text\": チャンネルタイプが対象外のため実行されませんでした。 (${commandEvent.authorName})" }
                    }
                    !satisfyCommandAvailabilityForGuildRequirement(event.guild) -> {
                        commandEvent.reply {
                            embed {
                                title("コマンドエラー: `$text`")
                                description { "サーバ ${event.guild.name} ではGLaDOSのコマンド機能は利用できません。サーバ管理者またはGLaDOS開発者にご連絡ください。" }
                                color(HexColor.Bad)
                                timestamp()
                            }
                        }.launchAndDelete(30, TimeUnit.SECONDS)

                        logger.warn { "\"$text\": サーバ ${event.guild.name} ではコマンド機能が無効なので実行されませんでした。 (${commandEvent.authorName})" }
                    }
                    !subscription.satisfyArgumentsRequirement(commandEvent.argList) -> {
                        commandEvent.reply {
                            embed {
                                title("コマンドエラー: `$text`")
                                descriptionBuilder {
                                    appendln("コマンドの引数の数が一致しません。`!help`コマンドも必要に応じてご確認ください。")
                                    append("実行例: `${subscription.primaryCommandSyntax} ${subscription.arguments.joinToString(" ") { "<$it>" }}`")
                                }
                                color(HexColor.Bad)
                                timestamp()
                            }
                        }.launchAndDelete(30, TimeUnit.SECONDS)

                        logger.warn { "\"$text\": コマンドの引数が足りません。 (${commandEvent.authorName})" }
                    }
                    !subscription.satisfyCommandConditionOfWhileInAnyVoiceChannel(message.member?.voiceState) -> {
                        commandEvent.reply {
                            embed {
                                title("コマンドエラー: `$text`")
                                description { "このコマンドはボイスチャンネルに参加中のみ実行できます。" }
                                color(HexColor.Bad)
                                timestamp()
                            }
                        }.launchAndDelete(30, TimeUnit.SECONDS)

                        logger.warn { "\"$text\": コマンド実行の要件(WhileInAnyVoiceChannel)が足りません。 (${commandEvent.authorName})" }
                    }
                    !subscription.satisfyCommandConditionOfWhileInSameVoiceChannel(message.member) -> {
                        commandEvent.reply {
                            embed {
                                title("コマンドエラー: `$text`")
                                description { "このコマンドはGLaDOSと同じボイスチャンネルに参加中のみ実行できます。" }
                                color(HexColor.Bad)
                                timestamp()
                            }
                        }.launchAndDelete(30, TimeUnit.SECONDS)

                        logger.warn { "\"$text\": コマンド実行の要件(WhileInSameVoiceChannel)が足りません。 (${commandEvent.authorName})" }
                    }
                    !subscription.satisfyCommandPermissionOfAdminOnly(event) -> {
                        if (event.channelType == ChannelType.TEXT) {
                            commandEvent.reply {
                                embed {
                                    title("コマンドエラー: `$text`")
                                    description { "このコマンドは`${event.guild.name}`の管理者ロールが付与されているメンバーのみが実行できます。判定に問題がある場合はサーバのオーナーにご連絡ください。" }
                                    color(HexColor.Bad)
                                    timestamp()
                                }
                            }.launchAndDelete(30, TimeUnit.SECONDS)

                            logger.warn { "\"$text\": 管理者ロールがないため実行されませんでした。 (${commandEvent.authorName})" }
                        } else {
                            commandEvent.reply {
                                embed {
                                    title("コマンドエラー: `$text`")
                                    description { "このコマンドは管理者ロールが必要であるため, DMでは実行できません。" }
                                    color(HexColor.Bad)
                                    timestamp()
                                }
                            }.launchAndDelete(30, TimeUnit.SECONDS)

                            logger.warn { "\"$text\": 管理者ロールがないため(DM)実行されませんでした。 (${commandEvent.authorName})" }
                        }
                    }
                    !subscription.satisfyCommandPermissionOfOwnerOnly(event) -> {
                        commandEvent.reply {
                            embed {
                                title("コマンドエラー: `$text`")
                                description { "このコマンドはGLaDOSのオーナーのみが実行できます。" }
                                color(HexColor.Bad)
                                timestamp()
                            }
                        }.launchAndDelete(30, TimeUnit.SECONDS)

                        logger.warn { "\"$text\": オーナーではないため実行されませんでした。 (${commandEvent.authorName})" }
                    }
                    else -> {
                        launch {
                            subscription.invoke(event.guild, commandEvent)
                        }
                    }
                }
            }
        }

        private fun Subscription.Command.satisfyChannelTypeRequirement(type: ChannelType): Boolean {
            return type in targetChannelType.types
        }

        private fun Subscription.Command.parseArgs(text: String): String? {
            return when (casePolicy) {
                Plugin.Command.CasePolicy.Strict -> {
                    commandSyntaxes.asSequence().filter {
                        text.split(spaceRegex).first() == it
                    }.sortedByDescending { it.length }.map {
                        text.removePrefix(it).trim()
                    }.firstOrNull()
                }
                Plugin.Command.CasePolicy.Ignore -> {
                    commandSyntaxes.asSequence().filter {
                        text.split(spaceRegex).first().equals(it, true)
                    }.sortedByDescending { it.length }.map {
                        "^$it".toRegex(RegexOption.IGNORE_CASE).replace(text, "")
                    }.firstOrNull()
                }
            }?.trim()
        }

        private fun Subscription.Command.satisfyArgumentsRequirement(args: List<String>): Boolean {
            return !shouldCheckArgumentsCount || arguments.size == args.size
        }

        private fun satisfyCommandAvailabilityForGuildRequirement(guild: Guild?): Boolean {
            return guild == null || config.forGuild(guild)?.boolOption("enable_command") == true
        }

        private fun Subscription.Command.satisfyCommandConditionOfWhileInAnyVoiceChannel(voiceState: GuildVoiceState?): Boolean {
            return conditionPolicy != Plugin.Command.ConditionPolicy.WhileInAnyVoiceChannel || voiceState?.inVoiceChannel() == true
        }

        private fun Subscription.Command.satisfyCommandConditionOfWhileInSameVoiceChannel(member: Member?): Boolean {
            return conditionPolicy != Plugin.Command.ConditionPolicy.WhileInSameVoiceChannel || member?.guild?.player?.currentVoiceChannel == member?.voiceState?.channel
        }

        private fun Subscription.Command.satisfyCommandPermissionOfAdminOnly(event: net.dv8tion.jda.core.events.Event): Boolean {
            val isAdmin = when (event) {
                is MessageReceivedEvent -> {
                    event.member?.isAdmin()
                }
                is MessageUpdateEvent -> {
                    event.member?.isAdmin()
                }
                else -> null
            } ?: false
            return permissionPolicy != Plugin.Command.PermissionPolicy.AdminOnly || isAdmin
        }

        private fun Subscription.Command.satisfyCommandPermissionOfOwnerOnly(event: net.dv8tion.jda.core.events.Event): Boolean {
            val isGLaDOSOwner = when (event) {
                is MessageReceivedEvent -> {
                    event.author.isGLaDOSOwner()
                }
                is MessageUpdateEvent -> {
                    event.author.isGLaDOSOwner()
                }
                else -> false
            }
            return permissionPolicy != Plugin.Command.PermissionPolicy.OwnerOnly || isGLaDOSOwner
        }
    }

    object Loop: Client<Plugin.Loop, Subscription.Loop>(), EventListener {
        @SubscribeEvent fun onReady(event: ReadyEvent) {
            launch {
                for (it in subscriptions) {
                    launch {
                        while (isActive) {
                            try {
                                it.invoke(null)
                            } catch (e: CancellationException) {
                                break
                            }

                            try {
                                delay(it.intervalMillis)
                            } catch (e: CancellationException) {
                                break
                            }
                        }

                        it.logger.info { "終了しました。 ($event)" }
                    }

                    it.logger.info { "開始されました。 ($event)" }
                }
            }
        }
    }

    object Schedule: Client<Plugin.Schedule, Subscription.Schedule>(), EventListener {
        @SubscribeEvent fun onReady(event: ReadyEvent) {
            launch {
                while (isActive) {
                    try {
                        val calendar = Calendar.getInstance()
                        delay(60000L - calendar.get(Calendar.SECOND) * 1000L - calendar.get(Calendar.MILLISECOND))

                        val newCalendar = Calendar.getInstance()
                        subscriptions.filter {
                            it.matches(newCalendar)
                        }.forEach {
                            launch {
                                it.invoke(null)
                            }
                        }

                        delay(1000)
                    } catch (e: CancellationException) {
                        break
                    }
                }

                logger.info { "ScheduleSubscriptionClient が終了しました。 ($event)" }
            }

            logger.info { "ScheduleSubscriptionClient が開始されました。 ($event)" }
        }
    }

    class AudioEvent private constructor(private val guildPlayer: GuildPlayer): Client<Plugin.Event, Subscription.Event>(), AudioEventListener {
        companion object {
            val subscriptions = CopyOnWriteArrayList<Subscription.Event>()

            fun create(guildPlayer: GuildPlayer): AudioEvent {
                return SubscriptionClient.AudioEvent(guildPlayer).also {
                    it.subscriptions += AudioEvent.subscriptions
                    it.sort()
                }
            }
        }

        private fun runEvent(vararg args: Any) {
            subscriptions.filter {
                it.matchParameters(*args)
            }.forEach {
                launch {
                    it.invoke(guildPlayer.guild, *args)
                }
            }
        }

        override fun onEvent(event: com.sedmelluq.discord.lavaplayer.player.event.AudioEvent) {
            when (event) {
                is PlayerPauseEvent -> runEvent(guildPlayer, event.player)
                is PlayerResumeEvent -> runEvent(guildPlayer, event.player)
                is TrackStartEvent -> runEvent(guildPlayer, event.player, event.track)
                is TrackEndEvent -> runEvent(guildPlayer, event.player, event.track, event.endReason)
                is TrackExceptionEvent -> runEvent(guildPlayer, event.player, event.track, event.exception)
                is TrackStuckEvent -> runEvent(guildPlayer, event.player, event.track, event.thresholdMs)
            }
            runEvent(guildPlayer, event)
        }
    }

    class ConnectionEvent private constructor(val guild: Guild): Client<Plugin.Event, Subscription.Event>(), ConnectionListener {
        companion object {
            val subscriptions = CopyOnWriteArrayList<Subscription.Event>()

            fun create(guild: Guild): ConnectionEvent {
                return SubscriptionClient.ConnectionEvent(guild).also {
                    it.subscriptions += ConnectionEvent.subscriptions
                    it.sort()
                }
            }
        }

        private fun runEvent(vararg args: Any) {
            subscriptions.filter {
                it.matchParameters(*args)
            }.forEach {
                launch {
                    it.invoke(guild, *args)
                }
            }
        }

        override fun onPing(ping: Long) {
            runEvent(guild, ping)
        }

        override fun onStatusChange(status: ConnectionStatus) {
            runEvent(guild, status)
        }

        override fun onUserSpeaking(user: User, speaking: Boolean) {
            runEvent(guild, user, speaking)
        }

        override fun onUserSpeaking(user: User, modes: EnumSet<SpeakingMode>) {
            runEvent(guild, user, modes)
        }

        override fun onUserSpeaking(user: User, speaking: Boolean, soundshare: Boolean) {
            runEvent(guild, user, speaking, soundshare)
        }
    }
}
