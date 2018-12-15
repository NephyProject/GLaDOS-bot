@file:Suppress("UNUSED")

package jp.nephy.glados.core.plugins

import com.codahale.metrics.Slf4jReporter
import com.sedmelluq.discord.lavaplayer.player.event.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.metrics.Metrics
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.request.userAgent
import io.ktor.response.respond
import io.ktor.response.respondFile
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.sessions.SessionStorageMemory
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import jp.nephy.glados.GLaDOS
import jp.nephy.glados.core.GuildPlayer
import jp.nephy.glados.core.config.GLaDOSConfig
import jp.nephy.glados.core.config.booleanOption
import jp.nephy.glados.core.config.textChannel
import jp.nephy.glados.core.logger.SlackLogger
import jp.nephy.glados.core.plugins.extensions.config
import jp.nephy.glados.core.plugins.extensions.hasAdminCapability
import jp.nephy.glados.core.plugins.extensions.isGLaDOSOwner
import jp.nephy.glados.core.plugins.extensions.jda.*
import jp.nephy.glados.core.plugins.extensions.jda.messages.HexColor
import jp.nephy.glados.core.plugins.extensions.jda.messages.edit
import jp.nephy.glados.core.plugins.extensions.jda.messages.prompt
import jp.nephy.glados.core.plugins.extensions.jda.messages.prompt.PromptEmoji
import jp.nephy.glados.core.plugins.extensions.jda.messages.reply
import jp.nephy.glados.core.plugins.extensions.resourceFile
import jp.nephy.glados.core.plugins.extensions.web.effectiveHost
import jp.nephy.glados.core.plugins.extensions.web.url
import jp.nephy.jsonkt.*
import jp.nephy.penicillin.core.streaming.UserStreamListener
import jp.nephy.penicillin.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.audio.AudioReceiveHandler
import net.dv8tion.jda.core.audio.CombinedAudio
import net.dv8tion.jda.core.audio.SpeakingMode
import net.dv8tion.jda.core.audio.UserAudio
import net.dv8tion.jda.core.audio.hooks.ConnectionListener
import net.dv8tion.jda.core.audio.hooks.ConnectionStatus
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.message.GenericMessageEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.MessageUpdateEvent
import net.dv8tion.jda.core.hooks.AnnotatedEventManager
import net.dv8tion.jda.core.hooks.SubscribeEvent
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

object SubscriptionClient {
    suspend fun registerClients(builder: JDABuilder) {
        PluginManager.loadAll()

        builder.apply {
            setEventManager(AnnotatedEventManager())

            SubscriptionClient::class.nestedClasses.mapNotNull { it.objectInstance }.forEach {
                if (it is EventListener) {
                    addEventListener(it)
                }
                if (it is Client<*, *>) {
                    it.sortByPriority()
                }
            }
        }
    }

    abstract class Client<A: Annotation, S: Subscription.Element<A>>: CoroutineScope {
        protected val logger = SlackLogger("GLaDOS.SubscriptionClient.${javaClass.simpleName}")

        private val job = Job()
        final override val coroutineContext = GLaDOS.dispatcher + job

        private val subscriptionsMutex = Mutex()
        private val subscriptions = mutableListOf<S>()
        val activeSubscriptions: List<S>
            get() = subscriptions.toList()

        operator fun plusAssign(target: S) {
            launch {
                subscriptionsMutex.withLock {
                    subscriptions += target
                }
            }
        }

        operator fun plusAssign(targets: Collection<S>) {
            launch {
                subscriptionsMutex.withLock {
                    subscriptions += targets
                }
            }
        }

        fun sortByPriority() {
            launch {
                subscriptionsMutex.withLock {
                    subscriptions.sortBy { it.priority }
                }
            }
        }
    }

    object ListenerEvent: Client<Plugin.Event, Subscription.Event>(), EventListener {
        @SubscribeEvent
        fun onGenericEvent(event: Event) {
            launch {
                val targets = activeSubscriptions.filter { it.matchParameters(event) }
                if (targets.isEmpty()) {
                    return@launch
                }

                val guild = try {
                    event.javaClass.getMethod("getGuild").invoke(event) as? Guild
                } catch (e: NoSuchMethodException) {
                    null
                }

                targets.forEach {
                    launch {
                        it.invoke(event)
                        it.logger.trace { "実行されました。(${guild?.name})" }
                    }
                }
            }
        }
    }

