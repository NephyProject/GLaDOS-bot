package jp.nephy.glados.feature.listener

import jp.nephy.glados.component.helper.Color
import jp.nephy.glados.component.helper.embedMessage
import jp.nephy.glados.component.helper.getMessages
import jp.nephy.glados.component.helper.isSelf
import jp.nephy.glados.feature.ListenerFeature
import jp.nephy.glados.logger
import jp.nephy.jsonkt.*
import jp.nephy.utils.FloatLinkedSingleCache
import jp.nephy.utils.IntLinkedSingleCache
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


class GiveawayChecker: ListenerFeature() {
    private var lastRedditGiveawayCreated by FloatLinkedSingleCache { 0F }
    private var lastGiveawaySuId by IntLinkedSingleCache { 0 }

    private val headers = Headers.Builder()
            .add("Accept-Language", "ja")
            .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
            .build()
    private val client = OkHttpClient()

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
            "twitch.tv"
    )

    override fun onReady(event: ReadyEvent) {
        val channels = bot.config.guilds.mapNotNull { it.textChannel.giveaway }.map { event.jda.getTextChannelById(it) }

        thread(name = "Clean Up Spams") {
            channels.forEach {
                it.getMessages(100)
                        .filter { it.author.isSelf && bannedDomains.any { ban -> it.embeds.firstOrNull()?.footer?.text?.endsWith(ban) == true } }
                        .forEach {
                            it.delete().queue()
                        }
            }
        }

        thread(name = "Giveaway Checker") {
            while (true) {
                try {
                    checkReddit(channels)
                    checkGiveawaySu(channels)
                } catch (e: Exception) {
                    logger.error(e) { "Giveawayのチェック中に例外が発生しました." }
                }

                TimeUnit.MINUTES.sleep(1)
            }
        }
    }

    private fun checkReddit(channels: List<TextChannel>) {
        val response = client.newCall(
                Request.Builder()
                        .url("https://www.reddit.com/r/FreeGamesOnSteam/new.json")
                        .headers(headers)
                        .build()
        ).execute()
        val content = response.body()!!.string()

        val json = JsonKt.toJsonObject(content)

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
                        bot.messageCacheManager.delete { it.author.isSelf && it.textChannel.idLong == channel.idLong && it.embeds.firstOrNull()?.description == redditUrl }
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
            // ストアから削除されたゲームを無視
            if (title.startsWith("[Removed Game]")) {
                return@forEachIndexed
            }
            // 禁止ドメインを無視
            if (bannedDomains.any { campaignDomain.endsWith(it) }) {
                return@forEachIndexed
            }

            // indiegala, HRKGameは最新の投稿がある場合 過去のメッセージを削除する
            if (arrayOf("indiegala.com", "hrkgame.com").contains(campaignDomain)) {
                channels.forEach { channel ->
                    bot.messageCacheManager.delete { it.author.isSelf && it.textChannel.idLong == channel.idLong && it.embeds.firstOrNull()?.footer?.text == campaignDomain }
                }
            }

            channels.forEach { channel ->
                val thumbnailUrl = it["data"].jsonObject.getOrNull("preview")?.jsonObject?.getOrNull("images")?.jsonArray?.get(0)?.jsonObject?.getOrNull("source")?.jsonObject?.getOrNull("url")?.string
                channel.embedMessage {
                    title(title)
                    author("Steam Giveaway Info", "https://giveaway.nephy.jp", "https://nephy.jp/assets/img/page/discord/giveaway.jpg")
                    description { campaignUrl }

                    field("Reddit 元記事") { redditUrl }

                    footer(campaignDomain)

                    if (thumbnailUrl != null) {
                        thumbnail(thumbnailUrl)
                    }

                    color(Color.Giveaway)
                    timestamp()
                }.queue()
            }
        }
    }

    private fun checkGiveawaySu(channels: List<TextChannel>) {
        val response = client.newCall(
                Request.Builder()
                        .url("https://giveaway.su/status")
                        .headers(headers)
                        .build()
        ).execute()
        val content = response.body()!!.string()
        val json = JsonKt.toJsonArray(content)

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
                bot.messageCacheManager.delete { it.author.isSelf && it.textChannel.idLong == channel.idLong && it.embeds.firstOrNull()?.footer?.text == "giveaway.su" }
            }

            channels.forEach { channel ->
                channel.embedMessage {
                    title(title)
                    author("Steam Giveaway Info", "https://giveaway.nephy.jp", "https://nephy.jp/assets/img/page/discord/giveaway.jpg")
                    description { url }


                    footer("giveaway.su")

                    color(Color.Giveaway)
                    timestamp()
                }.queue()
            }
        }
    }

    override fun onGuildMessageDelete(event: GuildMessageDeleteEvent) {
        val message = bot.messageCacheManager.get(event.messageIdLong) ?: return
        if (! message.author.isSelf) {
            return
        }

        val config = bot.config.getGuildConfig(event.guild)
        if (message.textChannel.idLong != config.textChannel.giveaway) {
            return
        }

        val url = message.embeds.firstOrNull()?.description ?: return
        bot.messageCacheManager.delete { it.author.isSelf && it.textChannel.idLong == config.textChannel.giveaway && it.embeds.firstOrNull()?.description == url }
    }
}
