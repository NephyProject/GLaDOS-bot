package jp.nephy.glados.features

import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Listener
import jp.nephy.glados.core.feature.subscription.Priority
import jp.nephy.glados.core.getHistory
import jp.nephy.glados.core.tmpFile
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.MessageUpdateEvent
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException
import java.nio.file.Files

class MessageCollector: BotFeature() {
    companion object {
        private val cache = mutableMapOf<Long, Message>()
        private val history = mutableMapOf<Long, Message?>()

        fun add(message: Message) {
            cache[message.idLong] = message
        }

        fun update(message: Message) {
            history[message.idLong] = cache[message.idLong]
            cache[message.idLong] = message
        }

        fun latest(id: Long): Message? {
            return cache[id]
        }

        fun history(id: Long): Message? {
            return history[id]
        }

        fun filter(predicate: (Message) -> Boolean): List<Message> {
            return cache.values.filter(predicate)
        }
    }

    @Listener
    override fun onReady(event: ReadyEvent) {
        launch {
            for (guild in event.jda.guilds) {
                for (channel in guild.textChannels.filter { it.hasLatestMessage() }) {
                    try {
                        channel.getHistory(300).forEach { m ->
                            add(m)
                            m.attachments.forEach {
                                tmpFile("attachments", m.guild.id, m.channel.id, "${m.id}_${it.id}_${it.fileName}") {
                                    if (!Files.exists(toPath())) {
                                        it.download(this)
                                    }
                                }
                            }
                        }
                    } catch (e: InsufficientPermissionException) {
                    }
                }
            }

            logger.info { "テキストチャンネルのキャッシュが完了しました." }
        }
    }

    @Listener(priority = Priority.Highest)
    override fun onMessageReceived(event: MessageReceivedEvent) {
        add(event.message)
    }

    @Listener(priority = Priority.Highest)
    override fun onMessageUpdate(event: MessageUpdateEvent) {
        update(event.message)
    }
}