    object Command: Client<Plugin.Command, Subscription.Command>(), EventListener {
        val spaceRegex = "\\s+".toRegex()

        @SubscribeEvent
        fun onMessageReceived(event: MessageReceivedEvent) {
            if (event.author.isBotOrSelfUser) {
                return
            }

            launch {
                handleMessage(event, event.message, event.channelType)
            }
        }

        @SubscribeEvent
        fun onMessageUpdate(event: MessageUpdateEvent) {
            if (event.author.isBotOrSelfUser) {
                return
            }

            launch {
                handleMessage(event, event.message, event.channelType)
            }
        }

        private suspend fun handleMessage(event: GenericMessageEvent, message: Message, channelType: ChannelType) {
            val text = message.contentDisplay.trim()

            for (subscription in activeSubscriptions) {
                val args = subscription.parseArgs(text) ?: continue
                val commandEvent = when (event) {
                    is MessageReceivedEvent -> Plugin.Command.Event(args, subscription, event)
                    is MessageUpdateEvent -> Plugin.Command.Event(args, subscription, event)
                    else -> throw UnsupportedOperationException("Unknown event: ${event.javaClass.canonicalName}.")
                }

                when {
                    !subscription.satisfyChannelTypeRequirement(channelType) -> {
                        commandEvent.reply {
                            embed {
                                title("コマンドエラー: `${commandEvent.command.primaryCommandSyntax}`")
                                description { "このコマンドは ${subscription.targetChannelType.types.joinToString(", ") { it.name }} チャンネルでのみ実行可能です。" }
                                color(HexColor.Bad)
                                timestamp()
                            }
                        }.launchAndDelete(15, TimeUnit.SECONDS)

                        logger.warn { "\"$text\": チャンネルタイプが対象外のため実行されませんでした。 (${commandEvent.authorName})" }
                    }
                    !subscription.satisfyBotChannelRequirement(event.channel) -> {
                        commandEvent.reply {
                            embed {
                                title("コマンドエラー: `${commandEvent.command.primaryCommandSyntax}`")
                                description { "このコマンドは ${event.guild.config?.textChannel("bot")?.asMention ?: "#bot"} チャンネルでのみ実行可能です。" }
                                color(HexColor.Bad)
                                timestamp()
                            }
                        }.launchAndDelete(15, TimeUnit.SECONDS)

                        logger.warn { "\"$text\": Bot チャンネルではないため実行されませんでした。 (${commandEvent.authorName})" }
                    }
                    !satisfyCommandAvailabilityForGuildRequirement(event.guild) -> {
                        commandEvent.reply {
                            embed {
                                title("コマンドエラー: `${commandEvent.command.primaryCommandSyntax}`")
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
                                title("コマンドエラー: `${commandEvent.command.primaryCommandSyntax}`")
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
                                title("コマンドエラー: `${commandEvent.command.primaryCommandSyntax}`")
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
                                title("コマンドエラー: `${commandEvent.command.primaryCommandSyntax}`")
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
                                    title("コマンドエラー: `${commandEvent.command.primaryCommandSyntax}`")
                                    description { "このコマンドは`${event.guild.name}`の管理者ロールが付与されているメンバーのみが実行できます。判定に問題がある場合はサーバのオーナーにご連絡ください。" }
                                    color(HexColor.Bad)
                                    timestamp()
                                }
                            }.launchAndDelete(30, TimeUnit.SECONDS)

                            logger.warn { "\"$text\": 管理者ロールがないため実行されませんでした。 (${commandEvent.authorName})" }
                        } else {
                            commandEvent.reply {
                                embed {
                                    title("コマンドエラー: `${commandEvent.command.primaryCommandSyntax}`")
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
                                title("コマンドエラー: `${commandEvent.command.primaryCommandSyntax}`")
                                description { "このコマンドはGLaDOSのオーナーのみが実行できます。" }
                                color(HexColor.Bad)
                                timestamp()
                            }
                        }.launchAndDelete(30, TimeUnit.SECONDS)

                        logger.warn { "\"$text\": オーナーではないため実行されませんでした。 (${commandEvent.authorName})" }
                    }
                    else -> {
                        return if (subscription.isExperimental) {
                            commandEvent.message.prompt {
                                emoji<ExperimentalConsent, ExperimentalConsent>(
                                    title = "`${commandEvent.command.primaryCommandSyntax}`", description = "⚠ この機能は現在 試験中(Experimental) です。予期しない不具合が発生する可能性がありますが, ご理解の上ご利用ください。", color = HexColor.Change, timeoutSec = 30
                                ) { consent, m, _ ->
                                    launch {
                                        if (consent == ExperimentalConsent.Agree) {
                                            m.delete().launch()

                                            subscription.invoke(commandEvent)
                                            subscription.logger.info { "同意したので実行されました。(${event.guild?.name})" }
                                        } else {
                                            m.edit {
                                                embed {
                                                    title("`${commandEvent.command.primaryCommandSyntax}`")
                                                    description { "キャンセルしました。" }
                                                    color(HexColor.Bad)
                                                    timestamp()
                                                }
                                            }.awaitAndDelete(15, TimeUnit.SECONDS)
                                        }
                                    }
                                }
                            }
                        } else {
                            subscription.invoke(commandEvent)
                            subscription.logger.trace { "実行されました。(${event.guild?.name})" }
                        }
                    }
                }
            }
        }

        private enum class ExperimentalConsent(override val emoji: String, override val friendlyName: String): PromptEmoji {
            Agree("✅", "OK"), Disagree("❌", "キャンセル")
        }

        private fun Subscription.Command.satisfyChannelTypeRequirement(type: ChannelType): Boolean {
            return type in targetChannelType.types
        }

        private fun Subscription.Command.satisfyBotChannelRequirement(channel: MessageChannel): Boolean {
            return targetChannelType != Plugin.Command.TargetChannelType.BotChannel || (channel is TextChannel && channel.guild.config?.textChannel("bot") == channel)
        }

        private fun Subscription.Command.parseArgs(text: String): List<String>? {
            val split = text.split(spaceRegex)
            val syntax = split.firstOrNull() ?: return null

            val matched = when (casePolicy) {
                Plugin.Command.CasePolicy.Strict -> {
                    commandSyntaxes.any { it == syntax }
                }
                Plugin.Command.CasePolicy.Ignore -> {
                    commandSyntaxes.any { it.equals(syntax, true) }
                }
            }

            if (!matched) {
                return null
            }

            return split.drop(1)
        }

        private fun Subscription.Command.satisfyArgumentsRequirement(args: List<String>): Boolean {
            return !shouldCheckArgumentsCount || arguments.size == args.size
        }

        private fun satisfyCommandAvailabilityForGuildRequirement(guild: Guild?): Boolean {
            return guild == null || guild.config.booleanOption("enable_command", false)
        }

        private fun Subscription.Command.satisfyCommandConditionOfWhileInAnyVoiceChannel(voiceState: GuildVoiceState?): Boolean {
            return conditionPolicy != Plugin.Command.ConditionPolicy.WhileInAnyVoiceChannel || voiceState?.inVoiceChannel() == true
        }

        private fun Subscription.Command.satisfyCommandConditionOfWhileInSameVoiceChannel(member: Member?): Boolean {
            return conditionPolicy != Plugin.Command.ConditionPolicy.WhileInSameVoiceChannel || member?.guild?.currentVoiceChannel == member?.voiceState?.channel
        }

        private fun Subscription.Command.satisfyCommandPermissionOfAdminOnly(event: net.dv8tion.jda.core.events.Event): Boolean {
            val isAdmin = when (event) {
                is MessageReceivedEvent -> {
                    event.member?.hasAdminCapability() ?: event.author?.hasAdminCapability()
                }
                is MessageUpdateEvent -> {
                    event.member?.hasAdminCapability() ?: event.author?.hasAdminCapability()
                }
                else -> null
            } ?: false
            return permissionPolicy != Plugin.Command.PermissionPolicy.AdminOnly || isAdmin
        }

        private fun Subscription.Command.satisfyCommandPermissionOfMainGuildAdminOnly(event: net.dv8tion.jda.core.events.Event): Boolean {
            val guild = when (event) {
                is MessageReceivedEvent -> {
                    event.guild?.config
                }
                is MessageUpdateEvent -> {
                    event.guild?.config
                }
                else -> null
            }
            val isAdmin = when (event) {
                is MessageReceivedEvent -> {
                    event.member?.hasAdminCapability() ?: event.author?.hasAdminCapability()
                }
                is MessageUpdateEvent -> {
                    event.member?.hasAdminCapability() ?: event.author?.hasAdminCapability()
                }
                else -> null
            } ?: false
            return permissionPolicy != Plugin.Command.PermissionPolicy.MainGuildAdminOnly || (guild != null && guild.isMain && isAdmin)
        }

        private fun Subscription.Command.satisfyCommandPermissionOfOwnerOnly(event: net.dv8tion.jda.core.events.Event): Boolean {
            val isGLaDOSOwner = when (event) {
                is MessageReceivedEvent -> {
                    event.author.isGLaDOSOwner
                }
                is MessageUpdateEvent -> {
                    event.author.isGLaDOSOwner
                }
                else -> false
            }
            return permissionPolicy != Plugin.Command.PermissionPolicy.OwnerOnly || isGLaDOSOwner
        }
    }

