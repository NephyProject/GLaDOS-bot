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

package jp.nephy.glados.core

import io.ktor.util.extension
import jp.nephy.glados.api.*
import jp.nephy.glados.clients.logger.of
import jp.nephy.glados.clients.utils.name
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.core.Closeable
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.streams.toList
import kotlin.system.measureTimeMillis

internal object ClientManager: ClassManager<SubscriptionClient<*, *, *>>, CoroutineScope by GLaDOS, Closeable {
    private val logger = Logger.of("GLaDOS.ClientManager")
    
    val clients = CopyOnWriteArraySet<SubscriptionClient<*, *, *>>()
    
    override fun loadAll() {
        check(clients.isEmpty()) {
            "既に SubscriptionClient はロードされているため, 一括ロードできません。"
        }

        val loadingTimeMillis = measureTimeMillis {
            val jobs = Files.walk(GLaDOS.config.paths.clients).filter { 
                it.extension == "jar"
            }.map { 
                launch { 
                    load(it)
                }
            }.toList()

            runBlocking {
                jobs.joinAll()
            }
        }
        
        logger.info { "$loadingTimeMillis ms で SubscriptionClient のロードが完了しました。" }
    }

    override fun load(jarPath: Path) {
        logger.debug { "Jar: \"${jarPath.toAbsolutePath()}\" のロードを試みます。" }
        
        runCatching {
            loadClasses<SubscriptionClient<*, *, *>>(jarPath)
        }.onSuccess { classes -> 
            for (kotlinClass in classes) {
                load(kotlinClass)
            }
        }.onFailure { e ->
            logger.error(e) { "Jar: \"${jarPath.toAbsolutePath()}\" のロードに失敗しました。" }
        }
    }

    override fun load(kotlinClass: KClass<out SubscriptionClient<*, *, *>>) {
        runCatching {
            kotlinClass.objectInstance ?: kotlinClass.createInstance()
        }.onSuccess { client ->
            clients += client
            logger.debug { "SubscriptionClient: \"${client.name}\" をロードしました。" }
        }.onFailure { e ->
            logger.error(e) { "クラス: \"${kotlinClass.qualifiedName}\" の初期化に失敗しました。" }
        }
    }
    
    fun startAll() {
        val jobs = clients.map { client ->
            launch {
                runCatching {
                    client.start()
                }.onSuccess {
                    client.logger.info { "開始しました。" }
                }.onFailure { e ->
                    client.logger.error(e) { "開始中に例外が発生しました。" }
                }
            }
        }
        
        runBlocking { 
            jobs.joinAll()
        }
    }

    override fun close() {
        logger.info { "ClientManager を終了します。" }

        val jobs = clients.map { client ->
            launch {
                runCatching {
                    client.stop()
                }.onSuccess {
                    client.logger.info { "終了しました。" }
                }.onFailure { e ->
                    client.logger.error(e) { "終了中に例外が発生しました。" }
                }
            }
        }

        runBlocking {
            jobs.joinAll()
        }
        
        clients.clear()
    }
}
