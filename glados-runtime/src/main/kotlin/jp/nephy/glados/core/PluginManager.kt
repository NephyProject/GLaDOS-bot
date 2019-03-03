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
import jp.nephy.glados.api.annotations.TestOnlyFeature
import jp.nephy.glados.api.annotations.TestableFeature
import jp.nephy.glados.clients.logger.of
import jp.nephy.glados.clients.utils.fullName
import jp.nephy.glados.clients.utils.name
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.jvmErasure
import kotlin.streams.toList
import kotlin.system.measureTimeMillis

internal object PluginManager: ClassManager<Plugin>, CoroutineScope by GLaDOS {
    private val logger = Logger.of("GLaDOS.PluginManager")
    
    override fun loadAll() {
        val loadingTimeMillis = measureTimeMillis {
            val jobs = Files.walk(GLaDOS.config.paths.plugins).filter { 
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
        
        logger.info { "$loadingTimeMillis ms で Plugin のロードを完了しました。" }
    }

    override fun load(jarPath: Path) {
        logger.debug { "Jar: \"${jarPath.toAbsolutePath()}\" のロードを試みます。" }

        runCatching {
            loadClasses<Plugin>(jarPath)
        }.onSuccess { classes ->
            for (kotlinClass in classes) {
                load(kotlinClass)
            }
        }.onFailure { e ->
            logger.error(e) { "Jar: \"${jarPath.toAbsolutePath()}\" のロードに失敗しました。" }
        }
    }
    
    override fun load(kotlinClass: KClass<out Plugin>) {
        if (GLaDOS.isDebugMode && kotlinClass.findAnnotation<TestOnlyFeature>() == null && kotlinClass.findAnnotation<TestableFeature>() == null) {
            logger.error { "クラス: \"${kotlinClass.qualifiedName}\" はテスト可能ではありません。スキップします。" }
            return
        } else if (!GLaDOS.isDebugMode && kotlinClass.findAnnotation<TestOnlyFeature>() != null) {
            logger.info { "クラス: \"${kotlinClass.qualifiedName}\" はテスト環境でのみ実行できます。スキップします。" }
            return
        }
        
        val plugin = runCatching {
            kotlinClass.objectInstance ?: kotlinClass.createInstance()
        }.onSuccess { 
            if (kotlinClass.objectInstance == null) {
                logger.warn { "Plugin: \"${it.fullName}\" は object 宣言ではなく class 宣言されています。object 宣言が推奨されます。" }
            }
            
            logger.debug { "Plugin: \"${it.fullName}\" をロードしました。" }
        }.onFailure { e ->
            logger.error(e) { "クラス: \"${kotlinClass.qualifiedName}\" の初期化に失敗しました。" }
        }.getOrNull() ?: return

        val jobs = kotlinClass.declaredFunctions.map { function -> 
            launch {
                if (function.valueParameters.size != 1) {
                    logger.trace { "関数: \"${plugin.name}#${function.name}\" は引数の長さが 1 ではありません。スキップします。" }
                    return@launch
                }

                val eventClass = function.valueParameters.first().type.jvmErasure
                if (!eventClass.isSubclassOf(Event::class)) {
                    logger.trace { "関数: \"${plugin.name}#${function.name}\" の引数は jp.nephy.glados.api.Event を継承していません。スキップします。" }
                    return@launch
                }
                
                if (function.javaMethod?.isDefault == true) {
                    logger.trace { "Subscription: \"${plugin.name}#${function.name}\" はデフォルト実装です。スキップします。" }
                    return@launch
                }
                
                if (function.visibility != KVisibility.PUBLIC) {
                    logger.warn { "Subscription: \"${plugin.name}#${function.name}\" は public 宣言されていません。スキップします。" }
                    return@launch
                }

                if (function.returnType.jvmErasure != Unit::class) {
                    logger.warn { "Subscription: \"${plugin.name}#${function.name}\" は返り値の型が ${function.returnType} です。Unit が推奨されます。" }
                }
                
                for (client in ClientManager.clients) {
                    if (client.register(plugin, function, eventClass)) {
                        logger.debug { "Subscription: \"${plugin.name}#${function.name}\" は \"${client.name}\" に登録されました。" }
                    }
                }
            }
        }
        
        runBlocking { 
            jobs.joinAll()
        }
    }
}
