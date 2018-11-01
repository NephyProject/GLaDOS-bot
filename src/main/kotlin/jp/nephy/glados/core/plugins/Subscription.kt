package jp.nephy.glados.core.plugins

import jp.nephy.glados.config
import jp.nephy.glados.core.Logger
import jp.nephy.glados.core.extensions.invocationException
import jp.nephy.glados.core.extensions.launchAndDelete
import jp.nephy.glados.core.extensions.messages.HexColor
import jp.nephy.glados.core.extensions.reply
import jp.nephy.utils.round
import jp.nephy.utils.stackTraceString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import net.dv8tion.jda.core.entities.Guild
import org.openjdk.jol.info.GraphLayout
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure
import kotlin.system.measureTimeMillis

object Subscription {
    private const val megabytes = 1048576.0

    abstract class Element<T: Annotation>(protected val annotation: T, private val instance: Plugin, private val function: KFunction<*>) {
        val logger = Logger("Subscription.${javaClass.simpleName}.$fullname")

        val name: String
            get() = function.name
        val fullname: String
            get() = "${instance.fullname}#$name"
        abstract val priority: Plugin.Priority

        private val parameterClasses = function.valueParameters.map { it.type.jvmErasure }
        fun matchParameters(vararg args: Any): Boolean {
            return parameterClasses.size == args.size && args.zip(parameterClasses).all { it.first::class.isSubclassOf(it.second) }
        }

        @Throws(CancellationException::class)
        suspend fun invoke(guild: Guild?, vararg args: Any) {
            try {
                if (!logger.isTraceEnabled) {
                    function.callSuspend(instance, *args)
                } else {
                    val processTimeMillis = measureTimeMillis {
                        function.callSuspend(instance, *args)
                    }

                    val layout = GraphLayout.parseInstance(instance)
                    if (guild != null) {
                        logger.trace { "$processTimeMillis ms, ${(layout.totalSize() / megabytes).round(3)} MB で終了しました。 (${guild.name})" }
                    } else {
                        logger.trace { "$processTimeMillis ms, ${(layout.totalSize() / megabytes).round(3)} MB  で終了しました。" }
                    }
                }
            } catch (e: Exception) {
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
                                        title("`${event.commandLine}` の実行中に例外が発生しました。")
                                        description { "ご不便をお掛けしています。この問題が何度も発生する場合は開発者にご連絡ください。" }
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
            }
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
        private val commandNames: List<String>
            get() = listOf(primaryCommandName) + annotation.aliases
        val description: String?
            get() = annotation.description.ifBlank { null }
        val arguments: List<String>
            get() = annotation.args.toList()
        val category: String?
            get() = annotation.category.ifBlank { null }

        private val prefix: String
            get() = annotation.prefix.ifBlank { config.prefix }
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

        val intervalMillis = annotation.unit.toMillis(annotation.interval)
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
}
