package jp.nephy.glados.plugins.kaigen

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import jp.nephy.glados.core.extensions.launch
import jp.nephy.glados.core.extensions.message
import jp.nephy.glados.core.extensions.textChannelsLazy
import jp.nephy.glados.core.plugins.Plugin
import jp.nephy.glados.httpClient
import jp.nephy.glados.userAgent
import jp.nephy.utils.IntLinkedSingleCache
import org.jsoup.Jsoup

object AtCoderNewContests: Plugin() {
    private var lastAtCoderPost by IntLinkedSingleCache { 0 }
    private val atCoderTextChannels by textChannelsLazy("atcoder")

    @Schedule(multipleHours = [1], multipleMinutes = [2])
    suspend fun check() {
        val content = httpClient.get<String>("https://beta.atcoder.jp") {
            header(HttpHeaders.AcceptLanguage, "ja")
            header(HttpHeaders.UserAgent, userAgent)
        }
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
                }.launch()
            }
        }
    }
}
