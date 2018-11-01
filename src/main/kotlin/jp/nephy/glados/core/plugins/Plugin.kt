package jp.nephy.glados.core.plugins

import jp.nephy.glados.core.Logger
import jp.nephy.glados.core.extensions.displayName
import jp.nephy.glados.core.extensions.fullName
import jp.nephy.glados.core.extensions.ifNullOrBlank
import jp.nephy.glados.core.extensions.spaceRegex
import jp.nephy.glados.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.MessageUpdateEvent
import java.util.concurrent.TimeUnit

abstract class Plugin(
        pluginName: String? = null,
        version: String? = null,
        val description: String? = null
): EventModel, CoroutineScope {
    val name = pluginName.ifNullOrBlank { javaClass.simpleName }
    val fullname = "$name[v${version ?: "1.0.0.0"}]"

    private val job = Job()
    override val coroutineContext
        get() = dispatcher + job

    val logger = Logger("Plugin.$fullname")
    val silentLogger = Logger("Plugin.$fullname", false)

    enum class Priority {
        Highest, Higher, High, Normal, Low, Lower, Lowest
    }

    @Target(AnnotationTarget.FUNCTION)
    annotation class Event(
            val priority: Priority = Priority.Normal
    )

    @Target(AnnotationTarget.FUNCTION)
    annotation class Command(
            val command: String = "",
            val aliases: Array<String> = [],
            val priority: Priority = Priority.Normal,
            val permission: PermissionPolicy = PermissionPolicy.Anyone,
            val channelType: TargetChannelType = TargetChannelType.Any,
            val case: CasePolicy = CasePolicy.Strict,
            val condition: ConditionPolicy = ConditionPolicy.Anytime,
            val description: String = "",
            val args: Array<String> = [],
            val checkArgsCount: Boolean = true,
            val prefix: String = "",
            val category: String = ""
    ) {
        enum class PermissionPolicy {

            Anyone, AdminOnly, OwnerOnly
        }

        enum class TargetChannelType(vararg val types: ChannelType) {
            Any(net.dv8tion.jda.core.entities.ChannelType.TEXT, net.dv8tion.jda.core.entities.ChannelType.PRIVATE),
            TextChannel(net.dv8tion.jda.core.entities.ChannelType.TEXT),
            PrivateMessage(net.dv8tion.jda.core.entities.ChannelType.PRIVATE)
        }

        enum class CasePolicy {
            Strict, Ignore
        }

        enum class ConditionPolicy {
            Anytime, WhileInAnyVoiceChannel, WhileInSameVoiceChannel
        }

        data class Event(
                val args: String,
                val command: Subscription.Command,
                val message: Message,
                val author: User,
                val member: Member?,
                val guild: Guild?,
                val textChannel: TextChannel?,
                val channel: MessageChannel,
                private val _jda: JDA,
                private val _responseNumber: Long
        ): net.dv8tion.jda.core.events.Event(_jda, _responseNumber) {
            constructor(args: String, command: Subscription.Command, event: MessageReceivedEvent): this(args, command, event.message, event.author, event.member, event.guild, event.textChannel, event.channel, event.jda, event.responseNumber)
            constructor(args: String, command: Subscription.Command, event: MessageUpdateEvent): this(args, command, event.message, event.author, event.member, event.guild, event.textChannel, event.channel, event.jda, event.responseNumber)

            val commandLine: String
                get() = message.contentDisplay
            val argList: List<String>
                get() = if (args.isNotBlank()) {
                    args.split(spaceRegex)
                } else {
                    emptyList()
                }
            val authorName: String
                get() = member?.fullName ?: author.displayName
        }
    }

    @Target(AnnotationTarget.FUNCTION)
    annotation class Loop(
            val interval: Long,
            val unit: TimeUnit,
            val priority: Priority = Priority.Normal
    )

    @Target(AnnotationTarget.FUNCTION)
    annotation class Schedule(
            val hours: IntArray = [],
            val minutes: IntArray = [],
            val multipleHours: IntArray = [],
            val multipleMinutes: IntArray = [],
            val priority: Priority = Priority.Normal
    )
}
