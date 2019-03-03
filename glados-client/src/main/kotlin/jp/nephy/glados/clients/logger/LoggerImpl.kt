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

package jp.nephy.glados.clients.logger

import ch.qos.logback.classic.Level
import jp.nephy.glados.api.*
import mu.KLogger
import mu.KotlinLogging
import java.util.logging.LogManager

internal class LoggerImpl(private val name: String, private val slackChannel: String?, private val slackIconEmoji: String?, private val useSlack: Boolean): Logger {
    companion object {
        init {
            LogManager.getLogManager().reset()
        }
    }
    
    private val backingLogger = KotlinLogging.logger(name)
    private val kLogger: KLogger
        get() {
            if (GLaDOS.initialized) {
                (backingLogger.underlyingLogger as ch.qos.logback.classic.Logger).level = GLaDOS.config.logging.level
            }
            
            return backingLogger
        }
    
    override val isTraceEnabled: Boolean
        get() = kLogger.isTraceEnabled
    
    override fun trace(message: () -> Any?) {
        kLogger.trace(message)
        slack(Level.TRACE, message)
    }

    override fun trace(throwable: Throwable, message: () -> Any?) {
        kLogger.trace(throwable, message)
        slack(Level.TRACE, throwable, message)
    }

    override val isDebugEnabled: Boolean
        get() = kLogger.isDebugEnabled
    
    override fun debug(message: () -> Any?) {
        kLogger.debug(message)
        slack(Level.DEBUG, message)
    }

    override fun debug(throwable: Throwable, message: () -> Any?) {
        kLogger.debug(throwable, message)
        slack(Level.DEBUG, throwable, message)
    }

    override val isInfoEnabled: Boolean
        get() = kLogger.isInfoEnabled
    
    override fun info(message: () -> Any?) {
        kLogger.info(message)
        slack(Level.INFO, message)
    }

    override fun info(throwable: Throwable, message: () -> Any?) {
        kLogger.info(throwable, message)
        slack(Level.INFO, throwable, message)
    }

    override val isWarnEnabled: Boolean
        get() = kLogger.isWarnEnabled
    
    override fun warn(message: () -> Any?) {
        kLogger.warn(message)
        slack(Level.WARN, message)
    }

    override fun warn(throwable: Throwable, message: () -> Any?) {
        kLogger.warn(throwable, message)
        slack(Level.WARN, throwable, message)
    }

    override val isErrorEnabled: Boolean
        get() = kLogger.isErrorEnabled
    
    override fun error(message: () -> Any?) {
        kLogger.error(message)
        slack(Level.ERROR, message)
    }

    override fun error(throwable: Throwable, message: () -> Any?) {
        kLogger.error(throwable, message)
        slack(Level.ERROR, throwable, message)
    }

    private fun isSlackEnabled(target: Level): Boolean {
        return when {
            !GLaDOS.initialized -> false
            !useSlack -> false
            GLaDOS.isDebugMode -> false
            !target.isGreaterOrEqual(GLaDOS.config.logging.levelForSlack) -> false
            else -> true
        }
    }
    
    private fun slack(level: Level, message: () -> Any?) {
        if (!isSlackEnabled(level)) {
            return
        }

        SlackWebhook.message(slackChannel ?: "#glados") {
            username = "[${level.levelStr.toLowerCase().capitalize()}] $name"
            icon = slackIconEmoji ?: ":desktop_computer:"
            text(message)
        }
    }
    
    private fun slack(level: Level, throwable: Throwable, message: () -> Any?) {
        slack(level) { "${message.toStringSafe()}\n${throwable.stackTraceString}" }
    }
}

internal fun (() -> Any?).toStringSafe(): String {
    return runCatching {
        invoke().toString()
    }.getOrNull().orEmpty()
}
