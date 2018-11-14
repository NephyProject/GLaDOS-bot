package jp.nephy.glados.plugins.miria

import io.ktor.client.request.get
import io.ktor.client.response.HttpResponse
import io.ktor.http.encodeURLParameter
import jp.nephy.glados.*
import jp.nephy.glados.core.extensions.*
import jp.nephy.glados.core.plugins.Plugin
import jp.nephy.jsonkt.*
import jp.nephy.jsonkt.delegation.*
import jp.nephy.penicillin.core.PenicillinException
import jp.nephy.penicillin.core.TwitterErrorMessage
import jp.nephy.penicillin.models.Status
import kotlinx.coroutines.delay
import kotlinx.coroutines.io.readRemaining
import kotlinx.io.core.readText
import org.litote.kmongo.getCollectionOfName
import java.net.URLEncoder
import java.text.Normalizer
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.roundToInt

object ScheduledYannaiyo: Plugin() {
    private val account = config.twitterAccount("MiriaYannaiyo_Official")
    private val account2 = config.twitterAccount("Miria_Feedback")

    private val yahooApiKey = secret.forKey<String>("Yahoo_ApiKey")

    private val histories = mongodb.collection("MiriaYannaiyo")
    private val kusoripus = mongodb.getCollectionOfName<Kusoripu>("Kusoripu")

    private val replyRegex = "@\\w{4,}".toRegex()
    private val alphabetRegex = "^\\w+$".toRegex()
    private val urlRegex = "http(?:s)?://.+?(?:\\s|$)".toRegex()
    private val feelingJsonRegex = "YAHOO.JP.srch.rt.sentiment = (.+?)</script>".toRegex()

    // 基本ルール
    private val basicRule = WordRule("名詞")
    // 形態素解析後にやんないよツイートとして採用する連続する品詞
    private val passRules = arrayOf(
        // 体言
        WordRule("特殊,記号,*,#,#,#", "名詞"),  // #xxx
        WordRule("名詞", "特殊,単漢", "名詞"),  // xxx～yyy
        WordRule("接頭辞", "名詞"), WordRule("名詞", "接尾辞"), WordRule("名詞", "助詞,格助詞,*,と,と,と", "名詞"),  // xxxとyyy
        WordRule("形容詞,形容", "接尾辞,接尾さ"),  // ~さ
        WordRule("連体詞,連体", "名詞"),  // あのxxx
        WordRule("動詞", "名詞"),  // ~するxxx
        WordRule("動詞", "助動詞,助動詞た", "名詞"),  // ~したxxx
        WordRule("動詞", "助動詞,助動詞ない", "名詞"),  // ~しないxxx
        WordRule("名詞", "助詞,助詞その他", "助動詞,助動詞だ,体言接続", "名詞"),  // ~みたいなxxx
        WordRule("名詞", "助動詞,助動詞だ,体言接続", "名詞"), // xxxなxxx
        WordRule("名詞", "接尾辞,接尾", "助動詞,助動詞だ,体言接続", "名詞"), // xxxなxxx
        WordRule("形容詞", "名詞"),  // ~なxxx
        WordRule("形容動詞,形動", "助動詞,助動詞だ,体言接続,な,な,だ", "名詞"),  // ~なxxx

        // 助詞
        WordRule("名詞", "助詞,係助詞,*,しか"),  // xxxしか
        WordRule("名詞", "助詞,助詞連体化,*,の", "名詞"),  // xxxのyyy
        WordRule("名詞", "助詞,格助詞", "名詞,名サ自"),  // xxxをxxx
        WordRule("名詞", "助詞,並立助詞,*,とか", "名詞"),  // xxxとかyyy
        WordRule("動詞", "助詞,接続助詞,*,て,て,て"),  // ~して
        WordRule("名詞", "助詞,格助詞,*,から", "名詞"),  // xxxからyyy
        WordRule("名詞", "助詞,格助詞,*,で", "名詞"),  // xxxでyyy

        // 副詞
        WordRule("副詞,副詞,*", "名詞")  // 絶対xxx
    )
    // 単一品詞で構成される場合スキップする品詞
    private val skipWhenSingleRules = arrayOf("名詞,数詞", "名詞,人姓")
    // Yahoo感情分析スキップ品詞
    private val skipCheckFeelingRules = arrayOf("特殊", "助詞", "接尾辞", "接頭辞", "助動詞", "名詞,数詞")

