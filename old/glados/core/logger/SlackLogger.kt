package jp.nephy.glados.core.logger

import ch.qos.logback.classic.Level
import jp.nephy.glados.GLaDOS
import jp.nephy.glados.core.plugins.extensions.stackTraceString
import mu.KLogger
import mu.KotlinLogging
import java.util.logging.LogManager

private typealias Message = () -> Any?

private fun Message.toStringSafe(): String {
    return runCatching {
        invoke().toString()
    }.getOrDefault("")
}

private var KLogger.logLevel
    get() = (underlyingLogger as ch.qos.logback.classic.Logger).level
    set(value) {
        (underlyingLogger as ch.qos.logback.classic.Logger).level = value
    }

class SlackLogger(private val name: String, private val channelName: String = "#glados") {
    companion object {
        init {
            LogManager.getLogManager().reset()
        }
    }

    private val underlyingLogger = KotlinLogging.logger(name)

    init {
        underlyingLogger.logLevel = GLaDOS.config.logLevel
    }

    fun trace(message: Message) {
        if (underlyingLogger.isTraceEnabled) {
            underlyingLogger.trace(message)
        }
    }

    fun debug(message: Message) {
        if (underlyingLogger.isDebugEnabled) {
            underlyingLogger.debug(message)
            slack(Level.DEBUG, message)
        }
    }

    fun info(message: Message) {
        if (underlyingLogger.isInfoEnabled) {
            underlyingLogger.info(message)
            slack(Level.INFO, message)
        }
    }

    fun warn(message: Message) {
        if (underlyingLogger.isWarnEnabled) {
            underlyingLogger.warn(message)
            slack(Level.WARN, message)
        }
    }

    fun warn(throwable: Throwable, message: Message) {
        if (underlyingLogger.isWarnEnabled) {
            underlyingLogger.warn(throwable, message)
            slack(Level.WARN) { "${message.toStringSafe()}\n${throwable.stackTraceString}" }
        }
    }

    fun error(message: Message) {
        if (underlyingLogger.isErrorEnabled) {
            underlyingLogger.error(message)
            slack(Level.ERROR, message)
        }
    }

    fun error(throwable: Throwable, message: Message) {
        if (underlyingLogger.isErrorEnabled) {
            underlyingLogger.error(throwable, message)
            slack(Level.ERROR) { "${message.toStringSafe()}\n${throwable.stackTraceString}" }
        }
    }

    private fun slack(level: Level, message: Message) {
        if (GLaDOS.isDebugMode || !level.isGreaterOrEqual(GLaDOS.config.logLevelForSlack)) {
            return
        }

        SlackWebhook.message(channelName) {
            username("[${level.levelStr.toLowerCase().capitalize()}] $name")
            icon(":desktop_computer:")
            text(message.toStringSafe())
        }
    }
}
