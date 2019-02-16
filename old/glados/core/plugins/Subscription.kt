package jp.nephy.glados.core.plugins

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.features.origin
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.util.pipeline.PipelineContext
import jp.nephy.glados.GLaDOS
import jp.nephy.glados.core.logger.SlackLogger
import jp.nephy.glados.core.plugins.extensions.invocationException
import jp.nephy.glados.core.plugins.extensions.jda.launchAndDelete
import jp.nephy.glados.core.plugins.extensions.jda.messages.HexColor
import jp.nephy.glados.core.plugins.extensions.jda.messages.reply
import jp.nephy.glados.core.plugins.extensions.stackTraceString
import jp.nephy.glados.core.plugins.extensions.web.meta.SitemapUpdateFrequency
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure
import kotlin.system.measureNanoTime

object Subscription {
    private const val nano = 1000000.0

    abstract class Element<T: Annotation>(protected val annotation: T, private val instance: Plugin, private val function: KFunction<*>) {
        val logger: SlackLogger = SlackLogger("Subscription.${javaClass.simpleName}.$fullname")
        
        private val parameterClasses = function.valueParameters.map { it.type.jvmErasure }
        fun matchParameters(vararg classes: KClass<*>): Boolean {
            return parameterClasses.size == classes.size && classes.zip(parameterClasses).all { it.first.isSubclassOf(it.second) }
        }

        fun matchParameters(vararg args: Any): Boolean {
            return matchParameters(*args.map { it::class }.toTypedArray())
        }

        @Throws(CancellationException::class)
        suspend fun invoke(vararg args: Any): Boolean {
            return runCatching {
                val processTimeNano = measureNanoTime {
                    function.callSuspend(instance, *args)
                }

                logger.trace { "${String.format("%.3f", processTimeNano / nano)} ms で終了しました。" }
            }.onFailure { e ->
                when (val exception = e.invocationException) {
                    is CancellationException -> {
                        withContext(NonCancellable) {
                            logger.debug { "キャンセルされました。" }
                        }
                        throw exception
                    }
                    is CommandError -> {
                        logger.error(exception) { "コマンドエラーが発生しました。" }
                    }
                    else -> {
                        when (val event = args.firstOrNull()) {
                            is Plugin.Command.Event -> {
                                event.reply {
                                    embed {
                                        title("`${event.command.primaryCommandSyntax}` の実行中に例外が発生しました")
                                        description { "引数: `${event.args}`\nご不便をお掛けしています。この問題が何度も発生する場合は開発者にご連絡ください。" }
                                        field("スタックトレース") { "${exception.stackTraceString.take(300)}..." }
                                        color(HexColor.Bad)
                                        timestamp()
                                    }
                                }.launchAndDelete(30, TimeUnit.SECONDS)
                            }
                            else -> {
                                logger.error(exception) { "実行中に例外が発生しました。" }
                            }
                        }
                    }
                }
            }.isSuccess
        }
    }

    class Event(annotation: Plugin.Event, instance: Plugin, function: KFunction<*>): Element<Plugin.Event>(annotation, instance, function) {
        override val priority: Plugin.Priority
            get() = annotation.priority
    }

    class Command(annotation: Plugin.Command, instance: Plugin, function: KFunction<*>): Element<Plugin.Command>(annotation, instance, function) {
        override val priority: Plugin.Priority
            get() = annotation.priority

        val primaryCommandName: String
            get() = annotation.command.ifBlank { name }
        val commandNames: List<String>
            get() = listOf(primaryCommandName) + annotation.aliases
        val description: String?
            get() = annotation.description.ifBlank { null }
        val arguments: List<String>
            get() = annotation.args.toList()
        val category: String?
            get() = annotation.category.ifBlank { null }

        private val prefix: String
            get() = annotation.prefix.ifBlank { GLaDOS.config.prefix }
        val commandSyntaxes: List<String>
            get() = commandNames.map { "$prefix$it" }
        val primaryCommandSyntax: String
            get() = commandSyntaxes.first()

        val permissionPolicy: Plugin.Command.PermissionPolicy
            get() = annotation.permission
        val targetChannelType: Plugin.Command.TargetChannelType
            get() = annotation.channelType
        val casePolicy: Plugin.Command.CasePolicy
            get() = annotation.case
        val conditionPolicy: Plugin.Command.ConditionPolicy
            get() = annotation.condition

        val shouldCheckArgumentsCount: Boolean
            get() = annotation.checkArgsCount
    }

    class Loop(annotation: Plugin.Loop, instance: Plugin, function: KFunction<*>): Element<Plugin.Loop>(annotation, instance, function) {
        override val priority: Plugin.Priority
            get() = annotation.priority

        val intervalMillis: Long = annotation.unit.toMillis(annotation.interval)
    }

    class Schedule(annotation: Plugin.Schedule, instance: Plugin, function: KFunction<*>): Element<Plugin.Schedule>(annotation, instance, function) {
        override val priority: Plugin.Priority
            get() = annotation.priority

