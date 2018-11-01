package jp.nephy.glados.core.plugins

import jp.nephy.glados.core.Logger
import jp.nephy.glados.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.net.JarURLConnection
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.kotlinFunction
import kotlin.system.measureTimeMillis

object PluginManager {
    private const val packagePrefix = "jp.nephy.glados.plugins"
    private val logger = Logger("GLaDOS.PluginManager", false)

    suspend fun loadAll() {
        val loadingTimeMs = measureTimeMillis {
            FeatureClassLoader.classes<Plugin>().map {
                GlobalScope.launch(dispatcher) {
                    val instance = try {
                        it.objectInstance ?: it.createInstance()
                    } catch (e: Exception) {
                        logger.error(e) { "${it.qualifiedName} のインスタンスの作成に失敗しました。" }
                        return@launch
                    }

                    ClassLoader(instance).load()
                }
            }.forEach {
                it.join()
            }

            SubscriptionClient.ListenerEvent.sort()
            SubscriptionClient.Command.sort()
            SubscriptionClient.Loop.sort()
            SubscriptionClient.Schedule.sort()
        }

        logger.info { "${loadingTimeMs}ms で Plugin のロードを完了しました。" }
    }

    private class ClassLoader(private val instance: Plugin) {
        companion object {
            private val listenerAdapterMethods = EventModel::class.java.declaredMethods.filter { it.isAnnotationPresent(EventModel.FromListenerAdapter::class.java) }.mapNotNull { it.kotlinFunction }
            private val connectionListenerMethods = EventModel::class.java.declaredMethods.filter { it.isAnnotationPresent(EventModel.FromConnectionListener::class.java) }.mapNotNull { it.kotlinFunction }
            private val audioEventAdapterMethods = EventModel::class.java.declaredMethods.filter { it.isAnnotationPresent(EventModel.FromAudioEventAdapter::class.java) }.mapNotNull { it.kotlinFunction }
        }

        fun load() {
            instance.javaClass.declaredMethods.asSequence().filter { !it.isDefault }.mapNotNull { it.kotlinFunction }.forEach {
                FunctionLoader(it).load()
            }
        }

        private inner class FunctionLoader(private val function: KFunction<*>) {
            private val valueParameters = function.valueParameters

            fun load() {
                val annotation = if (function.annotations.isNotEmpty()) {
                    function.findAnnotation<Plugin.Command>()
                            ?: function.findAnnotation<Plugin.Loop>()
                            ?: function.findAnnotation<Plugin.Schedule>()
                            ?: function.findAnnotation<Plugin.Event>()
                } else {
                    null
                }

                when (annotation) {
                    is Plugin.Command -> {
                        loadCommand(annotation)
                    }
                    is Plugin.Loop -> {
                        loadLoop(annotation)
                    }
                    is Plugin.Schedule -> {
                        loadSchedule(annotation)
                    }
                    else -> {
                        loadEventListener(annotation as? Plugin.Event)
                    }
                }
            }

            private fun loadCommand(annotation: Plugin.Command) {
                if (valueParameters.size == 1 && valueParameters.first().type == Plugin.Command.Event::class.createType()) {
                    SubscriptionClient.Command.subscriptions += Subscription.Command(annotation, instance, function).also {
                        logger.info { "Command: ${it.fullname} をロードしました。" }
                    }
                } else {
                    logger.warn { "[${instance.name}#${function.name}] @Command が付与されていますが, 引数が [Plugin.Command.Event] ではありません。スキップします。" }
                }
            }

            private fun loadLoop(annotation: Plugin.Loop) {
                if (valueParameters.isEmpty()) {
                    SubscriptionClient.Loop.subscriptions += Subscription.Loop(annotation, instance, function).also {
                        logger.info { "Loop: ${it.fullname} をロードしました。" }
                    }
                } else {
                    logger.warn { "[${instance.name}#${function.name}] @Loop が付与されていますが, 引数の数は 0 である必要があります。スキップします。" }
                }
            }

            private fun loadSchedule(annotation: Plugin.Schedule) {
                if (valueParameters.isEmpty()) {
                    SubscriptionClient.Schedule.subscriptions += Subscription.Schedule(annotation, instance, function).also {
                        logger.info { "Schedule: ${it.fullname} をロードしました。" }
                    }
                } else {
                    logger.warn { "[${instance.name}#${function.name}] @Schedule が付与されていますが, 引数の数は 0 である必要があります。スキップします。" }
                }
            }

            private fun loadEventListener(annotation: Plugin.Event?) {
                when {
                    function satisfies listenerAdapterMethods -> {
                        SubscriptionClient.ListenerEvent.subscriptions += Subscription.Event(annotation ?: defaultEventAnnotation, instance, function).also {
                            logger.info { "JDAEvent: ${it.fullname} をロードしました。" }
                        }
                    }
                    function satisfies connectionListenerMethods -> {
                        SubscriptionClient.ConnectionEvent.subscriptions += Subscription.Event(annotation ?: defaultEventAnnotation, instance, function).also {
                            logger.info { "ConnectionEvent: ${it.fullname} をロードしました。" }
                        }
                    }
                    function satisfies audioEventAdapterMethods -> {
                        SubscriptionClient.AudioEvent.subscriptions += Subscription.Event(annotation ?: defaultEventAnnotation, instance, function).also {
                            logger.info { "AudioEvent: ${it.fullname} をロードしました。" }
                        }
                    }
                }
            }

            private infix fun KFunction<*>.satisfies(originalFunctions: List<KFunction<*>>): Boolean {
                originalFunctions.filter { it.valueParameters.size == valueParameters.size }.find { originalFunction ->
                    originalFunction.valueParameters.zip(valueParameters).all {
                        it.first.type == it.second.type
                    }
                } ?: return false

                return true
            }
        }
    }

