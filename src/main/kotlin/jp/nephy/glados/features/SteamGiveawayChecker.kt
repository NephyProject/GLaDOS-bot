package jp.nephy.glados.features

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import io.ktor.client.request.headers
import jp.nephy.glados.config
import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.message
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Listener
import jp.nephy.glados.core.feature.subscription.Loop
import jp.nephy.glados.core.getHistory
import jp.nephy.glados.core.isSelfUser
import jp.nephy.jsonkt.*
import jp.nephy.utils.FloatLinkedSingleCache
import jp.nephy.utils.IntLinkedSingleCache
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent
import java.net.URLDecoder
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class SteamGiveawayChecker: BotFeature() {
    private var lastRedditGiveawayCreated by FloatLinkedSingleCache { 0F }
    private var lastGiveawaySuId by IntLinkedSingleCache { 0 }

    private val httpClient = HttpClient(Apache)

    private val ignoreTags = arrayOf("Discussion", "Ended", "Restocked", "Open Alpha", "Beta", "Mod post")
    private val bannedDomains = arrayOf(
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

    private val channels = CopyOnWriteArrayList<TextChannel>()

    @Listener
    override fun onReady(event: ReadyEvent) {
        config.guilds.forEach {
            channels.add(it.value.textChannel("steam_giveaway") ?: return@forEach)
        }

        launch {
            channels.forEach {
                it.getHistory(100)
                        .filter { it.author.isSelfUser && bannedDomains.any { ban -> it.embeds.firstOrNull()?.footer?.text?.endsWith(ban) == true } }
                        .forEach {
                            it.delete().queue()
                        }
            }
        }
    }

    @Loop(1, TimeUnit.MINUTES)
    suspend fun checking() {
        try {
            checkReddit(channels)
            checkGiveawaySu(channels)
        } catch (e: Exception) {
            logger.error(e) { "Giveawayのチェック中に例外が発生しました." }
        }
    }

    private suspend fun checkReddit(channels: List<TextChannel>) {
        val response = httpClient.get<String>("https://www.reddit.com/r/FreeGamesOnSteam/new.json") {
            headers {
                append("Accept-Language", "ja")
                append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
            }
        }
        val json = response.toJsonObject()

        json["data"]["children"].jsonArray.map { it.jsonObject }.forEachIndexed { i, it ->
            val url = it["data"]["url"].nullableString ?: return@forEachIndexed
            val title = it["data"]["title"].string
            val campaignUrl = URLDecoder.decode(url, Charsets.UTF_8.displayName())
            val campaignDomain = it["data"]["domain"].string
            val redditUrl = "https://www.reddit.com${it["data"]["permalink"].string}"
            val tag = it["data"]["link_flair_text"].nullableString
            val created = it["data"]["created"].float

            // 古いGiveawayの投稿
            if (created <= lastRedditGiveawayCreated) {
                // 終了済みはメッセージを削除
                if (tag == "Ended") {
                    channels.forEach { channel ->
                        MessageCollector.filter { it.author.isSelfUser && it.textChannel.idLong == channel.idLong && it.embeds.firstOrNull()?.description == redditUrl }.forEach { it.delete().queue({}, {}) }
                    }
                }
                return@forEachIndexed
            }

            // 最新のインデックスを記録
            if (i == 0) {
                lastRedditGiveawayCreated = created
            }

            // タグ付きを無視
            if (ignoreTags.contains(tag)) {
                return@forEachIndexed
            }
            // お知らせ(PSA, Public Service Announcement)を無視
            if (title.startsWith("[PSA]")) {
                return@forEachIndexed
            }
            // 禁止ドメインを無視
            if (bannedDomains.any { campaignDomain.endsWith(it) }) {
                return@forEachIndexed
            }

            // indiegala, HRKGameは最新の投稿がある場合 過去のメッセージを削除する
            if (arrayOf("indiegala.com", "hrkgame.com").contains(campaignDomain)) {
                channels.forEach { channel ->
                    MessageCollector.filter { it.author.isSelfUser && it.textChannel.idLong == channel.idLong && it.embeds.firstOrNull()?.footer?.text == campaignDomain }.forEach { it.delete().queue({}, {}) }
                }
            }

            channels.forEach { channel ->
                val thumbnailUrl = it["data"].jsonObject.getOrNull("preview")?.jsonObject?.getOrNull("images")?.jsonArray?.get(0)?.jsonObject?.getOrNull("source")?.jsonObject?.getOrNull("url")?.string
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

                        color(Color.Giveaway)
                        timestamp()
                    }
                }.queue()
            }
        }
    }

    private suspend fun checkGiveawaySu(channels: List<TextChannel>) {
        val response = httpClient.get<String>("https://giveaway.su/status") {
            headers {
                append("Accept-Language", "ja")
                append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
            }
        }
        val json = response.toJsonArray()

        json.filter { it["type"].string == "local" }.sortedByDescending { it["id"].string.toInt() }.forEachIndexed { i, it ->
            val title = it["name"].string
            val id = it["id"].string.toInt()
            val url = "https://giveaway.su/giveaway/view/$id"

            // 古いGiveawayの投稿
            if (id <= lastGiveawaySuId) {
                return@forEachIndexed
            }
            if (i == 0) {
                lastGiveawaySuId = id
            }

            // 過去のメッセージを削除する
            channels.forEach { channel ->
                MessageCollector.filter { it.author.isSelfUser && it.textChannel.idLong == channel.idLong && it.embeds.firstOrNull()?.footer?.text == "giveaway.su" }.forEach { it.delete().queue({}, {}) }
            }

            channels.forEach { channel ->
                channel.message {
                    embed {
                        title(title)
                        author("Steam Giveaway Info", "https://www.reddit.com/r/FreeGamesOnSteam", "https://nephy.jp/assets/img/discord/giveaway.jpg")
                        description { url }


                        footer("giveaway.su")

                        color(Color.Giveaway)
                        timestamp()
                    }
                }.queue()
            }
        }
    }

    @Listener
    override fun onGuildMessageDelete(event: GuildMessageDeleteEvent) {
        val message = MessageCollector.latest(event.messageIdLong) ?: return
        if (!message.author.isSelfUser) {
            return
        }

        val steamGiveawayChannel = config.forGuild(event.guild)?.textChannel("steam_giveaway") ?: return

        val url = message.embeds.firstOrNull()?.description ?: return
        MessageCollector.filter { it.author.isSelfUser && it.textChannel.idLong == steamGiveawayChannel.idLong && it.embeds.firstOrNull()?.description == url }.forEach { it.delete().queue({}, {}) }
    }
}
