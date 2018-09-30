package jp.nephy.glados.features

import io.ktor.http.HttpHeaders
import jp.nephy.glados.core.builder.message
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Loop
import jp.nephy.glados.core.feature.textChannelsLazy
import jp.nephy.utils.IntLinkedSingleCache
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class AtCoderNewContests: BotFeature() {
    private val httpClient = OkHttpClient()
    private var lastAtCoderPost by IntLinkedSingleCache { 273 }
    private val atCoderTextChannels by textChannelsLazy("atcoder")

    @Loop(3, TimeUnit.MINUTES)
    fun check() {
        val content = httpClient.newCall(
                Request.Builder()
                        .url("https://beta.atcoder.jp")
                        .header(HttpHeaders.AcceptLanguage, "ja")
                        .header(HttpHeaders.UserAgent, "GLaDOS-bot/1.0 (https://github.com/SlashNephy/GLaDOS-bot)")
                        .build()
        ).execute().body()!!.string()
        val html = Jsoup.parse(content)

        for (element in html.getElementsByClass("panel panel-default")) {
            val h3 = element.getElementsByClass("panel-title").first()
            val title = h3.text()
            val path = h3.getElementsByTag("a").attr("href")

            val id = path.split("/").last().toInt()
            if (id <= lastAtCoderPost) {
                continue
            }
            lastAtCoderPost = id

            val body = element.getElementsByClass("panel-body blog-post").first().wholeText().split("\r\n\r\n")
            val description = body[0].replace("[", "").replace("]", " ").replace("(", "").replace(")", "")

            for (channel in atCoderTextChannels) {
                channel.message {
                    embed {
                        title(title, "https://beta.atcoder.jp$path")
                        description { description }
                        for (li in body[1].split("\n")) {
                            val liElements = li.trim().removePrefix("-").split("[:：]".toRegex())
                            if (liElements.size < 2) {
                                continue
                            }

                            val liTitle = liElements.first().trim()
                            val liText = Jsoup.parse(liElements.drop(1).joinToString(":")).text()
                            field(liTitle) { liText }
                        }
                        timestamp()
                    }
                }.queue()
            }
        }
    }
}