    object Loop: Client<Plugin.Loop, Subscription.Loop>(), EventListener {
        @Suppress("UNUSED_PARAMETER")
        @SubscribeEvent
        fun onReady(event: ReadyEvent) {
            launch {
                for (it in activeSubscriptions) {
                    launch {
                        while (isActive) {
                            try {
                                it.invoke()
                                it.logger.trace { "実行されました。" }
                            } catch (e: CancellationException) {
                                break
                            }

                            try {
                                delay(it.intervalMillis)
                            } catch (e: CancellationException) {
                                break
                            }
                        }

                        it.logger.trace { "終了しました。" }
                    }

                    it.logger.trace { "開始しました。" }
                }
            }
        }
    }

    object Schedule: Client<Plugin.Schedule, Subscription.Schedule>(), EventListener {
        @Suppress("UNUSED_PARAMETER")
        @SubscribeEvent
        fun onReady(event: ReadyEvent) {
            if (activeSubscriptions.isEmpty()) {
                return
            }

            launch {
                while (isActive) {
                    try {
                        val calendar = Calendar.getInstance()
                        delay(60000L - calendar.get(Calendar.SECOND) * 1000L - calendar.get(Calendar.MILLISECOND))

                        val newCalendar = Calendar.getInstance()
                        activeSubscriptions.filter {
                            it.matches(newCalendar)
                        }.forEach {
                            launch {
                                it.invoke()
                                it.logger.trace { "実行されました。" }
                            }
                        }

                        delay(1000)
                    } catch (e: CancellationException) {
                        break
                    }
                }

                logger.trace { "終了しました。" }
            }

            logger.trace { "開始しました。" }
        }
    }

