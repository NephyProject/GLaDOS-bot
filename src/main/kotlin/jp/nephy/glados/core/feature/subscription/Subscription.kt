package jp.nephy.glados.core.feature.subscription

import jp.nephy.glados.core.GLaDOSConfig
import jp.nephy.glados.core.feature.BotFeature
import java.lang.reflect.Method

interface Subscription<T: Annotation> {
    val annotation: T
    val instance: BotFeature
    val method: Method

    fun execute(vararg args: Any?) {
        method.invoke(instance, *args)
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