    @Schedule(multipleMinutes = [15])
    suspend fun yannaiyo() {
        val start = Date()
        var sinceId: Long? = null

        // アイマス用語辞書
        val imasDictionary = httpClient.get<HttpResponse>("https://raw.githubusercontent.com/maruamyu/imas-ime-dic/master/dic.txt").use {
            it.content.readRemaining().readText(charset = Charsets.UTF_16LE)
        }.lines().asSequence().map { it.trim().split("\t") }.filter { it.size == 4 }.map {
            WordNode(it[1], it[0], "名詞,${it[2]},アイマス関連名詞", it[3])
        }.toList()

        val maxRetries = 15
        account.officialClient.use { client ->
            repeat(maxRetries) { i ->
                val timeline = try {
                    client.timeline.home(count = 200, sinceId = sinceId, tweetMode = "extended").await()
                } catch (e: Throwable) {
                    logger.error(e) { "タイムライン取得中にエラーが発生しました。 (${i + 1}/$maxRetries)" }
                    return@repeat
                }

                if (timeline.isEmpty()) {
                    logger.debug { "タイムラインが空でした。5秒待機します。 (${i + 1}/$maxRetries)" }
                    delay(5000)
                    return@repeat
                }

                for (status in timeline.shuffled()) {
                    status.registerKusoripu()

                    // BANワードチェック
                    val bannedWord = BannedCollection.checkWordRules(status.fullText())
                    if (bannedWord != null) {
                        logger.info { "https://twitter.com/${status.user.screenName}/status/${status.id} を無視しました。\n理由: `${bannedWord.word}` (${bannedWord.category}) を含むツイートであるため。" }
                        continue
                    }
                    // BANクライアントチェック
                    val bannedClient = BannedCollection.checkClientRules(status.source.name)
                    if (bannedClient != null) {
                        logger.info { "https://twitter.com/${status.user.screenName}/status/${status.id} を無視しました。\n理由: `${bannedClient.name}` (${bannedClient.category}) からのツイートであるため。" }
                        continue
                    }
                    // BANスクリーンネームチェック
                    val bannedUser = BannedCollection.checkUserRules(status.user.screenName)
                    if (bannedUser != null) {
                        logger.info { "https://twitter.com/${status.user.screenName}/status/${status.id} を無視しました。\n理由: @${bannedUser.screen_name} (${bannedUser.reason}) のツイートであるため。" }
                        continue
                    }

                    // 次のツイートは破棄
                    // 自分のツイート, メンション, RT, ふぁぼ済み, 非公開アカウントのツイート, 非日本語圏のツイート/ユーザ, アルファベットのみのツイート
                    val text = status.fullText()
                    if (status.user.screenName == "MiriaYannaiyo" || status.user.screenName == "Miria_Feedback" || text.contains("@") || status.favorited || status.user.protected || (status.lang.value != "ja" && status.user.lang.value != "ja") || alphabetRegex.matches(text)) {
                        continue
                    }

                    try {
                        if (createTweet(status, start, imasDictionary)) {
                            return
                        }
                    } catch (e: Throwable) {
                        logger.error(e) { "ツイートの生成中にエラーが発生しました." }
                    }
                }

                sinceId = timeline.first().id
            }
        }
    }

