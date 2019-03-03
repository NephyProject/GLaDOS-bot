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

package jp.nephy.glados.clients.twitter

import io.ktor.client.engine.cio.CIO
import io.ktor.util.KtorExperimentalAPI
import jp.nephy.glados.api.Plugin
import jp.nephy.glados.clients.GLaDOSSubscriptionClient
import jp.nephy.glados.clients.twitter.config.TwitterAccount
import jp.nephy.glados.clients.twitter.config.client
import jp.nephy.glados.clients.twitter.event.*
import jp.nephy.glados.clients.utils.eventClass
import jp.nephy.glados.clients.utils.invoke
import jp.nephy.glados.clients.utils.subscriptions
import jp.nephy.jsonkt.*
import jp.nephy.penicillin.endpoints.stream
import jp.nephy.penicillin.extensions.endpoints.TweetstormListener
import jp.nephy.penicillin.extensions.endpoints.tweetstorm
import jp.nephy.penicillin.extensions.listen
import jp.nephy.penicillin.models.DirectMessage
import jp.nephy.penicillin.models.Status
import jp.nephy.penicillin.models.Stream
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf

object TwitterSubscriptionClient: GLaDOSSubscriptionClient<TwitterEvent, TwitterEventBase, TwitterSubscription>() {
    override fun create(plugin: Plugin, function: KFunction<*>, eventClass: KClass<*>): TwitterSubscription? {
        if (!eventClass.isSubclassOf(TwitterEventBase::class)) {
            return null
        }

        val annotation = function.findAnnotation() ?: defaultTwitterEventAnnotation
        return TwitterSubscription(plugin, function, annotation)
    }
    
    private val jobs = ConcurrentHashMap<TwitterAccount, Job>()
    
    override fun start() {
        subscriptions.flatMap { it.accounts }.distinct().forEach { account ->
            account.startJob()
        }
    }

    @UseExperimental(KtorExperimentalAPI::class)
    private fun TwitterAccount.startJob() {
        jobs[this] = launch {
            while (isActive) {
                try {
                    client(engine = CIO).use {
                        // TODO
                        it.stream.tweetstorm().listen(createListener(this@startJob)).await(autoReconnect = false)
                    }
                } catch (e: CancellationException) {
                    delay(5000)
                } catch (e: Throwable) {
                    logger.error(e) { "例外が発生しました。(@${user.screenName})" }
                }
            }

            logger.trace { "終了しました。(@${user.screenName})" }
        }

        logger.trace { "開始しました。(@${user.screenName})" }
    }

    override fun stop() {
        for (account in jobs.keys) {
            account.stopJob()
        }
    }
    
    private fun TwitterAccount.stopJob() {
        jobs[this]?.cancel()
    }

    override fun onSubscriptionLoaded(subscription: TwitterSubscription) {
        subscription.accounts.filter { 
            !jobs.containsKey(it) 
        }.forEach { account ->
            account.startJob()
        }
    }

    override fun onSubscriptionUnloaded(subscription: TwitterSubscription) {
        subscription.accounts.filter {  account ->
            subscriptions.none { account in it.accounts }
        }.forEach { account ->
            account.stopJob()
        }
    }
    
    private fun createListener(account: TwitterAccount) = object: TweetstormListener {
        override suspend fun onConnect() {
            runEvent(ConnectEvent(account))

            logger.info { "Tweetstorm (@${account.user.screenName}) が開始されました。" }
        }

        override suspend fun onDisconnect() {
            runEvent(DisconnectEvent(account))

            logger.warn { "Tweetstorm (@${account.user.screenName}) から切断されました。" }
        }

        override suspend fun onStatus(status: Status) {
            runEvent(StatusEvent(account, status))
        }

        override suspend fun onDirectMessage(message: DirectMessage) {
            runEvent(DirectMessageEvent(account, message))
        }

        override suspend fun onFriends(friends: Stream.Friends) {
            runEvent(FriendsEvent(account, friends))
        }

        override suspend fun onDelete(delete: Stream.Delete) {
            runEvent(DeleteEvent(account, delete))
        }

        override suspend fun onHeartbeat() {
            runEvent(HeartbeatEvent(account))
        }

        override suspend fun onLength(length: Int) {
            runEvent(LengthEvent(account, length))
        }

        override suspend fun onAnyJson(json: JsonObject) {
            runEvent(AnyJsonEvent(account, json))
        }

        override suspend fun onUnhandledJson(json: JsonObject) {
            runEvent(UnhandledJsonEvent(account, json))
        }

        override suspend fun onUnknownData(data: String) {
            runEvent(UnknownDataEvent(account, data))
        }

        override suspend fun onRawData(data: String) {
            runEvent(RawDataEvent(account, data))
        }

        private fun runEvent(event: TwitterEventBase) {
            subscriptions.filter { event.account in it.accounts && it.eventClass == event::class }.forEach {
                launch {
                    it.invoke(event)
                    it.logger.trace { "実行されました。(@${event.account.user.screenName})" }
                }
            }
        }
    }
}
