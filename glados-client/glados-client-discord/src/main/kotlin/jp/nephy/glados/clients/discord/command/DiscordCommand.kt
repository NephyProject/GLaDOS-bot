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

import jp.nephy.glados.api.annotations.Priority
import net.dv8tion.jda.api.entities.ChannelType

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
}
