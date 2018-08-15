package jp.nephy.glados.features

import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.edit
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Command
import jp.nephy.glados.core.feature.subscription.CommandEvent
import jp.nephy.glados.core.feature.subscription.CommandPermission
import javax.script.ScriptEngineManager
import kotlin.system.measureTimeMillis

class EvalCommand: BotFeature() {
    @Command(permission = CommandPermission.OwnerOnly, description = "JavaScriptを実行します。", args = "<JavaScriptコード>")
    fun eval(event: CommandEvent) {
        event.reply {
            embed {
                title("実行中...")
                description { "> `${event.args}`" }
                color(Color.Plain)
            }
        }.queue {
            it.edit {
                embed {
                    title("実行結果")
                    try {
                        val engine = ScriptEngineManager().getEngineByName("Nashorn").apply {
                            put("event", event)
                            put("jda", event.jda)
                            put("guild", event.guild)
                            put("channel", event.channel)
                            put("member", event.member)
                        }

                        val time = measureTimeMillis {
                            descriptionBuilder {
                                appendln("> `${event.args}`")
                                append(engine.eval(event.args))
                            }
                        }
                        footer("実行時間: ${time}ms")

                        color(Color.Good)
                    } catch (e: Exception) {
                        descriptionBuilder {
                            appendln("失敗しました:")
                            append(e.localizedMessage)
                        }

                        color(Color.Bad)
                    }
                }
            }.queue()
        }
    }
}
