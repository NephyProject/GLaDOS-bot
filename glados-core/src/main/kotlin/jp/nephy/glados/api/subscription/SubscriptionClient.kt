package jp.nephy.glados.api.subscription

import kotlinx.coroutines.sync.Mutex

abstract class SubscriptionClient<A: Annotation, S: Subscription<A>> {
    private val subscriptionsMutex = Mutex()
    private val subscriptions = mutableListOf<S>()
}