    class AudioEvent private constructor(private val guildPlayer: GuildPlayer): Client<Plugin.Event, Subscription.Event>(), AudioEventListener {
        companion object: CoroutineScope {
            override val coroutineContext: CoroutineContext
                get() = GLaDOS.dispatcher

            private val subscriptionsMutex = Mutex()
            private val subscriptions = mutableListOf<Subscription.Event>()

            operator fun plusAssign(subscription: Subscription.Event) {
                launch {
                    subscriptionsMutex.withLock {
                        subscriptions += subscription
                    }
                }
            }

            fun create(guildPlayer: GuildPlayer): AudioEvent {
                return SubscriptionClient.AudioEvent(guildPlayer).also {
                    it += AudioEvent.subscriptions
                    it.sortByPriority()
                }
            }
        }

        private fun runEvent(vararg args: Any) {
            activeSubscriptions.filter {
                it.matchParameters(*args)
            }.forEach {
                launch {
                    it.invoke(*args)
                    it.logger.trace { "実行されました。(${guildPlayer.guild.name})" }
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

    class ReceiveAudio private constructor(private val guildPlayer: GuildPlayer): Client<Plugin.Event, Subscription.Event>(), AudioReceiveHandler {
        companion object: CoroutineScope {
            override val coroutineContext: CoroutineContext
                get() = GLaDOS.dispatcher

            private val subscriptionsMutex = Mutex()
            private val subscriptions = mutableListOf<Subscription.Event>()

            operator fun plusAssign(subscription: Subscription.Event) {
                launch {
                    subscriptionsMutex.withLock {
                        subscriptions += subscription
                    }
                }
            }

            fun create(guildPlayer: GuildPlayer): ReceiveAudio {
                return ReceiveAudio(guildPlayer).also {
                    it += subscriptions
                    it.sortByPriority()
                }
            }
        }

        private fun runEvent(vararg args: Any) {
            activeSubscriptions.filter {
                it.matchParameters(*args)
            }.forEach {
                launch {
                    it.invoke(*args)
                    it.logger.trace { "実行されました。(${guildPlayer.guild.name})" }
                }
            }
        }

        override fun canReceiveCombined() = true

        override fun handleCombinedAudio(combinedAudio: CombinedAudio) {
            runEvent(guildPlayer, combinedAudio)
        }

        override fun canReceiveUser() = true

        override fun handleUserAudio(userAudio: UserAudio) {
            runEvent(guildPlayer, userAudio)
        }
    }

    class ConnectionEvent private constructor(val guild: Guild): Client<Plugin.Event, Subscription.Event>(), ConnectionListener {
        companion object: CoroutineScope {
            override val coroutineContext: CoroutineContext
                get() = GLaDOS.dispatcher

            private val subscriptionsMutex = Mutex()
            private val subscriptions = mutableListOf<Subscription.Event>()

            operator fun plusAssign(subscription: Subscription.Event) {
                launch {
                    subscriptionsMutex.withLock {
                        subscriptions += subscription
                    }
                }
            }

            fun create(guild: Guild): ConnectionEvent {
                return SubscriptionClient.ConnectionEvent(guild).also {
                    it += ConnectionEvent.subscriptions
                    it.sortByPriority()
                }
            }
        }

        private fun runEvent(vararg args: Any) {
            activeSubscriptions.filter {
                it.matchParameters(*args)
            }.forEach {
                launch {
                    it.invoke(*args)
                    it.logger.trace { "実行されました。(${guild.name})" }
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

    object Tweetstorm: Client<Plugin.Tweetstorm, Subscription.Tweetstorm>(), EventListener {
        @Suppress("UNUSED_PARAMETER")
        @SubscribeEvent
        fun onReady(event: ReadyEvent) {
            activeSubscriptions.flatMap { it.accounts }.distinct().forEach { account ->
                launch {
                    while (true) {
                        try {
                            @Suppress("DEPRECATION") account.client.use {
                                it.stream.user().await().listen(createListener(account)).startBlocking(autoReconnect = false)
                            }
                        } catch (e: CancellationException) {
                            break
                        } catch (e: Throwable) {
                            logger.error(e) { "例外が発生しました。(@${account.user.screenName})" }
                        }

                        try {
                            delay(5000)
                        } catch (e: CancellationException) {
                            break
                        }
                    }

                    logger.trace { "終了しました。(@${account.user.screenName})" }
                }

                logger.trace { "開始しました。(@${account.user.screenName})" }
            }
        }

        private fun createListener(account: GLaDOSConfig.Accounts.TwitterAccount) = object: UserStreamListener {
            override suspend fun onConnect() {
                runEvent(Plugin.Tweetstorm.ConnectEvent(account))

                logger.info { "Tweetstorm (@${account.user.screenName}) が開始されました。" }
            }

            override suspend fun onDisconnect() {
                runEvent(Plugin.Tweetstorm.DisconnectEvent(account))

                logger.warn { "Tweetstorm (@${account.user.screenName}) から切断されました。" }
            }

            override suspend fun onStatus(status: Status) {
                runEvent(Plugin.Tweetstorm.StatusEvent(account, status))
            }

            override suspend fun onDirectMessage(message: DirectMessage) {
                runEvent(Plugin.Tweetstorm.DirectMessageEvent(account, message))
            }

            override suspend fun onAnyEvent(event: UserStreamEvent) {
                runEvent(Plugin.Tweetstorm.StreamEvent(account, event))
            }

            override suspend fun onAnyStatusEvent(event: UserStreamStatusEvent) {
                runEvent(Plugin.Tweetstorm.StreamStatusEvent(account, event))
            }

            override suspend fun onFavorite(event: UserStreamStatusEvent) {
                runEvent(Plugin.Tweetstorm.FavoriteEvent(account, event))
            }

            override suspend fun onUnfavorite(event: UserStreamStatusEvent) {
                runEvent(Plugin.Tweetstorm.UnfavoriteEvent(account, event))
            }

            override suspend fun onFavoritedRetweet(event: UserStreamStatusEvent) {
                runEvent(Plugin.Tweetstorm.FavoritedRetweetEvent(account, event))
            }

            override suspend fun onRetweetedRetweet(event: UserStreamStatusEvent) {
                runEvent(Plugin.Tweetstorm.RetweetedRetweetEvent(account, event))
            }

            override suspend fun onQuotedTweet(event: UserStreamStatusEvent) {
                runEvent(Plugin.Tweetstorm.QuotedTweetEvent(account, event))
            }

            override suspend fun onAnyUserEvent(event: UserStreamUserEvent) {
                runEvent(Plugin.Tweetstorm.StreamUserEvent(account, event))
            }

            override suspend fun onFollow(event: UserStreamUserEvent) {
                runEvent(Plugin.Tweetstorm.FollowEvent(account, event))
            }

            override suspend fun onUnfollow(event: UserStreamUserEvent) {
                runEvent(Plugin.Tweetstorm.UnfollowEvent(account, event))
            }

            override suspend fun onMute(event: UserStreamUserEvent) {
                runEvent(Plugin.Tweetstorm.MuteEvent(account, event))
            }

            override suspend fun onUnmute(event: UserStreamUserEvent) {
                runEvent(Plugin.Tweetstorm.UnmuteEvent(account, event))
            }

            override suspend fun onBlock(event: UserStreamUserEvent) {
                runEvent(Plugin.Tweetstorm.BlockEvent(account, event))
            }

            override suspend fun onUnblock(event: UserStreamUserEvent) {
                runEvent(Plugin.Tweetstorm.UnblockEvent(account, event))
            }

            override suspend fun onUserUpdate(event: UserStreamUserEvent) {
                runEvent(Plugin.Tweetstorm.UserUpdateEvent(account, event))
            }

            override suspend fun onAnyListEvent(event: UserStreamListEvent) {
                runEvent(Plugin.Tweetstorm.StreamListEvent(account, event))
            }

            override suspend fun onListCreated(event: UserStreamListEvent) {
                runEvent(Plugin.Tweetstorm.ListCreatedEvent(account, event))
            }

            override suspend fun onListDestroyed(event: UserStreamListEvent) {
                runEvent(Plugin.Tweetstorm.ListDestroyedEvent(account, event))
            }

            override suspend fun onListMemberAdded(event: UserStreamListEvent) {
                runEvent(Plugin.Tweetstorm.ListMemberAddedEvent(account, event))
            }

            override suspend fun onListMemberRemoved(event: UserStreamListEvent) {
                runEvent(Plugin.Tweetstorm.ListMemberRemovedEvent(account, event))
            }

            override suspend fun onListUpdated(event: UserStreamListEvent) {
                runEvent(Plugin.Tweetstorm.ListUpdatedEvent(account, event))
            }

            override suspend fun onListUserSubscribed(event: UserStreamListEvent) {
                runEvent(Plugin.Tweetstorm.ListUserSubscribedEvent(account, event))
            }

            override suspend fun onListUserUnsubscribed(event: UserStreamListEvent) {
                runEvent(Plugin.Tweetstorm.ListUserUnsubscribedEvent(account, event))
            }

            override suspend fun onFriends(friends: UserStreamFriends) {
                runEvent(Plugin.Tweetstorm.FriendsEvent(account, friends))
            }

            override suspend fun onDelete(delete: StreamDelete) {
                runEvent(Plugin.Tweetstorm.DeleteEvent(account, delete))
            }

            override suspend fun onDisconnectMessage(disconnect: UserStreamDisconnect) {
                runEvent(Plugin.Tweetstorm.DisconnectMessageEvent(account, disconnect))
            }

            override suspend fun onLimit(limit: UserStreamLimit) {
                runEvent(Plugin.Tweetstorm.LimitEvent(account, limit))
            }

            override suspend fun onScrubGeo(scrubGeo: UserStreamScrubGeo) {
                runEvent(Plugin.Tweetstorm.ScrubGeoEvent(account, scrubGeo))
            }

            override suspend fun onStatusWithheld(withheld: UserStreamStatusWithheld) {
                runEvent(Plugin.Tweetstorm.StatusWithheldEvent(account, withheld))
            }

            override suspend fun onUserWithheld(withheld: UserStreamUserWithheld) {
                runEvent(Plugin.Tweetstorm.UserWithheldEvent(account, withheld))
            }

            override suspend fun onWarning(warning: UserStreamWarning) {
                runEvent(Plugin.Tweetstorm.WarningEvent(account, warning))
            }

            override suspend fun onHeartbeat() {
                runEvent(Plugin.Tweetstorm.HeartbeatEvent(account))
            }

            override suspend fun onLength(length: Int) {
                runEvent(Plugin.Tweetstorm.LengthEvent(account, length))
            }

            override suspend fun onAnyJson(json: JsonObject) {
                runEvent(Plugin.Tweetstorm.AnyJsonEvent(account, json))
            }

            override suspend fun onUnhandledJson(json: JsonObject) {
                runEvent(Plugin.Tweetstorm.UnhandledJsonEvent(account, json))
            }

            override suspend fun onUnknownData(data: String) {
                runEvent(Plugin.Tweetstorm.UnknownDataEvent(account, data))
            }

            override suspend fun onRawData(data: String) {
                runEvent(Plugin.Tweetstorm.RawDataEvent(account, data))
            }

            private fun runEvent(event: Plugin.Tweetstorm.Event) {
                activeSubscriptions.filter { event.account in it.accounts && it.matchParameters(event) }.forEach {
                    launch {
                        it.invoke(event)
                        it.logger.trace { "実行されました。(@${event.account.user.screenName})" }
                    }
                }
            }
        }
    }

    object Web: EventListener {
        lateinit var application: Application
        private val logger = SlackLogger("GLaDOS.SubscriptionClient.${javaClass.simpleName}")

        @Suppress("UNUSED_PARAMETER")
        @SubscribeEvent
        fun onReady(event: ReadyEvent) {
            if (Page.activeSubscriptions.isEmpty() && ErrorPage.activeSubscriptions.isEmpty()) {
                return
            }

            Page.sortByPriority()
            ErrorPage.sortByPriority()
            Session.sortByPriority()

            embeddedServer(Netty, GLaDOS.config.web.port, GLaDOS.config.web.host) {
                install(XForwardedHeaderSupport)
                install(AutoHeadResponse)
                install(DefaultHeaders) {
                    header(HttpHeaders.Server, "GLaDOS-bot")
                    header("X-Powered-By", "GLaDOS-bot (+https://github.com/NephyProject/GLaDOS-bot)")
                }
                install(Metrics) {
                    if (GLaDOS.isDebugMode) {
                        val log = KotlinLogging.logger("GLaDOS.SubscriptionClient.Web.Metrics")
                        Slf4jReporter.forRegistry(registry).outputTo(log).convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build().start(10, TimeUnit.SECONDS)
                    }
                }
                install(StatusPages) {
                    exception<Exception> { e ->
                        logger.error(e) { "Internal server error occurred." }
                        call.respond(HttpStatusCode.InternalServerError)
                    }

                    for (status in ErrorPage.activeSubscriptions.flatMap { it.statuses }.distinct()) {
                        status(status) {
                            val subscription = ErrorPage.activeSubscriptions.firstOrNull { status in it.statuses && (it.domain == null || it.domain == call.request.effectiveHost) } ?: return@status call.respond(status)

                            val accessEvent = Plugin.Web.AccessEvent(this, null, emptyMap())
                            subscription.invoke(accessEvent, status)
                        }
                    }
                }
                install(Web.Feature)
                install(Sessions) {
                    runBlocking {
                        for (subscription in Session.activeSubscriptions) {
                            cookie(subscription.sessionName, subscription.sessionClass, SessionStorageMemory()) {
                                subscription.invoke(this)
                            }
                        }
                    }
                }
                install(Routing) {
                    get("/sitemap.xml") {
                        val allRouting = "all" in call.parameters
                        call.respondText(ContentType.Text.Xml) {
                            buildString {
                                appendln("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                                appendln("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">")
                                for (page in Page.activeSubscriptions.filter { allRouting || (it.domain == null || it.domain == call.request.origin.host) && !it.banRobots && it.pathType == Plugin.Web.PathType.Normal }) {
                                    appendln("    <url>")
                                    appendln("        <loc>${call.request.origin.scheme}://${call.request.origin.host}${page.path}</loc>")
                                    appendln("        <changefreq>${page.updateFrequency.name.toLowerCase()}</changefreq>")
                                    appendln(
                                        "        <priority>${when (page.priority) {
                                            Plugin.Priority.Highest -> "1.0"
                                            Plugin.Priority.Higher -> "0.8"
                                            Plugin.Priority.High -> "0.7"
                                            Plugin.Priority.Normal -> "0.5"
                                            Plugin.Priority.Low -> "0.3"
                                            Plugin.Priority.Lower -> "0.2"
                                            Plugin.Priority.Lowest -> "0.1"
                                        }}</priority>"
                                    )
                                    appendln("    </url>")
                                }
                                appendln("</urlset>")
                            }
                        }
                    }

                    get("/robots.txt") {
                        call.respondText(ContentType.Text.Plain) {
                            buildString {
                                Page.activeSubscriptions.filter { (it.domain == null || it.domain == call.request.origin.host) && it.banRobots && it.pathType == Plugin.Web.PathType.Normal }.forEach {
                                    appendln("User-agent: *")
                                    appendln("Disallow: ${it.path}")
                                }
                                appendln("User-agent: *")
                                appendln("Sitemap: /sitemap.xml")
                            }
                        }
                    }
                }

                application = this
            }.start(wait = false)
        }

        private object Feature: ApplicationFeature<ApplicationCallPipeline, Feature.Configuration, Feature.DynamicResolver> {
            override val key = AttributeKey<DynamicResolver>("DynamicResolver")

            override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): DynamicResolver {
                return DynamicResolver.apply {
                    pipeline.intercept(ApplicationCallPipeline.Call) {
                        handleRequest()
                    }
                }
            }

            private object Configuration

            object DynamicResolver {
                private val accessLogger = SlackLogger("GLaDOS.Web.Access", channelName = "#glados-web")
                private val accessStaticLogger = SlackLogger("GLaDOS.Web.AccessStatic", channelName = "#glados-web-static")

                suspend fun PipelineContext<Unit, ApplicationCall>.handleRequest() {
                    val host = call.request.effectiveHost
                    val path = call.request.path().removePrefix("/").removeSuffix("/")

                    if (call.request.httpMethod == HttpMethod.Get) {
                        val searchPaths = listOf(
                            arrayOf(host, path), arrayOf(host, path, "index.html"), arrayOf(path)
                        )
                        val file = searchPaths.map { resourceFile("static", *it) }.firstOrNull { it.isFile && it.exists() }
                        if (file != null) {
                            call.respondFile(file)
                            callLogging(accessStaticLogger)
                            return
                        }
                    }

                    val routing = Page.activeSubscriptions.find { it.canHandle(call) } ?: return
                    val event = routing.makeEvent(this)

                    if (call.request.httpMethod != HttpMethod.Options) {
                        if (routing.invoke(event)) {
                            callLogging(accessLogger)
                        }
                    } else {
                        call.respond(HttpStatusCode.OK, "")
                    }
                }

                private fun PipelineContext<Unit, ApplicationCall>.callLogging(logger: SlackLogger) {
                    val httpStatus = call.response.status() ?: HttpStatusCode.OK
                    val userAgent = call.request.userAgent()
                    val remoteHost = call.request.origin.remoteHost
                    if (!userAgent.isNullOrBlank() && GLaDOS.config.web.ignoreUserAgents.any { it in userAgent } || GLaDOS.config.web.ignoreIpAddressRanges.any { it in remoteHost }) {
                        return
                    }

                    logger.info {
                        buildString {
                            appendln("${httpStatus.value} ${httpStatus.description}: ${call.request.origin.version} ${call.request.origin.method.value} ${call.request.url}")
                            append("<- $remoteHost")
                            if (!userAgent.isNullOrBlank()) {
                                append(" ($userAgent)")
                            }
                        }
                    }
                }
            }
        }

        object Page: Client<Plugin.Web.Page, Subscription.Web.Page>()

        object ErrorPage: Client<Plugin.Web.ErrorPage, Subscription.Web.ErrorPage>()

        object Session: Client<Plugin.Web.Session, Subscription.Web.Session>()
    }
}
