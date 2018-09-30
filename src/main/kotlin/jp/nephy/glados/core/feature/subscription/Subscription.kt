package jp.nephy.glados.core.feature.subscription

import jp.nephy.glados.core.GLaDOSConfig
import jp.nephy.glados.core.feature.BotFeature
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.reflect.KFunction

interface Subscription<T: Annotation> {
    val annotation: T
    val instance: BotFeature
    val function: KFunction<*>

    suspend operator fun invoke(vararg args: Any?) {
        if (function.isSuspend) {
            suspendCoroutine<Any?> {
                try {
                    val result = function.call(instance, *args, it)
                    it.resume(result)
                } catch (e: Exception) {
                    it.resumeWithException(e)
                }
            }
        } else {
            function.call(instance, *args)
        }
    }
}

interface GuildSpecificSubscription<T: Annotation>: Subscription<T> {
    val targetGuilds: List<GLaDOSConfig.GuildConfig>
}

enum class Priority {
    Highest, Higher, High, Normal, Low, Lower, Lowest
}

interface SubscriptionClient<T: Annotation> {
    val subscriptions: List<Subscription<T>>

    fun onReady()
}
