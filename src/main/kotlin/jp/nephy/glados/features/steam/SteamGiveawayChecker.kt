package jp.nephy.glados.features.steam

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import io.ktor.client.request.headers
import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.message
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Event
import jp.nephy.glados.core.feature.subscription.Loop
import jp.nephy.glados.core.feature.textChannelsLazy
import jp.nephy.glados.core.getHistory
import jp.nephy.glados.core.isSelfUser
import jp.nephy.glados.core.launch
import jp.nephy.glados.dispatcher
import jp.nephy.glados.features.internal.MessageCollector
import jp.nephy.jsonkt.*
import jp.nephy.jsonkt.delegation.*
import jp.nephy.utils.FloatLinkedSingleCache
import jp.nephy.utils.IntLinkedSingleCache
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

class SteamGiveawayChecker: BotFeature() {
    companion object {
        private val color = Color("f6546a")
    }

    private val channels by textChannelsLazy("steam_giveaway")
    private var lastRedditGiveawayCreated by FloatLinkedSingleCache { 0F }
    private var lastGiveawaySuId by IntLinkedSingleCache { 0 }

    private val httpClient = HttpClient(Apache)

    private val ignoreTags = arrayOf("Discussion", "Ended", "Restocked", "Open Alpha", "Beta", "Mod post")
    private val bannedDomains = arrayOf(
            "self.FreeGamesOnSteam",
            "nagift.ru",
            "giveaway.su",
            "gamehag.com",
            "discord.gg",
            "discordapp.com",
            "youtube.com",
            "youtu.be",
            "gmail.com",
            "firefoxusercontent.com",
            "marvelousga.com",
            "imgur.com",
            "gamehag.net",
            "bananagiveaway.com",
            "gamecode.win",
            "redd.it",
            "twitch.tv",
            "opiumpulses.com",
            "gamezito.com"
    )
    private val latestPostOnlyDomains = arrayOf("indiegala.com", "hrkgame.com")

    @Event
    override suspend fun onReady(event: ReadyEvent) {
        GlobalScope.launch(dispatcher) {
            for (it in channels) {
                it.getHistory(100)
                        .filter {
                            it.author.isSelfUser && bannedDomains.any { ban ->
                                it.embeds.firstOrNull()?.footer?.text?.endsWith(ban) == true
                            }
                        }
                        .forEach {
                            it.delete().launch()
                        }
            }
        }
    }

    @Loop(1, TimeUnit.MINUTES)
    suspend fun checking() {
        checkReddit(channels)
        checkGiveawaySu(channels)
    }

    private suspend fun checkReddit(channels: List<TextChannel>) {
        val json = httpClient.get<String>("https://www.reddit.com/r/FreeGamesOnSteam/new.json") {
            headers {
                append("Accept-Language", "ja")
                append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
            }
        }.toJsonObject()

        json["data"]["children"].toImmutableJsonObjectList().forEachIndexed { i, it ->
            val data by it.byImmutableJsonObject
            if ("url" !in data) {
                return@forEachIndexed
            }

            val redditUrl by data.byLambda("permalink") { "https://www.reddit.com${it.string}" }
            val tag by data.byNullableString("link_flair_text")
            val created by data.byFloat

            // 古いGiveawayの投稿
            if (created <= lastRedditGiveawayCreated) {
                // 終了済みはメッセージを削除
                if (tag == "Ended") {
                    MessageCollector.filter {
                        it.author.isSelfUser && it.textChannel in channels && it.embeds.firstOrNull()?.description == redditUrl
                    }.forEach {
                        it.delete().launch()
                    }
                }
                return@forEachIndexed
            }

            // 最新のインデックスを記録
            if (i == 0) {
                lastRedditGiveawayCreated = created
            }

            // タグ付きを無視
            if (tag in ignoreTags) {
                return@forEachIndexed
            }

            val title by data.byString
            // お知らせ(PSA, Public Service Announcement)を無視
            if (title.startsWith("[PSA]")) {
                return@forEachIndexed
            }

            val campaignDomain by data.byString("domain")
            // 禁止ドメインを無視
            if (bannedDomains.any { campaignDomain.endsWith(it) }) {
                return@forEachIndexed
            }

            // latestPostOnlyDomainsは最新の投稿がある場合 過去のメッセージを削除する
            if (campaignDomain in latestPostOnlyDomains) {
                MessageCollector.filter {
                    it.author.isSelfUser && it.textChannel in channels && it.embeds.firstOrNull()?.footer?.text == campaignDomain
                }.forEach {
                    it.delete().launch()
                }
            }

            val campaignUrl by data.byLambda("url") { URLDecoder.decode(it.string, Charsets.UTF_8.displayName()) }
            val thumbnailUrl = runCatching {
                data["preview"]["images"][0]["source"]["url"].string
            }.getOrNull()

            for (channel in channels) {
                channel.message {
                    embed {
                        title(title)
                        author("Steam Giveaway Info", "https://www.reddit.com/r/FreeGamesOnSteam", "https://nephy.jp/assets/img/discord/giveaway.jpg")
                        description { campaignUrl }

                        field("Reddit 元記事") { redditUrl }

                        footer(campaignDomain)

                        if (thumbnailUrl != null) {
                            thumbnail(thumbnailUrl)
                        }

                        color(color)
                        timestamp()
                    }
                }.launch()
            }
        }
    }

    private suspend fun checkGiveawaySu(channels: List<TextChannel>) {
        val jsonArray = httpClient.get<String>("https://giveaway.su/status") {
            headers {
                append("Accept-Language", "ja")
                append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
            }
        }.toJsonArray()

        jsonArray.toImmutableJsonObjectList().sortedByDescending { it["id"].string.toInt() }.forEachIndexed { i, json ->
            val type by json.byString
            if (type != "local") {
                return@forEachIndexed
            }

            val title by json.byString("name")
            val id by json.byInt
            val url = "https://giveaway.su/giveaway/view/$id"

            // 古いGiveawayの投稿
            if (id <= lastGiveawaySuId) {
                return@forEachIndexed
            }
            if (i == 0) {
                lastGiveawaySuId = id
            }

            // 過去のメッセージを削除する
            MessageCollector.filter {
                it.author.isSelfUser && it.textChannel in channels && it.embeds.firstOrNull()?.footer?.text == "giveaway.su"
            }.forEach {
                it.delete().launch()
            }

            for (channel in channels) {
                channel.message {
                    embed {
                        title(title)
                        author("Steam Giveaway Info", "https://www.reddit.com/r/FreeGamesOnSteam", "https://nephy.jp/assets/img/discord/giveaway.jpg")
                        description { url }


                        footer("giveaway.su")

                        color(color)
                        timestamp()
                    }
                }.launch()
            }
        }
    }

    @Event
    override suspend fun onGuildMessageDelete(event: GuildMessageDeleteEvent) {
        val message = MessageCollector.latest(event.messageIdLong) ?: return
        if (!message.author.isSelfUser || event.channel !in channels) {
            return
        }

        val url = message.embeds.firstOrNull()?.description ?: return
        MessageCollector.filter {
            it.author.isSelfUser && it.textChannel in channels && it.embeds.firstOrNull()?.description == url
        }.forEach {
            it.delete().launch()
        }
    }
}
