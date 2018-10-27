package jp.nephy.glados.features.internal

import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Event
import jp.nephy.glados.core.feature.subscription.Priority
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.MessageUpdateEvent
import java.util.concurrent.ConcurrentHashMap

class MessageCollector: BotFeature() {
    companion object {
        private val cache = ConcurrentHashMap<Long, Message>()
        private val history = ConcurrentHashMap<Long, Message?>()

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

    @Event(priority = Priority.Highest)
    override suspend fun onMessageReceived(event: MessageReceivedEvent) {
        add(event.message)
    }

    @Event(priority = Priority.Highest)
    override suspend fun onMessageUpdate(event: MessageUpdateEvent) {
        update(event.message)
    }
}
