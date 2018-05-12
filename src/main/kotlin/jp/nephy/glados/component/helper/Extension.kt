package jp.nephy.glados.component.helper

import jp.nephy.glados.GLaDOS
import jp.nephy.utils.divMod
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.math.roundToLong


fun Long?.toDeltaMilliSecondString(): String? {
    if (this == null) {
        return null
    }

    return (Date().time - this).toMilliSecondString()
}

fun Long?.toMilliSecondString(): String? {
    if (this == null) {
        return null
    }

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

fun tmpFile(first: String, vararg more: String): File {
    val tmpDir = Paths.get(if (GLaDOS.instance.isDebugMode) "tmp_debug" else "tmp")
    if (! Files.exists(tmpDir)) {
        Files.createDirectory(tmpDir)
    }

    if (more.isNotEmpty()) {
        Files.createDirectories(Paths.get(tmpDir.toString(), first, *more.dropLast(1).toTypedArray()))
    }

    return Paths.get(tmpDir.toString(), first, *more).toFile()
}
