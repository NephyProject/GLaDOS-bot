package jp.nephy.glados.core.wui

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.client.request.post
import io.ktor.features.origin
import io.ktor.html.respondHtmlTemplate
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.encodeURLParameter
import io.ktor.http.userAgent
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.response.respondFile
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.sessions.clear
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import io.ktor.util.pipeline.PipelineContext
import jp.nephy.glados.config
import jp.nephy.glados.httpClient
import jp.nephy.glados.plugins.player.SoundBot
import jp.nephy.glados.slack
import jp.nephy.glados.userAgent
import jp.nephy.jsonkt.ImmutableJsonObject
import jp.nephy.jsonkt.delegation.JsonModel
import jp.nephy.jsonkt.delegation.long
import jp.nephy.jsonkt.delegation.string
import jp.nephy.jsonkt.parse
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.writeStringUtf8
import kotlinx.html.*
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.collections.set

fun Route.getTop() {
    get("/") {
        call.respondHtmlTemplate(FooterLayout()) {
            footerContent {
                style {
                    unsafe {
                        +".github-corner:hover .octo-arm{animation:octocat-wave 560ms ease-in-out}@keyframes octocat-wave{0%,100%{transform:rotate(0)}20%,60%{transform:rotate(-25deg)}40%,80%{transform:rotate(10deg)}}@media (max-width:500px){.github-corner:hover .octo-arm{animation:none}.github-corner .octo-arm{animation:octocat-wave 560ms ease-in-out}}"
                    }
                }
                a("https://github.com/SlashNephy/GLaDOS-bot", "_blank", "github-corner") {
                    attributes["aria-label"] = "View source on Github"
                    unsafe {
                        +"""<svg width="80" height="80" viewBox="0 0 250 250" style="fill:#151513; color:#fff; position: absolute; top: 0; border: 0; right: 0;" aria-hidden="true"><path d="M0,0 L115,115 L130,115 L142,142 L250,250 L250,0 Z"></path><path d="M128.3,109.0 C113.8,99.7 119.0,89.6 119.0,89.6 C122.0,82.7 120.5,78.6 120.5,78.6 C119.2,72.0 123.4,76.3 123.4,76.3 C127.3,80.9 125.5,87.3 125.5,87.3 C122.9,97.6 130.6,101.9 134.4,103.2" fill="currentColor" style="transform-origin: 130px 106px;" class="octo-arm"></path><path d="M115.0,115.0 C114.9,115.1 118.7,116.5 119.8,115.4 L133.7,101.6 C136.9,99.2 139.9,98.4 142.2,98.6 C133.8,88.0 127.5,74.4 143.8,58.0 C148.5,53.4 154.0,51.2 159.7,51.0 C160.3,49.4 163.2,43.6 171.4,40.1 C171.4,40.1 176.1,42.5 178.8,56.2 C183.1,58.6 187.2,61.8 190.9,65.4 C194.5,69.0 197.7,73.2 200.1,77.6 C213.8,80.2 216.3,84.9 216.3,84.9 C212.7,93.1 206.9,96.0 205.4,96.6 C205.1,102.4 203.0,107.8 198.3,112.5 C181.9,128.9 168.3,122.5 157.7,114.1 C157.9,116.9 156.7,120.9 152.7,124.9 L141.0,136.5 C139.8,137.7 141.6,141.9 141.8,141.8 Z" fill="currentColor" class="octo-body"></path></svg>"""
                    }
                }
                div("jumbotron") {
                    h1 { +"GLaDOS WebUI" }
                    hr("my-4")
                    p("lead") { +"View GLaDOS-bot activities on Web." }
                }
                div("container") {
                    div("list-group") {
                        a("/dashboard", "_blank", "list-group-item list-group-item-action") {
                            span("fas fa-tachometer-alt")
                            +" Dashboard"
                        }
                        a("https://github.com/SlashNephy/GLaDOS-bot", "_blank", "list-group-item list-group-item-action") {
                            span("fab fa-github")
                            +" GitHub"
                        }
                        a("https://twitter.com/SlashNephy", "_blank", "list-group-item list-group-item-action") {
                            span("fab fa-twitter")
                            +" Contact"
                        }
                    }
                }
            }
        }
    }
}

