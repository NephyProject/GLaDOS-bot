package jp.nephy.glados.feature.command

import com.jagrosh.jdautilities.command.CommandEvent
import jp.nephy.glados.component.audio.music.*
import jp.nephy.glados.component.helper.Color
import jp.nephy.glados.component.helper.deleteQueue
import jp.nephy.glados.component.helper.embedMention
import jp.nephy.glados.component.helper.toMilliSecondString
import jp.nephy.glados.feature.CommandFeature
import jp.nephy.utils.characterLength
import jp.nephy.utils.sumBy
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.requests.restaction.MessageAction
import java.util.concurrent.TimeUnit


class Queue: CommandFeature() {
    init {
        name = "queue"
        help = "現在の再生キューを取得します。"

        isMusicCommand = true
        requireSameChannel = true
    }

    companion object {
        fun respondQueue(channel: TextChannel, member: Member, guildPlayer: GuildPlayer): MessageAction {
            return channel.embedMention(member) {
                author("♪ 再生中のトラックとキュー")
                blankField()

                val playing = guildPlayer.controls.currentTrack
                val queue = guildPlayer.controls.queue
                var ms = playing?.remaining ?: 0

                if (playing != null) {
                    title(playing.info.effectiveTitle, playing.info.uri)
                    descriptionBuilder {
                        if (playing.type != TrackType.UserRequest) {
                            appendln("このトラックは自動再生です。ユーザがリクエストしたトラックではありません。")
                            appendln("曲をリクエストすると このトラックの再生は中断されます。")
                        }
                        appendln("by ${playing.info.author}")
                        append("再生位置: ${playing.position.toMilliSecondString()} / ${playing.duration.toMilliSecondString()}")
                    }

                    if (playing.youtubedl.info?.thumbnailUrl != null) {
                        thumbnail(playing.youtubedl.info?.thumbnailUrl!!)
                    }
                } else {
                    title("再生中のトラックはありません。")
                }
                field("キューに入っているトラック") {
                    "総トラック数: ${queue.size}, 総再生時間: ${queue.sumBy { it.duration }.toMilliSecondString()}"
                }
                queue.take(20).forEachIndexed { i, it ->
                    field("#${(i + 1).toString().padEnd(queue.size.characterLength)}: ${it.info.effectiveTitle}") {
                        "長さ: ${it.duration.toMilliSecondString()} / 再生までおよそ ${ms.toMilliSecondString()}"
                    }
                    ms += it.duration
                }
                if (queue.size > 20) {
                    field("...") { "#20以降のトラックは省略されました。" }
                }
                color(Color.Good)
                timestamp()
            }
        }
    }

    override fun executeCommand(event: CommandEvent) {
        respondQueue(event.textChannel, event.member, guildPlayer).deleteQueue(30, TimeUnit.SECONDS)
    }
}
