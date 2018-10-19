package jp.nephy.glados.features

import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.message
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Listener
import jp.nephy.glados.core.feature.textChannelsLazy
import jp.nephy.glados.core.isBotOrSelfUser
import jp.nephy.glados.secret
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.emulation.OfficialClient
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent

class IHateSuchWatcher: BotFeature() {
    private val iHateSuchTextChannels by textChannelsLazy("i_hate_such")
    private val twitter = PenicillinClient {
        account {
            application(OfficialClient.OAuth1a.TwitterForiPad)
            token(secret.forKey("twitter_access_token"), secret.forKey("twitter_access_token_secret"))
        }
    }
    private val twitterUrl = "(?:http(?:s)?://)?(?:m|mobile)?twitter\\.com/((?:\\w|_){1,16})/status/(\\d+)".toRegex()

    @Listener
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.member.user.isBotOrSelfUser || event.channel !in iHateSuchTextChannels) {
            return
        }

        if (!twitterUrl.containsMatchIn(event.message.contentDisplay)) {
            logger.info { "Twitter URLパターンに一致しないメッセージです: ${event.message.contentDisplay}" }
            return
        }

        event.channel.revealTweet(event.message.contentDisplay, onlyWhenProtected = true)

        for (it in HateEmoji.values()) {
            event.message.addReaction(it.emoji).queue()
        }
    }

    @Listener
    override fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) {
        if (event.member.user.isBotOrSelfUser || event.channel !in iHateSuchTextChannels) {
            return
        }

        if (!twitterUrl.containsMatchIn(event.message.contentDisplay)) {
            logger.info { "Twitter URLパターンに一致しないメッセージです: ${event.message.contentDisplay}" }
            return
        }

        for (e in HateEmoji.values()) {
            if (event.message.reactions.any { it.reactionEmote.name == e.emoji }) {
                continue
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
                                appendln(status.result.fullText())
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