fun Route.getDashboard() {
    get("/dashboard") {
        val session = call.sessions.get<DiscordOAuth2Session>() ?: return@get call.respondRedirect("https://glados.nephy.jp/api/login?path=/dashboard")
        val user = session.user

        call.respondHtmlTemplate(NavLayout()) {
            navContent {
                div("alert alert-success") {
                    h4 {
                        span("far fa-smile-wink")
                        +" You're logged in as @${user.username}#${user.discriminator}. If you wish to sign out, you may do "
                        a(href = "https://glados.nephy.jp/api/signout?path=/") {
                            +"here"
                        }
                        +"."
                    }
                }
            }
        }
    }
}

fun Route.getSoundsList() {
    get("/sounds/{guild}") {
        val guildId = call.parameters["guild"]?.toLongOrNull() ?: return@get call.respond(HttpStatusCode.NotFound)

        call.respondHtmlTemplate(NavLayout()) {
            navContent {
                table(classes = "table") {
                    thead {
                        tr {
                            th {
                                +"コマンド"
                            }
                            td {
                                +"試聴"
                            }
                        }
                    }
                    tbody {
                        SoundBot.listSounds().forEach { sound ->
                            tr {
                                th {
                                    span {
                                        +".${sound.fileName.toString().split(".").first()}"
                                    }
                                }
                                td {
                                    audio {
                                        attributes["preload"] = "none"
                                        controls = true

                                        source {
                                            src = "https://glados.nephy.jp/sounds/file/$guildId/${sound.fileName}"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Route.getSoundFile() {
    get("/sounds/file/{guild}/{filename}") {
        val guildId = call.parameters["guild"]?.toLongOrNull() ?: return@get call.respond(HttpStatusCode.NotFound)
        val filename = call.parameters["filename"] ?: return@get call.respond(HttpStatusCode.NotFound)
        val path = Paths.get("sounds", guildId.toString(), filename)
        if (!Files.exists(path)) {
            return@get call.respond(HttpStatusCode.NotFound)
        }

        call.respondFile(path.toFile())
    }
}

fun Route.login() {
    get("/api/login") {
        call.sessions.set(CallbackSession(call.parameters["path"]))
        call.respondRedirect("https://discordapp.com/api/oauth2/authorize?client_id=${config.clientId}&redirect_uri=${config.redirectUri.encodeURLParameter()}&response_type=code&scope=guilds%20identify")
    }
}

fun Route.signout() {
    get("/api/signout") {
        call.sessions.clear<DiscordOAuth2Session>()
        call.respondRedirect(CallbackSession(call.parameters["path"]).fullUrl)
    }
}

fun Route.loginCallback() {
    get("/api/login/callback") {
        val code = call.parameters["code"] ?: return@get call.respond(HttpStatusCode.NotFound)

        val json = httpClient.post<String>("https://discordapp.com/api/v6/oauth2/token") {
            userAgent(userAgent)
            body = object: OutgoingContent.WriteChannelContent() {
                override val contentType = ContentType.Application.FormUrlEncoded

                override suspend fun writeTo(channel: ByteWriteChannel) {
                    channel.writeStringUtf8("client_id=${config.clientId}&client_secret=${config.clientSecret}&grant_type=authorization_code&code=$code&redirect_uri=${config.redirectUri.encodeURLParameter()}")
                }
            }
        }
        val token = json.parse<DiscordAccessTokenResponse>()

        val callback = call.sessions.callback
        val session = DiscordOAuth2Session(token.accessToken, token.tokenType, token.expiresIn, token.scope, token.refreshToken)
        call.sessions.set(session)

        slack.message("#auth") {
            username("/v2/auth/discord/callback")
            icon(":desktop_computer:")
            text { ":bird: ${session.user.username}#${session.user.discriminator} がDiscord認証 (GLaDOS)を実行しました。 (${call.request.origin.remoteHost} -> ${callback.fullUrl} )" }
        }

        call.respondRedirect(callback.fullUrl)
    }
}

data class DiscordAccessTokenResponse(override val json: ImmutableJsonObject): JsonModel {
    val accessToken by string("access_token")
    val tokenType by string("token_type")
    val expiresIn by long("expires_in")
    val refreshToken by string("refresh_token")
    val scope by string
}

suspend fun PipelineContext<Unit, ApplicationCall>.notFound() {
    call.respondHtmlTemplate(NavLayout(), HttpStatusCode.NotFound) {
        navContent {
            div("alert alert-dismissible alert-danger") {
                h4 {
                    span("far fa-sad-tear")
                    +" 404 Page Not Found"
                }
                p { +"${call.request.httpMethod.value.toUpperCase()} ${call.request.path()}" }
            }
        }
    }
}
