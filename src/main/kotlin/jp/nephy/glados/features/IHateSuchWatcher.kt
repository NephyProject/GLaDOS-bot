package jp.nephy.glados.features

import jp.nephy.glados.config
import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.message
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Listener
import jp.nephy.glados.core.isSelfUser
import jp.nephy.glados.secret
import jp.nephy.penicillin.OfficialClient
import jp.nephy.penicillin.PenicillinClient
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent

class IHateSuchWatcher: BotFeature() {
    private val twitter = PenicillinClient.build {
        application(OfficialClient.TwitterForiPad)
        token(secret.forKey("twitter_access_token"), secret.forKey("twitter_access_token_secret"))
    }
    private val twitterUrl = "(?:http(?:s)?://)?(?:m|mobile)?twitter\\.com/((?:\\w|_){1,16})/status/(\\d+)".toRegex()

    @Listener
    override fun onGuildMessageReactionAdd(event: GuildMessageReactionAddEvent) {
        val iHateSuchChannel = config.forGuild(event.guild)?.textChannel("i_hate_such") ?: return
        if (event.member == null || event.user.isSelfUser || event.channel.idLong != iHateSuchChannel.idLong) {
            return
        }

        val message = MessageCollector.get(event.messageIdLong) ?: return
        val match = twitterUrl.find(message.contentDisplay)
        if (match == null) {
            logger.info { "Twitter URLパターンに一致しないメッセージです: ${message.contentDisplay}" }
            return
        }

        val emoji = HateEmoji.fromEmoji(event.reactionEmote.name)
        if (emoji == null) {
            logger.info { "`${message.contentDisplay}`に未知のリアクション ${event.reactionEmote.name} が付きました." }
            return
        }
    }

    @Listener
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        val iHateSuchChannel = config.forGuild(event.guild)?.textChannel("i_hate_such") ?: return
        if (event.member == null || event.author.isSelfUser || event.channel.idLong != iHateSuchChannel.idLong) {
            return
        }

        if (!twitterUrl.containsMatchIn(event.message.contentDisplay)) {
            logger.info { "Twitter URLパターンに一致しないメッセージです: ${event.message.contentDisplay}" }
            return
        }

        event.channel.revealTweet(event.message.contentDisplay, onlyWhenProtected = true)

        HateEmoji.values().forEach {
            event.message.addReaction(it.emoji).queue()
        }
    }

    @Listener
    override fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) {
        val iHateSuchChannel = config.forGuild(event.guild)?.textChannel("i_hate_such") ?: return
        if (event.member == null || event.author.isSelfUser || event.channel.idLong != iHateSuchChannel.idLong) {
            return
        }

        if (!twitterUrl.containsMatchIn(event.message.contentDisplay)) {
            logger.info { "Twitter URLパターンに一致しないメッセージです: ${event.message.contentDisplay}" }
            return
        }

        HateEmoji.values().forEach { e ->
            if (event.message.reactions.any { it.reactionEmote.name == e.emoji }) {
                return@forEach
            }

            event.message.addReaction(e.emoji).queue()
        }
    }

    private fun TextChannel.revealTweet(text: String, onlyWhenProtected: Boolean = false) {
        twitterUrl.findAll(text).forEach { matched ->
            val statusId = matched.groupValues.last().toLong()
            twitter.status.show(id = statusId).queue { status ->
                if (!onlyWhenProtected || status.result.user.protected) {
                    message {
                        embed {
                            author("${status.result.user.name} @${status.result.user.screenName}", "https://twitter.com/${status.result.user.screenName}", status.result.user.profileImageUrlHttps)
                            descriptionBuilder {
                                appendln(status.result.fullText)
                                appendln()
                                append(matched.value)
                            }
                            if (status.result.extendedEntities?.media.orEmpty().isNotEmpty()) {
                                image(status.result.extendedEntities!!.media.first().mediaUrlHttps)
                            }

                            color(Color.Twitter)
                            footer("Twitter", "https://abs.twimg.com/icons/apple-touch-icon-192x192.png")
                            timestamp()
                        }
                    }.queue()
                }
            }
        }
    }
}

enum class HateEmoji(val emoji: String) {
    Face1("😩"), Face2("😰"), Face3("😟");

    companion object {
        fun fromEmoji(emoji: String): HateEmoji? {
            return values().find { it.emoji == emoji }
        }
    }
}
