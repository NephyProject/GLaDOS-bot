package jp.nephy.glados.core

import jp.nephy.glados.api.GLaDOS
import jp.nephy.glados.api.plugin.GLaDOSPlugin
import jp.nephy.glados.api.plugin.TestOnly
import jp.nephy.glados.api.plugin.Testable
import mu.KotlinLogging
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaMethod
import kotlin.system.measureNanoTime

class PluginManager(private val instance: GLaDOS) {
    companion object {
        private const val nano = 1000000.0
    }
    
    private val logger = KotlinLogging.logger("GLaDOS.PluginManager")
    
    suspend fun loadAll() {
        val loadingTimeNano = measureNanoTime {
            
        }
        
        logger.info { "${String.format("%.3f", loadingTimeNano / nano)} ms で プラグインのロードを完了しました。" }
    }
    
    private fun load(pluginClass: KClass<out GLaDOSPlugin>) = sequence {
        if (instance.isDebugMode && !pluginClass.hasAnnotation<TestOnly>() && !pluginClass.hasAnnotation<Testable>()) {
            logger.error { "${pluginClass.qualifiedName} はテスト可能ではありません。スキップします。" }
            return@sequence
        } else if (!instance.isDebugMode && pluginClass.hasAnnotation<TestOnly>()) {
            logger.info { "${pluginClass.qualifiedName} はテスト環境でのみ実行できます。スキップします。" }
            return@sequence
        }
        
        val plugin = runCatching {
            pluginClass.objectInstance ?: pluginClass.createInstance().also {
                logger.warn { "${it.name} は object 宣言ではなく class 宣言されています。object 宣言が推奨されます。" }
            }
        }.onFailure { e ->
            logger.error(e.cause ?: e) { "${pluginClass.qualifiedName} のインスタンスの作成に失敗しました。" }
        }.getOrNull() ?: return@sequence
        
        for (function in pluginClass.declaredFunctions) {
            if (function.valueParameters.size != 1) {
                logger.debug { "${plugin.name}#${function.name} は引数の長さが 1 ではありません。" }
                continue
            }
            
            val parameterEventType = function.valueParameters.first().type
            if (instance.featureManager.events.none { it.createType() == parameterEventType }) {
                logger.debug { "${plugin.name}#${function.name} の引数は登録されていないイベントです。" }
                continue
            }

            if (function.javaMethod?.isDefault != false) {
                logger.debug { "${plugin.name}#${function.name} はデフォルト実装です。スキップします。" }
                continue
            }

            if (function.visibility != KVisibility.PUBLIC) {
                logger.warn { "${plugin.name}#${function.name} は public 宣言されていません。スキップします。" }
                continue
            }
            
            for (annotation in function.annotations) {
                val subscription = instance.featureManager.annotations[annotation.annotationClass]?.invoke(function) ?: continue
                yield(subscription)
            }
        }
    }
    
    private inline fun <reified T: Annotation> KAnnotatedElement.hasAnnotation(): Boolean {
        return findAnnotation<T>() != null
    }
}
