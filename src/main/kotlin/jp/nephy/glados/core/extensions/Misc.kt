package jp.nephy.glados.core.extensions

import jp.nephy.glados.isDebugMode
import jp.nephy.utils.divMod
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.math.roundToLong

fun tmpFile(first: String, vararg more: String, block: File.() -> Unit = {}): File {
    val tmpDir = Paths.get(if (isDebugMode) "tmp_debug" else "tmp")
    if (!Files.exists(tmpDir)) {
        Files.createDirectory(tmpDir)
    }

    if (more.isNotEmpty()) {
        Files.createDirectories(Paths.get(tmpDir.toString(), first, *more.dropLast(1).toTypedArray()))
    }

    return Paths.get(tmpDir.toString(), first, *more).toFile().apply(block)
}

fun Long.toDeltaMilliSecondString(): String {
    return (Date().time - this).toMilliSecondString()
}

inline fun String?.ifNullOrBlank(operation: () -> String): String {
    return this?.ifBlank(operation) ?: operation()
}

fun Long.toMilliSecondString(): String {
    val sec = div(1000.0).roundToLong()
    return buildString {
        val (q1, r1) = sec.divMod(60 * 60 * 24)
        if (q1 > 0) {
            append("${q1}日")
        }

        val (q2, r2) = r1.divMod(60 * 60)
        if (q2 > 0) {
            append("${q2}時間")
        }

        val (q3, r3) = r2.divMod(60)
        if (q3 > 0) {
            append("${q3}分")
        }
        append("${r3}秒")
    }
}

val Exception.invocationException: Throwable
    get() = if (this is InvocationTargetException) {
        targetException
    } else {
        this
    }
internal val spaceRegex = "\\s+".toRegex()
