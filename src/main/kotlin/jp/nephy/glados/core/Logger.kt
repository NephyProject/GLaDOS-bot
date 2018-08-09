package jp.nephy.glados.core

import jp.nephy.glados.isDebugMode
import jp.nephy.glados.secret
import jp.nephy.utils.SlackWebhook
import jp.nephy.utils.logger
import jp.nephy.utils.stackTraceString
import org.slf4j.event.Level

class Logger(private val name: String, private val useSlack: Boolean = true) {
    private val slack = SlackWebhook(secret.forKey("slack_webhook_url"))
    private val logger = logger(name)

    private fun (() -> Any).toStringSafe() = try {
        invoke().toString()
    } catch (e: Exception) {
        ""
    }

    fun debug(msg: () -> Any) {
        if (logger.isDebugEnabled) {
            val message = msg.toStringSafe()
            logger.debug(message)
            slack(Level.DEBUG, message)
        }
    }

    fun debug(throwable: Throwable, msg: () -> Any) {
        if (logger.isDebugEnabled) {
            val message = "${msg.toStringSafe()}\n${throwable.stackTraceString}"
            logger.debug(message)
            slack(Level.DEBUG, message)
        }
    }

    fun info(msg: () -> Any) {
        if (logger.isInfoEnabled) {
            val message = msg.toStringSafe()
            logger.info(message)
            slack(Level.INFO, message)
        }
    }

    fun warn(msg: () -> Any) {
        if (logger.isWarnEnabled) {
            val message = msg.toStringSafe()
            logger.warn(message)
            slack(Level.WARN, message)
        }
    }

    fun warn(throwable: Throwable, msg: () -> Any) {
        if (logger.isWarnEnabled) {
            val message = "${msg.toStringSafe()}\n${throwable.stackTraceString}"
            logger.warn(message)
            slack(Level.WARN, message)
        }
    }

    fun error(msg: () -> Any) {
        if (logger.isErrorEnabled) {
            val message = msg.toStringSafe()
            logger.error(message)
            slack(Level.ERROR, message)
        }
    }

    fun error(throwable: Throwable, msg: () -> Any) {
        if (logger.isErrorEnabled) {
            val message = "${msg.toStringSafe()}\n${throwable.stackTraceString}"
            logger.error(message)
            slack(Level.ERROR, message)
        }
    }

    private fun slack(level: Level, message: String) {
        if (isDebugMode || !useSlack) {
            return
        }

        slack.message("#glados") {
            username("[${level.name}] $name")
            icon(":desktop_computer:")
            text(message)
        }
    }
}
