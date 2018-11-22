@file:Suppress("UNUSED")

package jp.nephy.glados.core.plugins.extensions

import jp.nephy.glados.GLaDOS
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

inline fun tmpFile(first: String, vararg more: String, block: File.() -> Unit = {}): File {
    return tmpPath(first, *more).toFile().apply(block)
}

inline fun tmpPath(first: String, vararg more: String, block: Path.() -> Unit = {}): Path {
    if (!Files.exists(GLaDOS.tmpDir)) {
        Files.createDirectory(GLaDOS.tmpDir)
    }

    if (more.isNotEmpty()) {
        Files.createDirectories(Paths.get(GLaDOS.tmpDir.toString(), first, *more.dropLast(1).toTypedArray()))
    }

    return Paths.get(GLaDOS.tmpDir.toString(), first, *more).apply(block)
}

inline fun resourceFile(first: String, vararg more: String, block: File.() -> Unit = {}): File {
    return resourcePath(first, *more).toFile().apply(block)
}

inline fun resourcePath(first: String, vararg more: String, block: Path.() -> Unit = {}): Path {
    if (!Files.exists(GLaDOS.resourceDir)) {
        Files.createDirectory(GLaDOS.resourceDir)
    }

    if (more.isNotEmpty()) {
        Files.createDirectories(Paths.get(GLaDOS.resourceDir.toString(), first, *more.dropLast(1).toTypedArray()))
    }

    return Paths.get(GLaDOS.resourceDir.toString(), first, *more).apply(block)
}
