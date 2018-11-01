package jp.nephy.glados.plugins

import jp.nephy.glados.config
import jp.nephy.glados.core.extensions.launchAndDelete
import jp.nephy.glados.core.extensions.messages.HexColor
import jp.nephy.glados.core.extensions.reply
import jp.nephy.glados.core.isFalseOrNull
import jp.nephy.glados.core.plugins.Plugin
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import org.jsoup.Jsoup
import java.net.URL
import java.util.concurrent.TimeUnit

object NiconicoDict: Plugin() {
    private val pattern = "^(.+?)(?:#)?とは$".toRegex()
    private val transportPattern = "location.replace\\('(.+?)'\\)".toRegex()

    override suspend fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (config.forGuild(event.guild)?.boolOption("enable_niconico_dict").isFalseOrNull()) {
            return
        }
        val match = pattern.matchEntire(event.message.contentDisplay) ?: return

        val word = match.groupValues[1]
        var articleUrl = "http://dic.nicovideo.jp/a/$word"
        var content = URL(articleUrl).readText()
        val transport = transportPattern.find(content)
        if (transport != null) {
            articleUrl = transport.groupValues[1]
            content = URL(articleUrl).readText()
        }

        val doc = Jsoup.parse(content)

        if (!doc.html().contains("まだ記事が書かれていません")) {
            val result = linkedMapOf<String, String>()
            var lastSection: String? = null
            var description: String? = ""

            doc.select("#article").first().children()
                    .filter { it.attr("id") != "page-menu" && !arrayOf("div", "ul", "table").contains(it.tagName()) }
                    .takeWhile { !arrayOf("関連動画", "関連項目", "記事編集").contains(it.text()) }
                    .takeWhile { !it.attr("class").startsWith("adsense-") }
                    .forEach {
                        when {
                            arrayOf("h1", "h2", "h3").contains(it.tagName()) -> {
                                lastSection = it.text()
                                result[lastSection!!] = ""
                            }
                            lastSection.isNullOrEmpty() -> {
                                description += it.text().trim()
                            }
                            else -> {
                                result[lastSection!!] = result.getOrDefault(lastSection!!, "") + it.text().trim()
                            }
                        }
                    }

            var text = "$description\n\n"
            result.filterNot { it.value.isBlank() }.forEach { k, v ->
                text += "$k\n    $v"
            }

            event.channel.reply(event.member) {
                embed {
                    title("$word とは")
                    author("ニコニコ大百科", articleUrl, "http://dic.nicovideo.jp/img/nv.gif")
                    description { "${text.take(500)}..." }
                    timestamp()
                    color(HexColor.Niconico)
                }
            }
        } else {
            event.channel.reply(event.member) {
                embed {
                    title("$word とは")
                    author("ニコニコ大百科", articleUrl, "http://dic.nicovideo.jp/img/nv.gif")
                    description { "記事が見つかりませんでした。" }
                    timestamp()
                    color(HexColor.Niconico)
                }
            }
        }.launchAndDelete(2, TimeUnit.MINUTES)
    }
}