    private object FeatureClassLoader {
        val classLoader = Thread.currentThread().contextClassLoader!!
        val classNamePattern = "([A-Za-z]+(\\d)?)\\.class".toRegex()
        const val packageSeparator = '.'
        const val jarPathSeparator = '/'
        val fileSystemPathSeparator = File.separatorChar
        val jarResourceName = packagePrefix.replace('.', jarPathSeparator)
        val fileSystemResourceName = packagePrefix.replace('.', fileSystemPathSeparator)

        @Suppress("UNCHECKED_CAST")
        inline fun <reified T: Any> classes(): Sequence<KClass<T>> {
            val root = classLoader.getResource(fileSystemResourceName)
                    ?: classLoader.getResource(jarResourceName)
                    ?: return emptySequence()

            return when (root.protocol) {
                "file" -> {
                    val paths = arrayListOf<Path>()
                    Files.walkFileTree(Paths.get(root.toURI()), object: SimpleFileVisitor<Path>() {
                        override fun visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult {
                            paths.add(path)
                            return FileVisitResult.CONTINUE
                        }
                    })

                    paths.asSequence().map {
                        ClassEntry(it.toString(), it.fileName.toString())
                    }.loadClasses(fileSystemResourceName, fileSystemPathSeparator)
                }
                "jar" -> {
                    (root.openConnection() as JarURLConnection).jarFile.use {
                        it.entries().toList()
                    }.asSequence().filter {
                        it.name.startsWith(jarResourceName)
                    }.map {
                        ClassEntry(it.name, it.name.split(jarPathSeparator).last())
                    }.loadClasses<T>(jarResourceName, jarPathSeparator)
                }
                else -> throw UnsupportedOperationException("Unknown procotol: ${root.protocol}")
            }.sortedBy { it.canonicalName }.map { it.kotlin }
        }

        data class ClassEntry(val path: String, val filename: String)

        @Suppress("UNCHECKED_CAST")
        inline fun <reified T> Sequence<ClassEntry>.loadClasses(resourceName: String, pathSeparator: Char): Sequence<Class<T>> {
            return filter { classNamePattern.containsMatchIn(it.filename) }
                    .asSequence()
                    .map { "$packagePrefix${it.path.split(resourceName).last().replace(pathSeparator, packageSeparator).removeSuffix(".class")}" }
                    .map { classLoader.loadClass(it) }
                    .filter { it.superclass == T::class.java }
                    .map { it as Class<T> }
        }
    }

    @Plugin.Event private fun getEventAnnotation() {}
    private val defaultEventAnnotation = this::getEventAnnotation.findAnnotation<Plugin.Event>()!!
}