    private fun parseText(text: String): List<WordNode> {
        val result = arrayListOf<WordNode>()

        try {
            val xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse("https://jlp.yahooapis.jp/MAService/V1/parse?appid=$yahooApiKey&results=ma&response=surface,reading,feature&sentence=${URLEncoder.encode(text, Charsets.UTF_8.name())}")
            xml.documentElement.normalize()

            val words = xml.getElementsByTagName("word")
            repeat(words.length) {
                val word = words.item(it).childNodes
                result.add(WordNode(word.item(0).textContent, word.item(1).textContent, word.item(2).textContent))
            }
        } catch (e: Throwable) {
            logger.error(e) { "Yahoo! 形態素解析に失敗しました." }
        }

        return result
    }

    private fun filterWords(text: String, nodes: List<WordNode>, imasDictionary: List<WordNode>): Pair<ConcurrentHashMap<String, List<WordNode>>, List<WordNode>> {
        val moddedNodes = nodes.toCollection(CopyOnWriteArrayList())
        repeat(5) {
            passRules.forEach { rule -> rule.join(moddedNodes) }  // 5回フィルタを実行
        }

        val imasNodes = imasDictionary.asSequence().filter { it.surface in text }.map { listOf(it) }.toList()

        return (basicRule.finalize(moddedNodes) + imasNodes).asSequence().map { filteredNodes ->
            val word = filteredNodes.joinToString("") { it.surface }

            // ハッシュタグが動作するように空白を両端につける
            if (filteredNodes.first().surface.startsWith("#")) {
                " $word "
            } else {
                word.trim()
            } to filteredNodes
        }.filterNot { (_, value) ->
            // 単一品詞のみで構成されるエントリーを削除
            skipWhenSingleRules.any { rule -> value.all { it.feature.startsWith(rule) } }
        }.filterNot {
            // 1文字のエントリーを削除
            it.first.length == 1
        }.filterNot { (_, value) ->
            // 同じ単語が3回以上連続するエントリーを削除
            value.any { (surface) -> value.count { it.surface == surface } >= 3 }
        }.toList().toMap(ConcurrentHashMap()) to moddedNodes
    }

    private suspend fun checkFeeling(nodes: ConcurrentHashMap<String, List<WordNode>>): List<WordNode> {
        val cache = ConcurrentHashMap<String, YahooFeelingModel>()
        val deleted = arrayListOf<WordNode>()

        nodes.values.flatten().filterNot {
            // 感情解析をスキップ
                node ->
            skipCheckFeelingRules.any { node.feature.startsWith(it) }
        }.forEach {
            it.feeling = cache.getOrPut(it.surface) {
                try {
                    val result = httpClient.get<String>("https://search.yahoo.co.jp/realtime/search?p=${it.surface.encodeURLParameter()}")
                    feelingJsonRegex.find(result)?.groupValues?.get(1)?.parse<YahooFeelingModel>()
                } catch (e: Throwable) {
                    logger.error(e) { "Yahoo! 感情分析の取得中にエラーが発生しました." }
                    null
                } ?: return@forEach
            }

            if (it.feeling!!.scores.positivePercent < 10 && it.feeling!!.scores.negativePercent > 60) {
                logger.info { "ワード: ${it.surface} (${it.feature}) を取り除きました. (スコア: ${it.feeling!!.scores.json})" }
                it.deleted = true

                nodes.remove(it.surface)
            }
        }

        return deleted
    }

    fun choosePattern(word: String): Pair<String, Double> {
        val r = Random().nextDouble()
        return when (r) {
            in 0.0..0.1 -> {
                "みりあも${word}やるー"
            }
            in 0.1..0.15 -> {
                "みりあも${word}やーるー！"
            }
            in 0.15..0.95 -> {
                "みりあ${word}やんないよ"
            }
            else -> {
                "みりあも${word}やーらない！"
            }
        } to r
    }

    private fun Status.registerKusoripu() {
        val text = fullText()
        if (text.startsWith("@") || text.startsWith("RT @") || text.startsWith("QT @") || text.startsWith(".") || text.length < 30 || "http" in text || !replyRegex.containsMatchIn(text)) {
            return
        }

        val kusoripu = replyRegex.replace(text, "@@@")
        if (!kusoripus.contains("text" eq kusoripu)) {
            kusoripus.insertOne(Kusoripu(kusoripu))
            logger.info { "クソリプ: 「$kusoripu」 を登録しました." }
        }
    }

