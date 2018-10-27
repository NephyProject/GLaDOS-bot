package jp.nephy.glados.features.command

import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.embedError
import jp.nephy.glados.core.feature.rejectNull
import jp.nephy.glados.core.feature.subscription.Command
import jp.nephy.glados.core.feature.subscription.CommandEvent
import jp.nephy.glados.core.feature.subscription.Event
import jp.nephy.glados.core.launch
import jp.nephy.glados.dispatcher
import jp.nephy.glados.features.internal.MessageCollector
import jp.nephy.utils.StringLinkedListCache
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.ReadyEvent
import java.text.SimpleDateFormat
import java.util.*

class TimerCommand: BotFeature() {
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    // TODO
    private var activeTimers by StringLinkedListCache { emptyList() }

    @Event
    override suspend fun onReady(event: ReadyEvent) {
        delay(30000)
        for ((message, epoch) in activeTimers.decodeTimers()) {
            startTimer(message, epoch)
        }
    }

    @Command(aliases = ["time"], args = ["<期限(XdXhXmXs)>"], description = "指定時間のタイマーを開始します。", category = "タイマー")
    fun timer(event: CommandEvent) {
        val duration = calculateDurationSeconds(event.args)
        rejectNull(duration) {
            event.embedError("!timer") {
                "与えられた引数: `${event.args}`は不正です。`!help`を参照してください。"
            }
        }

        val calendar = Calendar.getInstance().also {
            it.add(Calendar.SECOND, duration)
        }
        startTimer(event.message, calendar.time.time)

        event.reply {
            embed {
                title("タイマー開始")
                description { "${timeFormat.format(calendar.time)} まで (${event.args.replace("d", "日").replace("h", "時間").replace("m", "分").replace("s", "秒")})" }
                color(Color.Good)
                timestamp()
            }
        }.launch()
    }

    private fun startTimer(message: Message, epoch: Long) {
        GlobalScope.launch(dispatcher) {
            activeTimers = activeTimers.decodeTimers().also {
                it[message] = epoch
            }.encodeTimers()

            delay(epoch - Date().time)
            message.reply {
                embed {
                    title("タイマー終了")
                    descriptionBuilder {
                        append("`${message.contentDisplay}`")
                    }
                    color(Color.Good)
                    timestamp()
                }
            }.launch()

            activeTimers = activeTimers.decodeTimers().also {
                it.remove(message)
            }.encodeTimers()
        }
    }

    private fun List<String>.decodeTimers(): MutableMap<Message, Long> {
        return mapNotNull {
            val (messageId, epoch) = it.split("+", limit = 2).map { it.toLong() }
            try {
                MessageCollector.latest(messageId)!! to epoch
            } catch (e: Exception) {
                null
            }
        }.toMap().toMutableMap()
    }

    private fun Map<Message, Long>.encodeTimers(): List<String> {
        return map { "${it.key.idLong}+${it.value}" }
    }
}
