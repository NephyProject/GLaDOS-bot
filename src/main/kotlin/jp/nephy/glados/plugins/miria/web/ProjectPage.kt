package jp.nephy.glados.plugins.miria.web

import com.mongodb.client.model.Sorts
import jp.nephy.glados.core.extensions.collection
import jp.nephy.glados.core.extensions.findAndParse
import jp.nephy.glados.core.extensions.web.*
import jp.nephy.glados.core.plugins.Plugin
import jp.nephy.glados.mongodb
import jp.nephy.glados.plugins.web.addons.*
import jp.nephy.glados.plugins.web.template.respondLayout
import kotlinx.html.*
import kotlin.collections.set
import kotlin.math.roundToInt

object ProjectPage: Plugin() {
    @Web.Page("/projects/miria", "nephy.jp")
    override suspend fun onAccess(event: Web.AccessEvent) {
        event.respondLayout {
            meta {
                title("みりあやんないよBot | Nephy Project")
                description("Nephy Project Teamが開発中のTwitter bot「みりあやんないよBot(@MiriaYannaiyo)」についてまとめています。")
            }

            addon {
                install<GoogleAnalytics>()
                install<jQuery>()
                install<Bootstrap4>()
                install<Bootstrap4JS>()
                install<MathJax>()
                install<FontCSS>()
            }

            content {
                div(classes = "container") {
                    div(classes = "row") {
                        div(classes = "col-md-3") {
                            div(classes = "card") {
                                ul(classes = "list-group list-group-flush") {
                                    li(classes = "list-group-item") {
                                        a(href = "#welcome") {
                                            +"はじめに"
                                        }
                                    }
                                    li(classes = "list-group-item") {
                                        a(href = "#features") {
                                            +"機能"
                                        }
                                    }
                                    li(classes = "list-group-item") {
                                        a(href = "#result") {
                                            +"最近のツイート"
                                        }
                                    }
                                    li(classes = "list-group-item") {
                                        a(href = "#api") {
                                            +"API"
                                        }
                                    }
                                    li(classes = "list-group-item") {
                                        a(href = "#ask") {
                                            +"お問い合わせ"
                                        }
                                    }
                                }
                            }
                        }

                        div(classes = "col-md-9") {
                            div(id = "welcome") {
                                h2 {
                                    i(classes = "fas fa-angle-double-right")
                                    +" はじめに"
                                }
                                p {
                                    +"このBotは, 15分に一度"
                                    i {
                                        +"following"
                                    }
                                    +"のツイートを拾って「みりあやんないよ」をツイートするTwitter Botです。"
                                    a(href = "https://twitter.com/MiriaYannaiyo", target = "_blank") {
                                        +"@MiriaYanniyo"
                                    }
                                }
                                p {
                                    a(href = "https://twitter.com/MiriaYannaiyo", classes = "twitter-follow-button") {
                                        +"Follow @MiriaYannaiyo"
                                    }
                                }
                                p {
                                    +"©2003-2018 BNEI"
                                }

                                p {
                                    +"ソースコードは "
                                    a(href="https://github.com/NephyProject/GLaDOS-bot")
                                }
                            }

                            hr()

                            div(id = "features") {
                                h2 {
                                    i(classes = "fas fa-star")
                                    +" 機能"
                                }
                                ul {
                                    li {
                                        h3 {
                                            +"15分に一度 (毎時0分, 15分, 30分, 45分頃), "
                                            i {
                                                +"following"
                                            }
                                            +"のツイートから「みりあやんないよ」を投稿します"
                                        }
                                        p {
                                            +"TLのランダムなツイートを形態素解析して自然な日本語を切り取り「みりあ○○やんないよ」の形にします。"
                                            +"みりあやんないよbotは, 利用させていただいたツイートをお気に入り登録します。"
                                        }
                                        p {
                                            +"定期ツイートなどの自動ツイートの類いは除外してします。また 不適切ワードをDB化し除外するようになっています。"
                                        }
                                        p {
                                            +"不適切な投稿を防止するため Yahoo! の感情解析の結果を利用するようになりました。(2018/2/24)"
                                        }
                                        p {
                                            +"80%の確率で「みりあ○○やんないよ」を選択しますが これだけではなくレアケースとして"
                                            b {
                                                +"10%の確率で「みりあも○○やるー」"
                                            }
                                            +"を さらに"
                                            b {
                                                +"5%の確率で「みりあも○○やーるー！」"
                                            }
                                            +"を そして残りの"
                                            b {
                                                +"5%の確率で「みりあも○○やーらない！」"
                                            }
                                            +"を選択します。"
                                        }
                                        p {
                                            +"この確率を表すパラメータをパターン決定係数 $ r $ と呼び, $ r $ は $ 0 \\leq r < 1 \$の乱数で「みりあ○○やんないよ」などのテンプレートを決定します。例えば, $ 0 \\leq r < 0.1 \$なら「みりあも○○やるー」, $ 0.1 \\leq r < 0.95 \$なら「みりあ○○やんないよ」, $ 0.95 \\leq r < 1\$なら「みりあも○○やーらない！」が採用されます."
                                        }
                                        br
                                        p {
                                            +"以下, 技術的備考です。"
                                        }
                                        p {
                                            +"形態素解析には, 2017/5/2以前はMeCabを利用していましたが, より高い精度のため"
                                            a(href = "https://developer.yahoo.co.jp/webapi/jlp/", target = "_blank") {
                                                b {
                                                    +"Yahoo!のテキスト解析"
                                                }
                                            }
                                            +"を使用しています。"
                                        }
                                        p {
                                            +"MeCab時代では, 単純に名詞のみを切り取っていましたが, 今では日本語が自然になるように「みりあやんないよ」に接続するように改善しました。詳しい内容はソースコードをご覧ください。"
                                        }
                                    }
                                    li {
                                        h4 {
                                            +"アイマス関連用語を重み付けし アイマス関連の話題に反応しやすくします"
                                        }
                                        p {
                                            a(href = "https://imas-db.jp/") {
                                                +"THE IDOLM@STER データベース"
                                            }
                                            +"様が作成しているIME辞書データを組み込んでおり, アイマス関連用語を優先的に採用するようにしています。"
                                        }
                                    }
                                    li {
                                        h4 {
                                            +"リプライに反応します"
                                        }
                                        p {
                                            +"みりあやんないよBotのツイートに対し「やって」「やんないで」などとリプライを飛ばすと反応します。"
                                        }
                                        p {
                                            +"スパムになってしまうのを防止するため, 1つのリプライに対し5分間のクールダウンを設けています。"
                                            +"多量のリプライを送るアカウントに対してはブラックリストに追加し, 反応しないように対処しますので予めご了承ください。(2018/11/14)"
                                        }
                                    }
                                    li {
                                        h4 {
                                            +"毎分フォロー返しを行います"
                                        }
                                        p {
                                            +"フォロー返しが行われていないときには「フォロバ」とリプライを送ってみてください。なお, フォロー規制に掛かっている場合がありますのでご了承ください。"
                                        }
                                    }
                                }
                            }

                            hr(classes = "hr")

                            div(id = "result") {
                                h2 {
                                    i(classes = "fas fa-list-ul")
                                    +" 最近の形態素解析 / 感情分析結果 (最新10件)"
                                }

                                div(classes = "card") {
                                    div(classes = "card-body") {
                                        val data = mongodb.collection("MiriaYannaiyo").findAndParse<MorphologicalAnalysisResult> { sort(Sorts.descending("_id")).limit(10) }
                                        data.forEach {
                                            div(classes = "card") {
                                                div(classes = "card-header") {
                                                    span(classes = "badge badge-secondary") { +it.datetime }
                                                    span { +it.chose }
                                                    if (it.url == null) {
                                                        span(classes = "badge badge-danger") { +"デバッグ" }
                                                    }
                                                }

                                                div(classes = "card-body") {
                                                    if (it.url != null) {
                                                        center {
                                                            blockQuote(classes = "twitter-tweet") {
                                                                attributes["data-lang"] = "ja"
                                                                p {
                                                                    attributes["lang"] = "ja"
                                                                    attributes["dir"] = "ltr"
                                                                    +it.chose
                                                                }
                                                                +"&mdash; みりあやんないよbot (@MiriaYannaiyo) "
                                                                a(href = "${it.url}?ref_src=twsrc%5Etfw")
                                                            }
                                                        }
                                                    }

                                                    p {
                                                        +"単語の候補は ${it.words.joinToString(", ") { "「$it」" }} でした。元のツイートは "
                                                        b { +it.via }
                                                        +" から投稿されました。"
                                                    }
                                                    p {
                                                        +"このツイートの生成に ${it.sec.roundToInt()}秒かかりました。パターン決定係数は \$ r = ${it.r} \$ でした。"
                                                    }

                                                    div(classes = "right") {
                                                        button(type = ButtonType.button, classes = "btn btn-success btn-sm original-trigger", dataToggle = "collapse") {
                                                            +"原文ツイートを表示"
                                                        }
                                                        button(type = ButtonType.button, classes = "btn btn-info btn-sm feature-trigger", dataToggle = "collapse") {
                                                            +"品詞情報を展開"
                                                        }
                                                        button(type = ButtonType.button, classes = "btn btn-danger btn-sm rejected-feature-trigger", dataToggle = "collapse") {
                                                            +"リジェクトされた自立語一覧を展開"
                                                        }
                                                    }
                                                }

                                                div(classes = "card-collapse collapse out") {
                                                    div(classes = "card-body") {
                                                        hr()

                                                        p {
                                                            i(classes = "fab fa-twitter")
                                                            +" "
                                                            b { u { +"原文ツイート" } }
                                                        }
                                                        center {
                                                            blockQuote(classes = "twitter-tweet") {
                                                                attributes["data-lang"] = "ja"
                                                                p {
                                                                    attributes["lang"] = "ja"
                                                                    attributes["dir"] = "ltr"
                                                                    +it.original
                                                                }
                                                                +"&mdash; "
                                                                a(href = "${it.tweetLink}?ref_src=twsrc%5Etfw")
                                                            }
                                                        }
                                                    }
                                                }

                                                div(classes = "card-collapse collapse out") {
                                                    div(classes = "card-body") {
                                                        hr()

                                                        table(classes = "table") {
                                                            p {
                                                                i(classes = "far fa-list-alt")
                                                                +" "
                                                                b { u { +"品詞情報テーブル" } }
                                                            }

                                                            buildNodeTable(it.node)
                                                        }
                                                    }
                                                }

                                                div(classes = "card-collapse collapse out") {
                                                    div(classes = "card-body") {
                                                        hr()

                                                        table(classes = "table") {
                                                            p {
                                                                i(classes = "far fa-list-alt")
                                                                +" "
                                                                b { u { +"リジェクトされた品詞情報テーブル" } }
                                                            }
                                                            p { +"感情分析の結果, ネガティブが圧倒する場合はリジェクトされます。" }

                                                            buildNodeTable(it.deletedNode)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                inlineCSS("/assets/css/miria.css")
                                inlineJS("/assets/js/miria.js")
                            }

                            hr(classes = "hr")

                            div(id = "api") {
                                h2 {
                                    i(classes = "fas fa-code")
                                    +" 公開API"
                                }
                                p {
                                    p { +"みりあやんないよbotでは 過去の形態素解析結果やツイート内容を取得できるJson APIを用意しています。" }
                                    p {
                                        +"詳しくは "
                                        a(href = "https://api.nephy.jp") { +"Nephy Project APIリファレンス" }
                                        +" をご覧ください。"
                                    }
                                }
                            }

                            hr(classes = "hr")

                            div(id = "ask") {
                                h2 {
                                    i(classes = "far fa-envelope")
                                    +" お問い合わせ"
                                }
                                p {
                                    +"みりあやんないよBotに TwitterでDMを送るか "
                                    code {
                                        +"akagi.miria※ya.ru"
                                    }
                                    +" までメールでお願いします (※を@に変えてください)。急用の案件はメールでお願いします。"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun TABLE.buildNodeTable(nodeList: List<MorphologicalAnalysisResult.Node>) {
        thead {
            tr {
                th { +"単語 (よみ)" }
                td { +"品詞" }
                td { +"感情分析" }
                td { +"イメージ" }
            }
        }
        tbody {
            nodeList.forEach {
                tr {
                    th {
                        if (it.surface != it.reading) {
                            +"${it.surface} (${it.reading})"
                        } else {
                            +it.surface.orEmpty()
                        }
                        if (it.deleted == true) {
                            span(classes = "badge badge-warning") { +"リジェクト" }
                        }
                    }
                    td {
                        +it.feature.orEmpty()
                        if (it.feature.orEmpty().contains("アイマス関連名詞")) {
                            span(classes = "badge badge-primary") { +"アイマス関連用語" }
                            p { +"${it.description} (出典: ${it.category})" }
                        }
                    }
                    td {
                        if (it.feeling != null) {
                            div(classes = "progress") {
                                div(classes = "progress-bar progress-bar-striped bg-success", role = "progressbar", style = "width: ${it.feeling!!.scores.positivePercent}%;") {
                                    span { +"${it.feeling!!.scores.positivePercent}%" }
                                }
                                div(classes = "progress-bar bg-empty", role = "progressbar", style = "width: ${it.feeling!!.scores.neutralPercent}%;")
                                div(classes = "progress-bar progress-bar-striped bg-danger", role = "progressbar", style = "width: ${it.feeling!!.scores.negativePercent}%;") {
                                    span {
                                        i(classes = "far fa-frown")
                                        +" ${it.feeling!!.scores.negativePercent}%"
                                    }
                                }
                            }
                        }
                    }
                    td {
                        when (it.feeling?.active) {
                            "positive" -> {
                                +"-> "
                                span(classes = "plus") { +"プラス" }
                            }
                            "negative" -> {
                                +"-> "
                                span(classes = "minus") { +"マイナス" }
                            }
                            else -> +"-"
                        }
                    }
                }
            }
        }
    }
}
