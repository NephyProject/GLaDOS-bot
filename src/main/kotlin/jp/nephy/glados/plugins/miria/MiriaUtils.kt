package jp.nephy.glados.plugins.miria

import jp.nephy.glados.core.extensions.await
import jp.nephy.glados.core.extensions.embedError
import jp.nephy.glados.core.extensions.messages.HexColor
import jp.nephy.glados.core.extensions.reject
import jp.nephy.glados.core.extensions.reply
import jp.nephy.glados.core.plugins.Plugin

object MiriaUtils: Plugin() {
    @Command(description = "", args = ["テキスト"], checkArgsCount = false)
    suspend fun checkWord(event: Command.Event) {
        val text = event.args
        reject(text.isBlank()) {
            event.embedError {
                "判定するテキストがブランクです。"
            }
        }

        val banned = BannedCollection.checkWordRules(text)
        if (banned == null) {
            event.reply {
                embed {
                    title("チェック結果")
                    description { "`$text` は BANワードを含んでいません。" }
                    color(HexColor.Good)
                    timestamp()
                }
            }
        } else {
            event.reply {
                embed {
                    title("チェック結果")
                    description { "`$text` は BANワードを含んでいます。\n  ワード: ${banned.word}\n  カテゴリ: ${banned.category}" }
                    color(HexColor.Bad)
                    timestamp()
                }
            }
        }.await()
    }
}
