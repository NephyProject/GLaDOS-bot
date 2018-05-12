package jp.nephy.glados.component.helper

import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.requests.restaction.MessageAction
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


val User.displayName: String
    get() = "@$name#$discriminator"
val User.isSelf: Boolean
    get() = idLong == jda.selfUser.idLong
val Member.fullName: String
    get() = if (guild != null) {
        "$effectiveName (${user.displayName}, ${guild.name})"
    } else {
        "$effectiveName (${user.displayName})"
    }
val Member.fullNameWithoutGuild: String
    get() = "$effectiveName (${user.displayName})"

val VoiceChannel.isNoOneExceptSelf: Boolean
    get() = members.count { ! it.user.isSelf } == 0

fun Member.hasRole(id: Long): Boolean {
    return roles.any { it.idLong == id }
}

val Event.nullableGuild: Guild?
    get() = try {
        this::class.java.getMethod("getGuild").invoke(this) as? Guild
    } catch (e: Exception) {
        null
    }
val Event.nullableUser: User?
    get() = try {
        this::class.java.getMethod("getUser").invoke(this) as? User
    } catch (e: Exception) {
        null
    }
val Event.nullableMember: Member?
    get() = try {
        this::class.java.getMethod("getMember").invoke(this) as? Member
    } catch (e: Exception) {
        null
    }

fun TextChannel.embedMessage(message: EmbedBuilder.() -> Unit): MessageAction {
    sendTyping().queue()
    val embed = EmbedBuilder()
    message(embed)
    return sendMessage(embed.build())
}

fun TextChannel.embedMention(target: Member, message: EmbedBuilder.() -> Unit): MessageAction {
    sendTyping().queue()
    val embed = EmbedBuilder()
    message(embed)
    embed.asMention(target)
    return sendMessage(embed.build())
}

fun CommandEvent.embedMention(message: EmbedBuilder.() -> Unit): MessageAction {
    textChannel.sendTyping().queue()
    val embed = EmbedBuilder()
    message(embed)
    embed.asMention(member)
    return textChannel.sendMessage(embed.build())
}

fun MessageAction.deleteQueue(delay: Long? = null, unit: TimeUnit = TimeUnit.MILLISECONDS, then: (Message) -> Unit = { }) {
    queue {
        if (delay != null) {
            thread {
                unit.sleep(delay)
                it.delete().queue({}, {})
            }
        } else {
            it.delete().queue({}, {})
        }
        then(it)
    }
}

inline fun <reified T: Event> EventWaiter.wait(noinline condition: T.() -> Boolean = { true }, timeout: Long? = null, noinline whenTimeout: () -> Unit = { }, noinline operation: T.() -> Unit) {
    var stop = false
    if (timeout != null) {
        thread(name = "Check Timeout") {
            Thread.sleep(timeout)
            stop = true
            whenTimeout()
        }
    }

    waitForEvent(T::class.java, {
        ! stop && condition(it)
    }, {
        if (stop) {
            return@waitForEvent
        }
        operation(it)
    })
}

fun TextChannel.getMessages(limit: Int): List<Message> {
    return iterableHistory.cache(false).take(limit)
}
