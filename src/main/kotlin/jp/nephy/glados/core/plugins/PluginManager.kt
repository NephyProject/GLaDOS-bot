package jp.nephy.glados.core.plugins

import jp.nephy.glados.GLaDOS
import jp.nephy.glados.core.logger.SlackLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.net.JarURLConnection
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.*
import kotlin.reflect.jvm.kotlinFunction
import kotlin.system.measureNanoTime

object PluginManager: CoroutineScope {
    private const val nano = 1000000.0
    private val logger = SlackLogger("GLaDOS.PluginManager")

    override val coroutineContext: CoroutineContext
        get() = GLaDOS.dispatcher

    suspend fun loadAll() {
        var success = 0
        var warn = 0
        var error = 0
        var total = 0

        val loadingTimeNano = measureNanoTime {
            for (packagePrefix in GLaDOS.config.pluginsPackagePrefixes) {
                FeatureClassLoader(packagePrefix).classes<Plugin>().map {
                    launch {
                        try {
                            total++
                            if (GLaDOS.isDebugMode && it.findAnnotation<Plugin.TestOnly>() == null && it.findAnnotation<Plugin.Testable>() == null) {
                                logger.error { "${it.qualifiedName} はテスト可能ではありません。スキップします。" }
                                error++
                                return@launch
                            } else if (!GLaDOS.isDebugMode && it.findAnnotation<Plugin.TestOnly>() != null) {
                                logger.info { "${it.qualifiedName} はテスト環境でのみ実行できます。スキップします。" }
                                return@launch
                            }

                            val instance = it.objectInstance ?: it.createInstance().also {
                                logger.warn { "${it.fullname} は object宣言ではなく class宣言されています。object宣言が推奨されています。" }
                                warn++
                            }
                            ClassLoader(instance).load()
                            success++
                        } catch (e: Throwable) {
                            logger.error(e.cause ?: e) { "${it.qualifiedName} のインスタンスの作成に失敗しました。" }
                            error++
                        }
                    }
                }.forEach {
                    it.join()
                }
            }
        }

        logger.info { "${String.format("%.3f", loadingTimeNano / nano)} ms で Plugin のロードを完了しました。\n全Plugin: $total (ロード成功: $success / 警告: $warn / エラー: $error)" }
    }

    private class ClassLoader(private val instance: Plugin) {
        fun load() {
            instance.javaClass.declaredMethods.asSequence().filter { !it.isDefault }.mapNotNull { it.kotlinFunction }.forEach {
                FunctionLoader(it).load()
            }
        }

