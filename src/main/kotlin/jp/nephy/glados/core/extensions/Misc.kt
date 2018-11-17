@file:Suppress("NOTHING_TO_INLINE")

package jp.nephy.glados.core.extensions

import io.ktor.client.request.post
import io.ktor.client.response.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import jp.nephy.glados.httpClient
import jp.nephy.glados.isDebugMode
import jp.nephy.glados.secret
import jp.nephy.jsonkt.*
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.writeStringUtf8
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.Normalizer

fun resourcePath(vararg path: String): Path {
    return Paths.get("resources", *path)
}

fun String.toNFKCString(): String {
    return Normalizer.normalize(this, Normalizer.Form.NFKC).orEmpty()
}

inline fun tmpFile(first: String, vararg more: String, block: File.() -> Unit = {}): File {
    return tmpPath(first, *more).toFile().apply(block)
}

inline fun tmpPath(first: String, vararg more: String, block: Path.() -> Unit = {}): Path {
    val tmpDir = Paths.get(if (isDebugMode) "tmp_debug" else "tmp")
    if (!Files.exists(tmpDir)) {
        Files.createDirectory(tmpDir)
    }

    if (more.isNotEmpty()) {
        Files.createDirectories(Paths.get(tmpDir.toString(), first, *more.dropLast(1).toTypedArray()))
    }

    return Paths.get(tmpDir.toString(), first, *more).apply(block)
}

fun <T> Iterable<T>.joinToStringIndexed(separator: String, operation: (Int, T) -> String): String {
    return mapIndexed { index, t -> operation(index, t) }.joinToString(separator)
}

val Number.characterLength: Int
    get() = toString().length

fun Double.round(digit: Int = 0): String {
    if (digit < 0) {
        throw IllegalArgumentException()
    }

    return String.format("%.${digit}f", this)
}

inline fun String?.ifNullOrBlank(operation: () -> String): String {
    return this?.ifBlank(operation) ?: operation()
}

fun <T> Iterable<T>.sumBy(selector: (T) -> Long): Long {
    return map(selector).sum()
}

fun <T> MutableList<T>.removeAtOrNull(index: Int): T? {
    return try {
        removeAt(index)
    } catch (e: IndexOutOfBoundsException) {
        null
    }
}

fun Long.divMod(other: Long): Pair<Long, Long> {
    val q = div(other)
    val r = this - other * q
    return q to r
}

val Throwable.stackTraceString: String
    get() {
        StringWriter().use { sw ->
            PrintWriter(sw).use { pw ->
                printStackTrace(pw)
                pw.flush()
            }
            return sw.toString()
        }
    }

val Throwable.invocationException: Throwable
    get() = if (this is InvocationTargetException) {
        targetException
    } else {
        this
    }

val spaceRegex = "\\s+".toRegex()

suspend fun speak(text: () -> String) {
    httpClient.post<HttpResponse>(secret.forKey<String>("Speak_Url")) {
        body = object: OutgoingContent.WriteChannelContent() {
            override val contentType = ContentType.Application.Json

            override suspend fun writeTo(channel: ByteWriteChannel) {
                channel.writeStringUtf8(mapOf("text" to text()).toJsonString())
            }
        }
    }.close()
}

infix fun <A, B, C> Pair<A, B>.to(another: C): Triple<A, B, C> {
    return Triple(first, second, another)
}

infix fun <A, B, C> A.to(pair: Pair<B, C>): Triple<A, B, C> {
    return this to pair.first to pair.second
}

inline fun <T, R> T.transformEach(source: Iterable<R>, operation: T.(R) -> T): T {
    var result = this
    for (s in source) {
        result = operation(s)
    }
    return result
}
