package jp.nephy.glados.core.extensions

import jp.nephy.glados.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import mu.KotlinLogging
import net.dv8tion.jda.core.requests.RestAction
import net.dv8tion.jda.core.requests.restaction.MessageAction
import java.util.concurrent.TimeUnit

suspend fun <T> RestAction<T>.await() = suspendCancellableCoroutine<T> {
    val result = runCatching {
        complete()
    }
    it.resumeWith(result)
}

fun <T> RestAction<T>.launch(timeout: Long = 0, unit: TimeUnit = TimeUnit.SECONDS, callback: suspend (T) -> Unit = {}) = GlobalScope.launch(dispatcher) {
    try {
        await().also {
            delay(unit.toMillis(timeout))
            callback(it)
        }
    } catch (e: Exception) {
        val logger = KotlinLogging.logger("GLaDOS.Coroutines")
        logger.error(e) { "非同期処理中に例外が発生しました。" }
    }
}

suspend fun MessageAction.awaitAndDelete(timeout: Long, unit: TimeUnit) {
    await().also {
        delay(unit.toMillis(timeout))
        it.delete().await()
    }
}

fun MessageAction.launchAndDelete(timeout: Long, unit: TimeUnit) = launch(timeout, unit) {
    it.delete().await()
}
