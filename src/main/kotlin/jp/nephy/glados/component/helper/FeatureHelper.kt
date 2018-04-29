package jp.nephy.glados.component.helper

import jp.nephy.glados.GLaDOS
import jp.nephy.glados.component.helper.profile.UserProfile
import jp.nephy.glados.component.helper.prompt.PromptBuilder
import jp.nephy.jsonkt.JsonKt
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.Event
import net.gpedro.integrations.slack.SlackApi
import net.gpedro.integrations.slack.SlackMessage
import okhttp3.OkHttpClient
import okhttp3.Request

class FeatureHelper(private val bot: GLaDOS) {
    private val client = OkHttpClient()
    fun getUserProfile(member: Member): UserProfile? {
        val config = bot.config.getGuildConfig(member.guild)
        if (config.option.clientToken == null) {
            return null
        }

        return try {
            val response = client.newCall(
                Request.Builder()
                        .url("https://discordapp.com/api/v6/users/${member.user.id}/profile")
                        .header("Authorization", config.option.clientToken)
                        .build()
            ).execute()
            val content = response.body()!!.string()

            JsonKt.parse(content)
        } catch (e: Exception) {
            bot.logger.error(e) { "プロフィールの取得に失敗しました" }
            null
        }
    }

    fun promptBuilder(textChannel: TextChannel, member: Member, operation: PromptBuilder.() -> Unit) {
        PromptBuilder.build(bot.eventWaiter, textChannel, member, operation)
    }

    fun messageLog(event: Event, title: String? = null, color: Color = Color.Plain, message: () -> Any) {
        val guild = event.nullableGuild
        if (guild == null) {
            bot.logger.warn { "${event.javaClass.simpleName} は guildフィールドを持っていないためMessageロガーは使用できません. 代わりにSlackロガーを使用します." }
            slackLog(event, message = message)
            return
        }

        val config = bot.config.getGuildConfig(guild)
        if (config.textChannel.log == null) {
            bot.logger.warn { "${guild.name} は logチャンネルが未定義のためMessageロガーは使用できません. 代わりにSlackロガーを使用します." }
            slackLog(event, message = message)
            return
        }

        guild.getTextChannelById(config.textChannel.log).embedMessage {
            if (title != null) {
                title(title)
            }
            description(message)
            color(color)
            footer(guild.name, guild.iconUrl)
            timestamp()
        }.queue()
        slackLog(event, message = message)
    }

    fun slackLog(event: Event, username: String? = null, channel: String? = null, iconUrl: String? = null, message: () -> Any) {
        val guild = event.nullableGuild
        val member = event.nullableMember
        val user = event.nullableUser

        val config = if (guild != null) {
            bot.config.getGuildConfig(guild)
        } else {
            null
        }

        val usernameNonNull = username ?: when {
            member != null -> when (config?.isMain) {
                true -> member.fullNameWithoutGuild
                else -> member.fullName
            }
            user != null -> user.displayName
            else -> "サーバログ"
        }
        val iconUrlNotNull = iconUrl ?: member?.user?.effectiveAvatarUrl ?: user?.effectiveAvatarUrl ?: ":desktop_computer:"
        val channelNonNull = channel ?: when (config?.isMain) {
            true -> "#discord"
            false -> "#discord-other"
            null -> "#discord-misc"
        }

        val msg = SlackMessage(channelNonNull, usernameNonNull, message().toString()).setLinkNames(false).apply {
            setIcon(iconUrl ?: iconUrlNotNull)
        }

        SlackApi(bot.secret.slackIncomingUrl).call(msg)
    }
}
