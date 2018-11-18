package jp.nephy.glados.core.plugins

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.util.pipeline.PipelineContext
import jp.nephy.glados.config
import jp.nephy.glados.core.config.GLaDOSConfig
import jp.nephy.glados.core.logger.SlackLogger
import jp.nephy.glados.core.extensions.displayName
import jp.nephy.glados.core.extensions.fullName
import jp.nephy.glados.core.extensions.ifNullOrBlank
import jp.nephy.glados.core.extensions.spaceRegex
import jp.nephy.glados.core.extensions.web.SitemapUpdateFrequency
import jp.nephy.glados.dispatcher
import jp.nephy.jsonkt.*
import jp.nephy.penicillin.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.io.core.Closeable
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.MessageUpdateEvent
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

abstract class Plugin(
    pluginName: String? = null, version: String? = null, val description: String? = null
): EventModel, CoroutineScope, Closeable {
    val name = pluginName.ifNullOrBlank {
        var name = javaClass.canonicalName
        for (prefix in config.pluginsPackagePrefixes) {
            name = name.removePrefix("$prefix.")
        }
        name
    }
    val fullname = "$name[v${version ?: "1.0.0.0"}]"

    private val job = Job()
    override val coroutineContext
        get() = dispatcher + job

    val logger = SlackLogger("Plugin.$fullname")

    val isExperimental: Boolean
        get() = this::class.findAnnotation<Experimental>() != null

    override fun close() {}

    enum class Priority {
        Highest, Higher, High, Normal, Low, Lower, Lowest
    }

    @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
    annotation class Experimental

    @Target(AnnotationTarget.FUNCTION)
    annotation class Event(
        val priority: Priority = Priority.Normal
    )

    @Target(AnnotationTarget.FUNCTION)
    annotation class Command(
        val command: String = "",
        val aliases: Array<String> = [],
        val priority: Priority = Priority.Normal,
        val permission: PermissionPolicy = PermissionPolicy.Anyone,
        val channelType: TargetChannelType = TargetChannelType.Any,
        val case: CasePolicy = CasePolicy.Strict,
        val condition: ConditionPolicy = ConditionPolicy.Anytime,
        val description: String = "",
        val args: Array<String> = [],
        val checkArgsCount: Boolean = true,
        val prefix: String = "",
        val category: String = ""
    ) {
        enum class PermissionPolicy {

            Anyone, AdminOnly, MainGuildAdminOnly, OwnerOnly
        }

        enum class TargetChannelType(vararg val types: ChannelType) {
            Any(net.dv8tion.jda.core.entities.ChannelType.TEXT, net.dv8tion.jda.core.entities.ChannelType.PRIVATE),
            TextChannel(net.dv8tion.jda.core.entities.ChannelType.TEXT),
            BotChannel(net.dv8tion.jda.core.entities.ChannelType.TEXT),
            PrivateMessage(net.dv8tion.jda.core.entities.ChannelType.PRIVATE)
        }

        enum class CasePolicy {
            Strict, Ignore
        }

        enum class ConditionPolicy {
            Anytime, WhileInAnyVoiceChannel, WhileInSameVoiceChannel
        }

        data class Event(
            val args: String, val command: Subscription.Command, val message: Message, val author: User, val member: Member?, val guild: Guild?, val textChannel: TextChannel?, val channel: MessageChannel, private val _jda: JDA, private val _responseNumber: Long
        ): net.dv8tion.jda.core.events.Event(_jda, _responseNumber) {
            constructor(args: String, command: Subscription.Command, event: MessageReceivedEvent): this(args, command, event.message, event.author, event.member, event.guild, event.textChannel, event.channel, event.jda, event.responseNumber)
            constructor(args: String, command: Subscription.Command, event: MessageUpdateEvent): this(args, command, event.message, event.author, event.member, event.guild, event.textChannel, event.channel, event.jda, event.responseNumber)

            val argList: List<String>
                get() = if (args.isNotBlank()) {
                    args.split(spaceRegex)
                } else {
                    emptyList()
                }
            val authorName: String
                get() = member?.fullName ?: author.displayName
        }
    }

    @Target(AnnotationTarget.FUNCTION)
    annotation class Loop(
        val interval: Long, val unit: TimeUnit, val priority: Priority = Priority.Normal
    )

    @Target(AnnotationTarget.FUNCTION)
    annotation class Schedule(
        val hours: IntArray = [], val minutes: IntArray = [], val multipleHours: IntArray = [], val multipleMinutes: IntArray = [], val priority: Priority = Priority.Normal
    )

    @Target(AnnotationTarget.FUNCTION)
    annotation class Tweetstorm(
        val accounts: Array<String>, val priority: Priority = Priority.Normal
    ) {
        interface Event {
            val account: GLaDOSConfig.Accounts.TwitterAccount
        }

        data class ConnectEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount): Event

        data class DisconnectEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount): Event

        data class StatusEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val status: Status): Event

        data class DirectMessageEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val message: DirectMessage): Event

        data class StreamEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamEvent): Event

        data class StreamStatusEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamStatusEvent): Event

        data class FavoriteEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamStatusEvent): Event

        data class UnfavoriteEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamStatusEvent): Event

        data class FavoritedRetweetEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamStatusEvent): Event

        data class RetweetedRetweetEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamStatusEvent): Event

        data class QuotedTweetEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamStatusEvent): Event

        data class StreamUserEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamUserEvent): Event

        data class FollowEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamUserEvent): Event

        data class UnfollowEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamUserEvent): Event

        data class MuteEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamUserEvent): Event

        data class UnmuteEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamUserEvent): Event

        data class BlockEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamUserEvent): Event

        data class UnblockEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamUserEvent): Event

        data class UserUpdateEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamUserEvent): Event

        data class StreamListEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamListEvent): Event

        data class ListCreatedEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamListEvent): Event

        data class ListDestroyedEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamListEvent): Event

        data class ListMemberAddedEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamListEvent): Event

        data class ListMemberRemovedEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamListEvent): Event

        data class ListUpdatedEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamListEvent): Event

        data class ListUserSubscribedEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamListEvent): Event

        data class ListUserUnsubscribedEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val event: UserStreamListEvent): Event

        data class FriendsEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val friends: UserStreamFriends): Event

        data class DeleteEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val delete: StreamDelete): Event

        data class DisconnectMessageEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val disconnect: UserStreamDisconnect): Event

        data class LimitEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val limit: UserStreamLimit): Event

        data class ScrubGeoEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val scrubGeo: UserStreamScrubGeo): Event

        data class StatusWithheldEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val withheld: UserStreamStatusWithheld): Event

        data class UserWithheldEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val withheld: UserStreamUserWithheld): Event

        data class WarningEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val warning: UserStreamWarning): Event

        data class HeartbeatEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount): Event

        data class LengthEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val length: Int): Event

        data class AnyJsonEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val json: JsonObject): Event

        data class UnhandledJsonEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val json: JsonObject): Event

        data class UnknownDataEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val data: String): Event

        data class RawDataEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val data: String): Event
    }

    object Web {
        @Target(AnnotationTarget.FUNCTION)
        annotation class Page(
            val path: String,
            val domain: String = "",
            val pathType: PathType = PathType.Normal,
            val methods: Array<HttpMethod> = [],
            val regexOptions: Array<RegexOption> = [],
            val updateFrequency: SitemapUpdateFrequency = SitemapUpdateFrequency.Monthly,
            val priority: Priority = Priority.Normal,
            val banRobots: Boolean = false
        )

        @Target(AnnotationTarget.FUNCTION)
        annotation class ErrorPage(
            val statuses: Array<HttpStatusCode>, val domain: String = "", val priority: Priority = Priority.Normal
        )

        @Target(AnnotationTarget.FUNCTION)
        annotation class Session(
            val name: String, val sessionClass: KClass<*>, val priority: Priority = Priority.Normal
        )

        enum class PathType {
            Normal, Regex, Pattern
        }

        enum class HttpMethod(val ktor: io.ktor.http.HttpMethod) {
            Get(io.ktor.http.HttpMethod.Get), Post(io.ktor.http.HttpMethod.Post), Put(io.ktor.http.HttpMethod.Put), Patch(io.ktor.http.HttpMethod.Patch), Delete(io.ktor.http.HttpMethod.Delete), Head(io.ktor.http.HttpMethod.Head), Options(io.ktor.http.HttpMethod.Options)
        }

        enum class HttpStatusCode(val ktor: io.ktor.http.HttpStatusCode) {
            Continue(io.ktor.http.HttpStatusCode.Continue),
            SwitchingProtocols(io.ktor.http.HttpStatusCode.SwitchingProtocols),
            Processing(io.ktor.http.HttpStatusCode.Processing),

            OK(io.ktor.http.HttpStatusCode.OK),
            Created(io.ktor.http.HttpStatusCode.Created),
            Accepted(io.ktor.http.HttpStatusCode.Accepted),
            NonAuthoritativeInformation(io.ktor.http.HttpStatusCode.NonAuthoritativeInformation),
            NoContent(io.ktor.http.HttpStatusCode.NoContent),
            ResetContent(io.ktor.http.HttpStatusCode.ResetContent),
            PartialContent(io.ktor.http.HttpStatusCode.PartialContent),
            MultiStatus(io.ktor.http.HttpStatusCode.MultiStatus),

            MultipleChoices(io.ktor.http.HttpStatusCode.MultipleChoices),
            MovedPermanently(io.ktor.http.HttpStatusCode.MovedPermanently),
            Found(io.ktor.http.HttpStatusCode.Found),
            SeeOther(io.ktor.http.HttpStatusCode.SeeOther),
            NotModified(io.ktor.http.HttpStatusCode.NotModified),
            UseProxy(io.ktor.http.HttpStatusCode.UseProxy),
            SwitchProxy(io.ktor.http.HttpStatusCode.SwitchProxy),
            TemporaryRedirect(io.ktor.http.HttpStatusCode.TemporaryRedirect),
            PermanentRedirect(io.ktor.http.HttpStatusCode.PermanentRedirect),

            BadRequest(io.ktor.http.HttpStatusCode.BadRequest),
            Unauthorized(io.ktor.http.HttpStatusCode.Unauthorized),
            PaymentRequired(io.ktor.http.HttpStatusCode.PaymentRequired),
            Forbidden(io.ktor.http.HttpStatusCode.Forbidden),
            NotFound(io.ktor.http.HttpStatusCode.NotFound),
            MethodNotAllowed(io.ktor.http.HttpStatusCode.MethodNotAllowed),
            NotAcceptable(io.ktor.http.HttpStatusCode.NotAcceptable),
            ProxyAuthenticationRequired(io.ktor.http.HttpStatusCode.ProxyAuthenticationRequired),
            RequestTimeout(io.ktor.http.HttpStatusCode.RequestTimeout),
            Conflict(io.ktor.http.HttpStatusCode.Conflict),
            Gone(io.ktor.http.HttpStatusCode.Gone),
            LengthRequired(io.ktor.http.HttpStatusCode.LengthRequired),
            PreconditionFailed(io.ktor.http.HttpStatusCode.PreconditionFailed),
            PayloadTooLarge(io.ktor.http.HttpStatusCode.PayloadTooLarge),
            RequestURITooLong(io.ktor.http.HttpStatusCode.RequestURITooLong),

            UnsupportedMediaType(io.ktor.http.HttpStatusCode.UnsupportedMediaType),
            RequestedRangeNotSatisfiable(io.ktor.http.HttpStatusCode.RequestedRangeNotSatisfiable),
            ExpectationFailed(io.ktor.http.HttpStatusCode.ExpectationFailed),
            UnprocessableEntity(io.ktor.http.HttpStatusCode.UnprocessableEntity),
            Locked(io.ktor.http.HttpStatusCode.Locked),
            FailedDependency(io.ktor.http.HttpStatusCode.FailedDependency),
            UpgradeRequired(io.ktor.http.HttpStatusCode.UpgradeRequired),
            TooManyRequests(io.ktor.http.HttpStatusCode.TooManyRequests),
            RequestHeaderFieldTooLarge(io.ktor.http.HttpStatusCode.RequestHeaderFieldTooLarge),

            InternalServerError(io.ktor.http.HttpStatusCode.InternalServerError),
            NotImplemented(io.ktor.http.HttpStatusCode.NotImplemented),
            BadGateway(io.ktor.http.HttpStatusCode.BadGateway),
            ServiceUnavailable(io.ktor.http.HttpStatusCode.ServiceUnavailable),
            GatewayTimeout(io.ktor.http.HttpStatusCode.GatewayTimeout),
            VersionNotSupported(io.ktor.http.HttpStatusCode.VersionNotSupported),
            VariantAlsoNegotiates(io.ktor.http.HttpStatusCode.VariantAlsoNegotiates),
            InsufficientStorage(io.ktor.http.HttpStatusCode.InsufficientStorage)
        }

        data class AccessEvent(val context: PipelineContext<*, ApplicationCall>, val matchResult: MatchResult?, val fragments: Map<String, String>) {
            val call: ApplicationCall
                get() = context.call
        }
    }
}
