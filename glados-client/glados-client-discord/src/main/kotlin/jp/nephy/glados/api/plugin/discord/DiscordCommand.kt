package jp.nephy.glados.api.plugin.discord

import jp.nephy.glados.api.plugin.Priority
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent

@Target(AnnotationTarget.FUNCTION)
annotation class DiscordCommand(
    val command: String = "",
    val aliases: Array<String> = [],
    val priority: Priority = Priority.Normal,
    val permission: PermissionPolicy = PermissionPolicy.Anyone,
    val channelType: TargetChannelType = TargetChannelType.Any,
    val case: CasePolicy = CasePolicy.Ignore,
    val condition: ConditionPolicy = ConditionPolicy.Anytime,
    val description: String = "",
    val args: Array<String> = [],
    val checkArgsCount: Boolean = true,
    val prefix: String = "",
    val category: String = ""
) {
    enum class PermissionPolicy {

        Anyone, AdminOnly, MainGuildAdminOnly, OwnerOnly
    }

    enum class TargetChannelType(vararg val types: ChannelType) {
        Any(ChannelType.TEXT, ChannelType.PRIVATE),
        
        TextChannel(ChannelType.TEXT), BotChannel(ChannelType.TEXT), PrivateMessage(ChannelType.PRIVATE)
    }

    enum class CasePolicy {
        Strict, Ignore
    }

    enum class ConditionPolicy {
        Anytime, WhileInAnyVoiceChannel, WhileInSameVoiceChannel
    }

    class Event(
        val argList: List<String>,
        val command: Subscription.Command,
        val message: Message,
        val author: User,
        val member: Member?,
        val guild: Guild?,
        val textChannel: TextChannel?,
        val channel: MessageChannel,
        _jda: JDA,
        _responseNumber: Long
    ): net.dv8tion.jda.api.events.Event(_jda, _responseNumber) {
        constructor(argList: List<String>, command: Subscription.Command, event: MessageReceivedEvent): this(argList, command, event.message, event.author, event.member, event.guild, event.textChannel, event.channel, event.jda, event.responseNumber)
        constructor(argList: List<String>, command: Subscription.Command, event: MessageUpdateEvent): this(argList, command, event.message, event.author, event.member, event.guild, event.textChannel, event.channel, event.jda, event.responseNumber)

        val args: String
            get() = argList.joinToString(" ")
        val authorName: String
            get() = member?.fullName ?: author.displayName

        val first: String
            get() = argList[0]

        operator fun component1(): String = first

        val second: String
            get() = argList[1]

        operator fun component2(): String = second

        operator fun component3(): String = argList[2]
        operator fun component4(): String = argList[3]
        operator fun component5(): String = argList[4]
        operator fun component6(): String = argList[5]
        operator fun component7(): String = argList[6]
        operator fun component8(): String = argList[7]
        operator fun component9(): String = argList[8]
        operator fun component10(): String = argList[9]
    }
}