        private val timing = object {
            val hours: IntArray
            val minutes: IntArray

            init {
                val hoursADay = 0 until 24
                val h = annotation.hours.toMutableList()
                if (annotation.multipleHours.isNotEmpty()) {
                    annotation.multipleHours.forEach { multipleHour: Int ->
                        hoursADay.filter { it * multipleHour in hoursADay }.forEach {
                            h.add(it * multipleHour)
                        }
                    }
                }
                hours = if (h.isEmpty()) {
                    hoursADay.toList().toIntArray()
                } else {
                    h.toSortedSet().toIntArray()
                }

                val minutesAnHour = 0 until 60
                val m = annotation.minutes.toMutableList()
                if (annotation.multipleMinutes.isNotEmpty()) {
                    annotation.multipleMinutes.forEach { multipleMinute: Int ->
                        minutesAnHour.filter { it * multipleMinute in minutesAnHour }.forEach {
                            m.add(it * multipleMinute)
                        }
                    }
                }
                minutes = if (m.isEmpty()) {
                    minutesAnHour.toList().toIntArray()
                } else {
                    m.toSortedSet().toIntArray()
                }
            }
        }

        fun matches(calendar: Calendar): Boolean {
            return calendar.get(Calendar.HOUR_OF_DAY) in timing.hours && calendar.get(Calendar.MINUTE) in timing.minutes
        }
    }

    class Tweetstorm(annotation: Plugin.Tweetstorm, instance: Plugin, function: KFunction<*>): Element<Plugin.Tweetstorm>(annotation, instance, function) {
        val accounts = annotation.accounts.map { GLaDOS.config.twitterAccount(it) }

        override val priority: Plugin.Priority
            get() = annotation.priority
    }

    object Web {
        class Page(annotation: Plugin.Web.Page, instance: Plugin, function: KFunction<*>): Element<Plugin.Web.Page>(annotation, instance, function) {
            companion object {
                private val fragmentPattern = "^\\{(.+)}$".toRegex()
            }

            private val httpMethods: List<HttpMethod>
                get() = annotation.methods.ifEmpty { Plugin.Web.HttpMethod.values() }.map { it.ktor }
            val domain: String?
                get() = annotation.domain.ifBlank { null }
            val updateFrequency: SitemapUpdateFrequency
                get() = annotation.updateFrequency
            val banRobots: Boolean
                get() = annotation.banRobots

            val pathType: Plugin.Web.PathType
                get() = annotation.pathType
            val path: String
                get() = "/${annotation.path.removePrefix("/").removeSuffix("/").trim()}"

            private val regexPath: Regex
                get() = "^$path$".toRegex(annotation.regexOptions.toSet())

            private val fragments = if (pathType == Plugin.Web.PathType.Pattern) {
                path.split("/").mapIndexedNotNull { i, it ->
                    (fragmentPattern.matchEntire(it)?.groupValues?.get(1) ?: return@mapIndexedNotNull null) to i
                }.toMap()
            } else {
                null
            }.orEmpty()

            fun canHandle(call: ApplicationCall): Boolean {
                if (!matchParameters(Plugin.Web.AccessEvent::class) || (call.request.httpMethod !in httpMethods && call.request.httpMethod != HttpMethod.Options) || (domain != null && domain != call.request.origin.host)) {
                    return false
                }

                val requestPath = call.request.path()
                return when (annotation.pathType) {
                    Plugin.Web.PathType.Normal -> {
                        path == requestPath
                    }
                    Plugin.Web.PathType.Regex -> {
                        regexPath.matches(requestPath)
                    }
                    Plugin.Web.PathType.Pattern -> {
                        val (expectPaths, actualPaths) = path.split("/") to requestPath.split("/")
                        if (expectPaths.size != actualPaths.size) {
                            return false
                        }

                        for ((expected, actual) in expectPaths.zip(actualPaths)) {
                            if (!fragmentPattern.matches(expected) && expected != actual) {
                                return false
                            }
                        }

                        return true
                    }
                }
            }

            fun makeEvent(context: PipelineContext<Unit, ApplicationCall>): Plugin.Web.AccessEvent {
                val matchResult = if (pathType == Plugin.Web.PathType.Regex) {
                    regexPath.matchEntire(context.call.request.path())
                } else {
                    null
                }
                val fragments = if (pathType == Plugin.Web.PathType.Pattern) {
                    val currentFragments = context.call.request.path().split("/")

                    fragments.map { (name, index) ->
                        name to currentFragments[index]
                    }.toMap()
                } else {
                    null
                }.orEmpty()

                return Plugin.Web.AccessEvent(context, matchResult, fragments)
            }

            override val priority: Plugin.Priority
                get() = annotation.priority
        }

        class ErrorPage(annotation: Plugin.Web.ErrorPage, instance: Plugin, function: KFunction<*>): Element<Plugin.Web.ErrorPage>(annotation, instance, function) {
            val domain: String?
                get() = annotation.domain.ifBlank { null }
            val statuses: List<HttpStatusCode>
                get() = annotation.statuses.map { it.ktor }

            override val priority: Plugin.Priority
                get() = annotation.priority
        }
    }
}
