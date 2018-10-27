package jp.nephy.glados.core.builder

import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import jp.nephy.glados.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.core.events.Event

inline fun <reified T: Event> EventWaiter.wait(noinline condition: T.() -> Boolean = { true }, timeout: Long? = null, noinline whenTimeout: () -> Unit = { }, noinline operation: T.() -> Unit) {
    var stop = false
    if (timeout != null) {
        GlobalScope.launch(dispatcher) {
            delay(timeout)
            stop = true
            whenTimeout()
        }
    }

    waitForEvent(T::class.java, {
        !stop && condition(it)
    }, {
                     if (stop) {
                         return@waitForEvent
                     }
                     operation(it)
                 })
}
