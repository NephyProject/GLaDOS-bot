package jp.nephy.glados.core.logger

import ch.qos.logback.classic.Level
import jp.nephy.glados.config
import jp.nephy.glados.core.extensions.stackTraceString
import jp.nephy.glados.isDebugMode
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

        var logLevel = config.logLevel
        var logLevelForSlack = config.logLevelForSlack
    }

    private val underlyingLogger = KotlinLogging.logger(name)
    private val logger: KLogger
        get() {
            underlyingLogger.logLevel = logLevel
            return underlyingLogger
        }

    fun trace(message: Message) {
        if (logger.isTraceEnabled) {
            logger.trace(message)
        }
    }

    fun debug(message: Message) {
        if (logger.isDebugEnabled) {
            logger.debug(message)
            slack(Level.DEBUG, message)
        }
    }

    fun info(message: Message) {
        if (logger.isInfoEnabled) {
            logger.info(message)
            slack(Level.INFO, message)
        }
    }

    fun warn(message: Message) {
        if (logger.isWarnEnabled) {
            logger.warn(message)
            slack(Level.WARN, message)
        }
    }

    fun warn(throwable: Throwable, message: Message) {
        if (logger.isWarnEnabled) {
            logger.warn(throwable, message)
            slack(Level.WARN) { "${message.toStringSafe()}\n${throwable.stackTraceString}" }
        }
    }

    fun error(message: Message) {
        if (logger.isErrorEnabled) {
            logger.error(message)
            slack(Level.ERROR, message)
        }
    }

    fun error(throwable: Throwable, message: Message) {
        if (logger.isErrorEnabled) {
            logger.error(throwable, message)
            slack(Level.ERROR) { "${message.toStringSafe()}\n${throwable.stackTraceString}" }
        }
    }

    private fun slack(level: Level, message: Message) {
        if (isDebugMode || !level.isGreaterOrEqual(logLevelForSlack)) {
            return
        }

        jp.nephy.glados.slack.message(channelName) {
            username("[${level.levelStr.toLowerCase().capitalize()}] $name")
            icon(":desktop_computer:")
            text(message.toStringSafe())
        }
    }
}
