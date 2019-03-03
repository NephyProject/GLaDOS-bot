/*
 * The MIT License (MIT)
 *
 *     Copyright (c) 2017-2019 Nephy Project Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package jp.nephy.glados.clients.web

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