    private suspend fun createTweet(status: Status, start: Date, imasDictionary: List<WordNode>): Boolean {
        // 改行削除, URL削除, 正規化
        val text = Normalizer.normalize(status.fullText().replace("\n", " ").replace(urlRegex, "").trim(), Normalizer.Form.NFKC)

        val fullNodes = parseText(text)
        val (candidateNodes, moddedNodes) = filterWords(text, parseText(text), imasDictionary)
        val deletedNodes = checkFeeling(candidateNodes)
        if (candidateNodes.isEmpty()) {
            return false
        }

        val (tweetText, r) = choosePattern(candidateNodes.keys.shuffled().asSequence().sortedByDescending { it.length }.first())

        val tweetResult = account.officialClient.use { client ->
            try {
                logger.info { "定期ツイート: $tweetText" }

                client.status.update(tweetText).complete().also { result ->
                    account2.officialClient.use { client2 ->
                        client2.status.createPollTweet(
                            "このツイートに対してのフィードバック投票にご協力ください。得られた回答を基にみりあやんないよbotの改善に役立てていきます。", choices = listOf(
                                "問題なし", "内容が不十分", "不適切なツイート (アダルト)", "不適切なツイート (その他)"
                            ), minutes = 60, options = *arrayOf("in_reply_to_status_id" to result.result.id)
                        ).complete()
                    }

                    client.favorite.create(id = status.id).complete()
                }
            } catch (e: PenicillinException) {
                return when (e.error) {
                    TwitterErrorMessage.StatusIsADuplicate -> {
                        logger.error { "ツイートが重複しました: $tweetText" }
                        false
                    }
                    TwitterErrorMessage.ApplicationCannotPerformWriteActions -> {
                        logger.error { "書き込み操作が制限されています." }
                        true
                    }
                    else -> {
                        logger.error(e) { "定期ツイート中にエラーが発生しました." }
                        false
                    }
                }
            }
        }

        slack.message("#miriayannaiyo") {
            username(tweetText)
            icon("https://pbs.twimg.com/profile_images/727841615461539841/O6zdKT-Z_400x400.jpg")
            textBuilder {
                appendln("オリジナルツイート: `$text` (${status.source.name})")
                appendln("候補ワード: ${candidateNodes.keys}")
                appendln("抽出ワード:\n```\n${candidateNodes.map { node -> "${node.key} (${node.value.joinToString(" / ") { it.feature }})" }.joinToString("\n")}\n```")
                append("結合ノード:\n```\n${moddedNodes.joinToString("\n") { "${it.surface} (${it.feature})" }}\n```")
            }
        }

        try {
            val finish = Date()
            histories.insertOne(
                "sec" to (finish.time - start.time) / 1000.0,
                "datetime" to finish.toString("MM/dd HH:mm:ss"),
                "tweetLink" to "https://twitter.com/${status.user.screenName}/status/${status.id}",
                "r" to r,
                "chose" to tweetText,
                "words" to candidateNodes.keys,
                "via" to status.source.name,
                "original" to status.fullText,
                "url" to "https://twitter.com/${tweetResult.result.user.screenName}/status/${tweetResult.result.id}",
                "node" to serializeNodes(fullNodes),
                "deletedNode" to serializeNodes(deletedNodes)
            )
        } catch (e: Throwable) {
            logger.error(e) { "DBへの記録に失敗しました." }
        }

        return true
    }

