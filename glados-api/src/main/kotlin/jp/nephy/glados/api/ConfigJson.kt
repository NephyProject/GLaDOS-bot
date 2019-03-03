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

package jp.nephy.glados.api

import ch.qos.logback.classic.Level
import jp.nephy.jsonkt.delegation.*
import java.nio.file.Path

/**
 * GLaDOS config.json model.
 */
interface ConfigJson: JsonModel {
    /**
     * GLaDOS coroutine dispatcher parallelism.
     */
    val parallelism: Int

    /**
     * GLaDOS HttpClient UserAgent.
     */
    val userAgent: String
    
    /**
     * Paths directive.
     */
    val paths: Paths

    /**
     * Paths model.
     */
    interface Paths: JsonModel {
        /**
         * tmp directory.
         */
        val tmp: Path

        /**
         * resources directory.
         */
        val resources: Path

        /**
         * GLaDOS SubscriptionClient jars directory.
         */
        val clients: Path

        /**
         * GLaDOS Plugin jars directory.
         */
        val plugins: Path
    }

    /**
     * Logging directive.
     */
    val logging: Logging

    /**
     * Logging model.
     */
    interface Logging: JsonModel {
        /**
         * Log level of GLaDOS logger.
         */
        val level: Level

        /**
         * Log level of Slack logger.
         */
        val levelForSlack: Level

        /**
         * Slack incoming webhook url.
         */
        val slackWebhookUrl: String?
    }
}
