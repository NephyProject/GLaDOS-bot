package jp.nephy.glados.api.plugin.web

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.util.pipeline.PipelineContext
import jp.nephy.glados.api.plugin.Priority
import kotlin.reflect.KClass

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
}