    private fun serializeNodes(nodes: List<WordNode>): List<Map<String, Any?>> {
        return nodes.map {
            mapOf(
                "surface" to it.surface, "reading" to it.reading, "feature" to it.feature, "description" to it.description, "deleted" to it.deleted, "feeling" to if (it.feeling == null) {
                    null
                } else {
                    mapOf(
                        "active" to it.feeling!!.active, "scores" to mapOf<String, Any?>(
                            "positive" to it.feeling!!.scores.positive,
                            "negative" to it.feeling!!.scores.negative,
                            "neutral" to it.feeling!!.scores.neutral,
                            "total" to it.feeling!!.scores.total,
                            "negative_percent" to it.feeling!!.scores.negativePercent,
                            "neutral_percent" to it.feeling!!.scores.neutralPercent,
                            "positive_percent" to it.feeling!!.scores.positivePercent
                        )
                    )
                }
            )
        }
    }

    private data class YahooFeelingModel(override val json: ImmutableJsonObject): JsonModel {
        val scores by model<Scores>()
        val active by string

        data class Scores(override val json: ImmutableJsonObject): JsonModel {
            val positive by int
            val negative by int
            val neutral by int
            val total by lazy { positive + negative + neutral }
            val negativePercent by lazy { (100.0 * negative / total).roundToInt() }
            val positivePercent by lazy { (100.0 * positive / total).roundToInt() }
            val neutralPercent by lazy { (100.0 * neutral / total).roundToInt() }
        }
    }

    private data class Kusoripu(val text: String)

    private data class WordNode(
        val surface: String,  // 単語
        val reading: String,  // 単語読み
        val feature: String,  // 品詞情報
        val description: String? = null,  // アイマス関連名詞の説明
        var feeling: YahooFeelingModel? = null,  // Yahoo 感情解析結果
        var deleted: Boolean = false
    )

    private class WordRule(private vararg val wordClasses: String) {
        private val first = wordClasses.first()
        private val lastRuleIndex = wordClasses.size - 1

        private var matching = false
        private var ruleIndex = 0
        private val currentCandidate = arrayListOf<WordNode>()

        private fun reset() {
            matching = false
            ruleIndex = 0
            currentCandidate.clear()
        }

        fun join(nodes: CopyOnWriteArrayList<WordNode>) {
            reset()

            nodes.toList().forEach { node ->
                if (lastRuleIndex < ruleIndex) {  // ルールのサイズを超過
                    reset()
                } else if (!matching && node.feature.startsWith(first)) {  // マッチ開始
                    matching = true
                    ruleIndex++
                    currentCandidate.add(node)
                } else if (matching && node.feature.startsWith(wordClasses[ruleIndex])) {  // ruleIndex番目のルールをチェック
                    currentCandidate.add(node)
                    if (ruleIndex == lastRuleIndex) {  // ルールの最後
                        val startIndex = nodes.indexOf(currentCandidate.first())
                        if (startIndex != -1) {
                            nodes[startIndex] = WordNode(currentCandidate.joinToString("") { it.surface }, currentCandidate.joinToString("") { it.reading }, "名詞(結合[${currentCandidate.joinToString(" / ") { it.feature }}])")
                            nodes.removeAll(currentCandidate)
                        }

                        reset()
                    } else {
                        ruleIndex++
                    }
                } else {
                    reset()
                }
            }

            // 固める
            if (this != basicRule) {
                basicRule.join(nodes)
            }
        }

        fun finalize(nodes: MutableList<WordNode>): List<List<WordNode>> {
            val found = arrayListOf<List<WordNode>>()
            reset()

            fun append() {
                if (currentCandidate.isNotEmpty()) {
                    val startIndex = nodes.indexOf(currentCandidate.first())
                    if (startIndex != -1) {
                        nodes[startIndex] = WordNode(currentCandidate.joinToString("") { it.surface }, currentCandidate.joinToString("") { it.reading }, "名詞(結合[${currentCandidate.joinToString(" / ") { it.feature }}])")
                        nodes.removeAll(currentCandidate)
                        found.add(currentCandidate.toList())
                    }
                }
            }

            for (it in nodes) {
                if (it.feature.startsWith(first)) {
                    currentCandidate.add(it)
                } else {
                    append()
                    reset()
                }
            }
            append()

            return found.asSequence().toSet().toList()
        }
    }
}
