package jp.nephy.glados.core.feature.subscription

import jp.nephy.glados.core.GLaDOSConfig
import jp.nephy.glados.core.feature.BotFeature
import net.dv8tion.jda.core.entities.Guild
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend

interface Subscription<T: Annotation> {
    val annotation: T
    val instance: BotFeature
    val function: KFunction<*>

    suspend operator fun invoke(vararg args: Any?) {
        if (function.isSuspend) {
            function.callSuspend(instance, *args)
        } else {
            function.call(instance, *args)
        }
    }
}

interface GuildSpecificSubscription<T: Annotation>: Subscription<T> {
    val targetGuilds: List<GLaDOSConfig.GuildConfig>

    fun matches(guild: Guild): Boolean {
        return targetGuilds.isEmpty() || targetGuilds.any { it.id == guild.idLong }
    }
}

enum class Priority {
    Highest, Higher, High, Normal, Low, Lower, Lowest
}

interface SubscriptionClient<T: Annotation> {
    val subscriptions: List<Subscription<T>>

    fun onReady()
}
