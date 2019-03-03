/*
 * The MIT License (MIT)
 *
 *     Copyright (c) 2017-2019 Nephy Project Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

@file:Suppress("UNUSED")

package jp.nephy.glados.api

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

inline fun GLaDOS.Companion.tmpFile(first: String, vararg more: String, block: File.() -> Unit = {}): File {
    return tmpPath(first, *more).toFile().apply(block)
}

inline fun GLaDOS.Companion.tmpPath(first: String, vararg more: String, block: Path.() -> Unit = {}): Path {
    if (!Files.exists(GLaDOS.config.paths.tmp)) {
        Files.createDirectory(GLaDOS.config.paths.tmp)
    }

    if (more.isNotEmpty()) {
        val parentDirectory = Paths.get(GLaDOS.config.paths.tmp.toString(), first, *more.dropLast(1).toTypedArray())
        if (!Files.exists(parentDirectory)) {
            Files.createDirectories(parentDirectory)
        }
    }

    return Paths.get(GLaDOS.config.paths.tmp.toString(), first, *more).apply(block)
}

inline fun GLaDOS.Companion.resourceFile(first: String, vararg more: String, block: File.() -> Unit = {}): File {
    return resourcePath(first, *more).toFile().apply(block)
}

inline fun GLaDOS.Companion.resourcePath(first: String, vararg more: String, block: Path.() -> Unit = {}): Path {
    return Paths.get(GLaDOS.config.paths.resources.toString(), first, *more).apply(block)
}