        private inner class FunctionLoader(private val function: KFunction<*>) {
            fun load() {
                when (val annotation = function.annotations.firstOrNull()) {
                    is Plugin.Command -> {
                        loadCommand(annotation)
                    }
                    is Plugin.Loop -> {
                        loadLoop(annotation)
                    }
                    is Plugin.Schedule -> {
                        loadSchedule(annotation)
                    }
                    is Plugin.Tweetstorm -> {
                        loadTweetstorm(annotation)
                    }
                    is Plugin.Web.Page -> {
                        loadWebPage(annotation)
                    }
                    is Plugin.Web.ErrorPage -> {
                        loadWebErrorPage(annotation)
                    }
                    is Plugin.Web.Session -> {
                        loadWebSession(annotation)
                    }
                    else -> {
                        loadEventListener(annotation as? Plugin.Event)
                    }
                }
            }

            private fun KFunction<*>.isPublic(): Boolean {
                return (visibility == KVisibility.PUBLIC).also {
                    if (!it) {
                        logger.warn { "${instance.fullname}#$name は public 宣言されていません。スキップします。" }
                    }
                }
            }

            private fun loadCommand(annotation: Plugin.Command) {
                if (function.valueParameters.size == 1 && function.valueParameters.first().type == Plugin.Command.Event::class.createType()) {
                    if (function.isPublic()) {
                        SubscriptionClient.Command += Subscription.Command(annotation, instance, function).also {
                            logger.trace { "Command: ${it.fullname} をロードしました。" }
                            if (it.description == null) {
                                logger.warn { "Command: ${it.fullname} は description が設定されていません。" }
                            }
                        }
                    }
                } else {
                    logger.warn { "[${instance.name}#${function.name}] @Command が付与されていますが, 引数が [Plugin.Command.Event] ではありません。スキップします。" }
                }
            }

            private fun loadLoop(annotation: Plugin.Loop) {
                if (function.valueParameters.isEmpty()) {
                    if (function.isPublic()) {
                        SubscriptionClient.Loop += Subscription.Loop(annotation, instance, function).also {
                            logger.trace { "Loop: ${it.fullname} をロードしました。" }
                        }
                    }
                } else {
                    logger.warn { "[${instance.name}#${function.name}] @Loop が付与されていますが, 引数の数は 0 である必要があります。スキップします。" }
                }
            }

            private fun loadSchedule(annotation: Plugin.Schedule) {
                if (function.valueParameters.isEmpty()) {
                    if (function.isPublic()) {
                        SubscriptionClient.Schedule += Subscription.Schedule(annotation, instance, function).also {
                            logger.trace { "Schedule: ${it.fullname} をロードしました。" }
                        }
                    }
                } else {
                    logger.warn { "[${instance.name}#${function.name}] @Schedule が付与されていますが, 引数の数は 0 である必要があります。スキップします。" }
                }
            }

            private fun loadTweetstorm(annotation: Plugin.Tweetstorm) {
                if (function.satisfies<EventModel.FromTweetstorm>()) {
                    if (function.isPublic()) {
                        SubscriptionClient.Tweetstorm += Subscription.Tweetstorm(annotation, instance, function).also {
                            logger.trace { "TweetstormEvent: ${it.fullname} をロードしました。" }
                        }
                    }
                } else {
                    logger.warn { "[${instance.name}#${function.name}] @Tweetstorm が付与されていますが, 引数が一致していません。スキップします。" }
                }
            }

            private fun loadWebPage(annotation: Plugin.Web.Page) {
                if (function.satisfies<EventModel.FromWebPage>()) {
                    if (function.isPublic()) {
                        SubscriptionClient.Web.Page += Subscription.Web.Page(annotation, instance, function).also {
                            logger.trace { "WebPage: ${it.fullname} をロードしました。" }
                        }
                    }
                } else {
                    logger.warn { "[${instance.name}#${function.name}] @Web.Page が付与されていますが, 引数が一致していません。スキップします。" }
                }
            }

            private fun loadWebErrorPage(annotation: Plugin.Web.ErrorPage) {
                if (function.satisfies<EventModel.FromWebErrorPage>()) {
                    if (function.isPublic()) {
                        SubscriptionClient.Web.ErrorPage += Subscription.Web.ErrorPage(annotation, instance, function).also {
                            logger.trace { "WebErrorPage: ${it.fullname} をロードしました。" }
                        }
                    }
                } else {
                    logger.warn { "[${instance.name}#${function.name}] @Web.ErrorPage が付与されていますが, 引数が一致していません。スキップします。" }
                }
            }

            private fun loadWebSession(annotation: Plugin.Web.Session) {
                if (function.satisfies<EventModel.FromWebSession>()) {
                    if (function.isPublic()) {
                        SubscriptionClient.Web.Session += Subscription.Web.Session(annotation, instance, function).also {
                            logger.trace { "WebSession: ${it.fullname} をロードしました。" }
                        }
                    }
                } else {
                    logger.warn { "[${instance.name}#${function.name}] @Web.Session が付与されていますが, 引数が一致していません。スキップします。" }
                }
            }

            private fun loadEventListener(annotation: Plugin.Event?) {
                when {
                    function.satisfies<EventModel.FromListenerAdapter>() -> {
                        if (function.isPublic()) {
                            SubscriptionClient.ListenerEvent += Subscription.Event(annotation ?: defaultEventAnnotation, instance, function).also {
                                logger.trace { "JDAEvent: ${it.fullname} をロードしました。" }
                            }
                        }
                    }
                    function.satisfies<EventModel.FromConnectionListener>() -> {
                        if (function.isPublic()) {
                            SubscriptionClient.ConnectionEvent += Subscription.Event(annotation ?: defaultEventAnnotation, instance, function).also {
                                logger.trace { "ConnectionEvent: ${it.fullname} をロードしました。" }
                            }
                        }
                    }
                    function.satisfies<EventModel.FromAudioEventAdapter>() -> {
                        if (function.isPublic()) {
                            SubscriptionClient.AudioEvent += Subscription.Event(annotation ?: defaultEventAnnotation, instance, function).also {
                                logger.trace { "AudioEvent: ${it.fullname} をロードしました。" }
                            }
                        }
                    }
                    function.satisfies<EventModel.FromAudioReceiveHandler>() -> {
                        if (function.isPublic()) {
                            SubscriptionClient.ReceiveAudio += Subscription.Event(annotation ?: defaultEventAnnotation, instance, function).also {
                                logger.trace { "ReceiveAudio: ${it.fullname} をロードしました。" }
                            }
                        }
                    }
                }
            }

            private inline fun <reified T: Annotation> KFunction<*>.satisfies(): Boolean {
                EventModel::class.declaredFunctions.find { originalFunction ->
                    originalFunction.valueParameters.size == valueParameters.size && originalFunction.findAnnotation<T>() != null && originalFunction.valueParameters.zip(valueParameters).all {
                        it.first.type == it.second.type
                    }
                } ?: return false

                return true
            }
        }
    }

    private class FeatureClassLoader(private val packagePrefix: String) {
        companion object {
            private const val packageSeparator = '.'
            private const val jarPathSeparator = '/'
        }

        val classLoader = Thread.currentThread().contextClassLoader!!
        val classNamePattern = "([A-Za-z]+(\\d)?)\\.class".toRegex()
        val fileSystemPathSeparator = File.separatorChar
        val jarResourceName = packagePrefix.replace('.', jarPathSeparator)
        val fileSystemResourceName = packagePrefix.replace('.', fileSystemPathSeparator)

        @Suppress("UNCHECKED_CAST")
        inline fun <reified T: Any> classes(): Sequence<KClass<T>> {
            val root = classLoader.getResource(jarResourceName) ?: classLoader.getResource(fileSystemResourceName) ?: return emptySequence()

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
                else -> {
                    throw UnsupportedOperationException("Unknown protocol: ${root.protocol}")
                }
            }.sortedBy { it.canonicalName }.map { it.kotlin }
        }

        data class ClassEntry(val path: String, val filename: String)

        @Suppress("UNCHECKED_CAST")
        inline fun <reified T> Sequence<ClassEntry>.loadClasses(resourceName: String, pathSeparator: Char): Sequence<Class<T>> {
            return filter { classNamePattern.containsMatchIn(it.filename) }.asSequence().map { "$packagePrefix${it.path.split(resourceName).last().replace(pathSeparator, packageSeparator).removeSuffix(".class")}" }.map { classLoader.loadClass(it) }.filter { it.superclass == T::class.java }
                .map { it as Class<T> }
        }
    }

    @Plugin.Event
    private fun getEventAnnotation() {
        throw UnsupportedOperationException()
    }

    private val defaultEventAnnotation = this::getEventAnnotation.findAnnotation<Plugin.Event>()!!
}
