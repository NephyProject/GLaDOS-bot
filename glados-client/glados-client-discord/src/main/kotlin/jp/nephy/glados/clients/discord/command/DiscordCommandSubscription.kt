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

package jp.nephy.glados.clients.discord.command

import jp.nephy.glados.api.*
import jp.nephy.glados.api.annotations.Priority
import jp.nephy.glados.clients.GLaDOSSubscription
import jp.nephy.glados.clients.discord.config.booleanOption
import jp.nephy.glados.clients.discord.config.discord
import jp.nephy.glados.clients.discord.config.textChannel
import jp.nephy.glados.clients.discord.extensions.*
import jp.nephy.glados.clients.discord.extensions.messages.HexColor
import jp.nephy.glados.clients.discord.extensions.messages.reply
import jp.nephy.glados.clients.utils.name
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import java.util.concurrent.TimeUnit
import kotlin.reflect.KFunction

class DiscordCommandSubscription(
    override val plugin: Plugin, override val function: KFunction<*>, override val annotation: DiscordCommand
): GLaDOSSubscription<DiscordCommand, DiscordCommandEvent>() {
    override val priority: Priority
        get() = annotation.priority

    override fun onFailure(throwable: Throwable, event: DiscordCommandEvent) {
        if (throwable is DiscordCommandError) {
            logger.error(throwable) { "コマンドエラーが発生しました。" }
        } else {
            event.reply {
                embed {
                    title("`${event.command.primaryCommandSyntax}` の実行中に例外が発生しました")
                    description { "引数: `${event.args}`\nご不便をお掛けしています。この問題が何度も発生する場合は開発者にご連絡ください。" }
                    field("スタックトレース") { "${throwable.stackTraceString.take(300)}..." }
                    color(HexColor.Bad)
                    timestamp()
                }
            }.launchAndDelete(30, TimeUnit.SECONDS)

            super.onFailure(throwable, event)
        }
    }
}

val DiscordCommandSubscription.primaryCommandName: String
    get() = annotation.command.ifBlank { name }

val DiscordCommandSubscription.commandNames: List<String>
    get() = listOf(primaryCommandName) + annotation.aliases

val DiscordCommandSubscription.description: String?
    get() = annotation.description.ifBlank { null }

val DiscordCommandSubscription.arguments: List<String>
    get() = annotation.args.toList()

val DiscordCommandSubscription.category: String?
    get() = annotation.category.ifBlank { null }

val DiscordCommandSubscription.prefix: String
    get() = annotation.prefix.ifBlank { GLaDOS.config.discord.prefix }

val DiscordCommandSubscription.commandSyntaxes: List<String>
    get() = commandNames.map { "$prefix$it" }

val DiscordCommandSubscription.primaryCommandSyntax: String
    get() = commandSyntaxes.first()

val DiscordCommandSubscription.permissionPolicy: DiscordCommand.PermissionPolicy
    get() = annotation.permission

val DiscordCommandSubscription.targetChannelType: DiscordCommand.TargetChannelType
    get() = annotation.channelType

val DiscordCommandSubscription.casePolicy: DiscordCommand.CasePolicy
    get() = annotation.case

val DiscordCommandSubscription.conditionPolicy: DiscordCommand.ConditionPolicy
    get() = annotation.condition

val DiscordCommandSubscription.shouldCheckArgumentsCount: Boolean
    get() = annotation.checkArgsCount

internal fun DiscordCommandSubscription.satisfyChannelTypeRequirement(type: ChannelType): Boolean {
    return type in targetChannelType.types
}

internal fun DiscordCommandSubscription.satisfyBotChannelRequirement(channel: MessageChannel): Boolean {
    return targetChannelType != DiscordCommand.TargetChannelType.BotChannel || (channel is TextChannel && channel.guild.config?.textChannel("bot") == channel)
}

internal fun DiscordCommandSubscription.parseArgs(text: String): List<String>? {
    val split = text.split(DiscordCommandSubscriptionClient.spaceRegex)
    val syntax = split.firstOrNull() ?: return null

    val matched = when (casePolicy) {
        DiscordCommand.CasePolicy.Strict -> {
            commandSyntaxes.any { it == syntax }
        }
        DiscordCommand.CasePolicy.Ignore -> {
            commandSyntaxes.any { it.equals(syntax, true) }
        }
    }

    if (!matched) {
        return null
    }

    return split.drop(1)
}

internal fun DiscordCommandSubscription.satisfyArgumentsRequirement(args: List<String>): Boolean {
    return !shouldCheckArgumentsCount || arguments.size == args.size
}

internal fun satisfyCommandAvailabilityForGuildRequirement(guild: Guild?): Boolean {
    return guild == null || guild.config.booleanOption("enable_command", false)
}

internal fun DiscordCommandSubscription.satisfyCommandConditionOfWhileInAnyVoiceChannel(voiceState: GuildVoiceState?): Boolean {
    return conditionPolicy != DiscordCommand.ConditionPolicy.WhileInAnyVoiceChannel || voiceState?.inVoiceChannel() == true
}

internal fun DiscordCommandSubscription.satisfyCommandConditionOfWhileInSameVoiceChannel(member: Member?): Boolean {
    return conditionPolicy != DiscordCommand.ConditionPolicy.WhileInSameVoiceChannel || member?.guild?.currentVoiceChannel == member?.voiceState?.channel
}

internal fun DiscordCommandSubscription.satisfyCommandPermissionOfAdminOnly(event: net.dv8tion.jda.api.events.Event): Boolean {
    val isAdmin = when (event) {
        is MessageReceivedEvent -> {
            event.member?.hasAdminCapability() ?: event.author?.hasAdminCapability()
        }
        is MessageUpdateEvent -> {
            event.member?.hasAdminCapability() ?: event.author?.hasAdminCapability()
        }
        else -> null
    } ?: false
    return permissionPolicy != DiscordCommand.PermissionPolicy.AdminOnly || isAdmin
}

internal fun DiscordCommandSubscription.satisfyCommandPermissionOfMainGuildAdminOnly(event: net.dv8tion.jda.api.events.Event): Boolean {
    val guild = when (event) {
        is MessageReceivedEvent -> {
            event.guild?.config
        }
        is MessageUpdateEvent -> {
            event.guild?.config
        }
        else -> null
    }
    val isAdmin = when (event) {
        is MessageReceivedEvent -> {
            event.member?.hasAdminCapability() ?: event.author?.hasAdminCapability()
        }
        is MessageUpdateEvent -> {
            event.member?.hasAdminCapability() ?: event.author?.hasAdminCapability()
        }
        else -> null
    } ?: false
    return permissionPolicy != DiscordCommand.PermissionPolicy.MainGuildAdminOnly || (guild != null && guild.isMain && isAdmin)
}

internal fun DiscordCommandSubscription.satisfyCommandPermissionOfOwnerOnly(event: net.dv8tion.jda.api.events.Event): Boolean {
    val isGLaDOSOwner = when (event) {
        is MessageReceivedEvent -> {
            event.author.isGLaDOSOwner
        }
        is MessageUpdateEvent -> {
            event.author.isGLaDOSOwner
        }
        else -> false
    }
    return permissionPolicy != DiscordCommand.PermissionPolicy.OwnerOnly || isGLaDOSOwner
}
