package jp.nephy.glados.core.feature.subscription

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import jp.nephy.glados.core.GLaDOSConfig
import jp.nephy.glados.core.Logger
import jp.nephy.glados.core.audio.player.GuildPlayer
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.CommandError
import jp.nephy.glados.core.invocationException
import jp.nephy.glados.core.nullableGuild
import jp.nephy.glados.dispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.core.audio.SpeakingMode
import net.dv8tion.jda.core.audio.hooks.ConnectionListener
import net.dv8tion.jda.core.audio.hooks.ConnectionStatus
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KFunction
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure

data class EventSubscription(
        override val annotation: jp.nephy.glados.core.feature.subscription.Event,
        override val instance: BotFeature,
        override val function: KFunction<*>,
        override val targetGuilds: List<GLaDOSConfig.GuildConfig>
): GuildSpecificSubscription<jp.nephy.glados.core.feature.subscription.Event>

@Target(AnnotationTarget.FUNCTION)
annotation class Event(
        val guilds: Array<String> = [],
        val priority: Priority = Priority.Normal
)

class ListenerEventSubscriptionClient: SubscriptionClient<Event>, ListenerAdapter() {
    private val logger = Logger("GLaDOS.ListenerEvent")
    override val subscriptions = CopyOnWriteArrayList<GuildSpecificSubscription<Event>>()

    override fun onReady() {
        subscriptions.sortBy { it.annotation.priority }
    }

    override fun onGenericEvent(event: net.dv8tion.jda.core.events.Event) {
        val guild = event.nullableGuild

        subscriptions.filter {
            (guild == null || it.matches(guild)) && event::class.isSubclassOf(it.function.valueParameters.first().type.jvmErasure)
        }.forEach {
            GlobalScope.launch(dispatcher) {
                try {
                    it.invoke(event)
                    logger.trace { "${it.instance.javaClass.simpleName}#${it.function.name} が実行されました. (${guild?.name})" }
                } catch (e: CancellationException) {
                    return@launch
                } catch (e: Exception) {
                    when (val exception = e.invocationException) {
                        is CommandError -> {
                            logger.error(exception) { "コマンドエラーが発生しました。" }
                        }
                        else -> {
                            logger.error(exception) { "[${it.instance.javaClass.simpleName}#${it.function.name}] 実行中に例外が発生しました。" }
                        }
                    }
                }
            }
        }
    }
}

class AudioEventSubscriptionClient(val guildPlayer: GuildPlayer): SubscriptionClient<Event>, AudioEventAdapter() {
    private val logger = Logger("GLaDOS.AudioEvent")
    override val subscriptions = CopyOnWriteArrayList<GuildSpecificSubscription<Event>>()

    override fun onReady() {
        subscriptions.sortBy { it.annotation.priority }
    }

    private fun runEvent(name: String, vararg args: Any) {
        subscriptions.filter {
            it.function.name == name
                    && it.function.valueParameters.size == args.size
                    && args.filterIndexed { i, arg -> arg::class.isSubclassOf(it.function.valueParameters[i].type.jvmErasure) }.size == args.size
        }.forEach { it ->
            GlobalScope.launch(dispatcher) {
                try {
                    it.invoke(*args)
                    logger.trace { "${it.instance.javaClass.simpleName}#${it.function.name} が実行されました." }
                } catch (e: CancellationException) {
                    return@launch
                } catch (e: Exception) {
                    logger.error(e.invocationException) { "[${it.instance.javaClass.simpleName}#${it.function.name}] 実行中に例外が発生しました。" }
                }
            }
        }
    }

    override fun onPlayerPause(player: AudioPlayer) {
        runEvent("onPlayerPause", guildPlayer, player)
    }

    override fun onPlayerResume(player: AudioPlayer) {
        runEvent("onPlayerResume", guildPlayer, player)
    }

    override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
        runEvent("onTrackStart", guildPlayer, player, track)
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        runEvent("onTrackEnd", guildPlayer, player, track, endReason)
    }

    override fun onTrackException(player: AudioPlayer, track: AudioTrack, exception: FriendlyException) {
        runEvent("onTrackException", guildPlayer, player, track, exception)
    }

    override fun onTrackStuck(player: AudioPlayer, track: AudioTrack, thresholdMs: Long) {
        runEvent("onTrackStuck", guildPlayer, player, track, thresholdMs)
    }

    override fun onEvent(event: AudioEvent) {
        runEvent("onEvent", guildPlayer, event)
    }
}

class ConnectionListenerSubscriptionClient(val guild: Guild): SubscriptionClient<Event>, ConnectionListener {
    private val logger = Logger("GLaDOS.ConnectionListener")
    override val subscriptions = CopyOnWriteArrayList<GuildSpecificSubscription<Event>>()

    override fun onReady() {
        subscriptions.sortBy { it.annotation.priority }
    }

    private fun runEvent(name: String, vararg args: Any) {
        subscriptions.filter {
            it.function.name == name
                    && it.function.valueParameters.size == args.size
                    && args.filterIndexed { i, arg -> arg::class.isSubclassOf(it.function.valueParameters[i].type.jvmErasure) }.size == args.size
        }.forEach { it ->
            GlobalScope.launch(dispatcher) {
                try {
                    it.invoke(*args)
                    logger.trace { "${it.instance.javaClass.simpleName}#${it.function.name} が実行されました." }
                } catch (e: CancellationException) {
                    return@launch
                } catch (e: Exception) {
                    logger.error(e.invocationException) { "[${it.instance.javaClass.simpleName}#${it.function.name}] 実行中に例外が発生しました。" }
                }
            }
        }
    }

    override fun onPing(ping: Long) {
        runEvent("onPing", guild, ping)
    }

    override fun onStatusChange(status: ConnectionStatus) {
        runEvent("onStatusChange", guild, status)
    }

    override fun onUserSpeaking(user: User, speaking: Boolean) {
        runEvent("onUserSpeaking", guild, user, speaking)
    }

    override fun onUserSpeaking(user: User, modes: EnumSet<SpeakingMode>) {
        runEvent("onUserSpeaking", guild, user, modes)
    }

    override fun onUserSpeaking(user: User, speaking: Boolean, soundshare: Boolean) {
        runEvent("onUserSpeaking", guild, user, speaking, soundshare)
    }
}
