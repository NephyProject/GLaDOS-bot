package jp.nephy.glados.core.wui

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.sessions.SessionStorageMemory
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import jp.nephy.glados.core.Logger
import org.slf4j.event.Level

fun Application.module() {
    install(CallLogging) {
        level = Level.INFO
    }

    install(DefaultHeaders) {
        header(HttpHeaders.Server, "GLaDOS-bot")
    }

    install(StatusPages) {
        val logger = Logger("GLaDOS.WebUI")

        exception<Exception> { e ->
            logger.error(e) { "Internal server error occurred." }
            call.respond(HttpStatusCode.InternalServerError)
        }

        status(HttpStatusCode.NotFound) {
            notFound()
        }
    }

    install(Sessions) {
        cookie<DiscordOAuth2Session>("DiscordOAuth2", SessionStorageMemory()) {
            cookie.path = "/"
        }
        cookie<CallbackSession>("Callback", SessionStorageMemory()) {
            cookie.path = "/"
        }
    }

    install(XForwardedHeaderSupport)

    install(Routing) {
        getTop()
        getDashboard()
        getSoundsList()
        getSoundFile()

        login()
        signout()
        loginCallback()
    }
}
