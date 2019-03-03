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

import jp.nephy.jsonkt.*
import jp.nephy.jsonkt.delegation.*
import kotlinx.serialization.json.*

fun SecretJson.boolean(key: String): Boolean? {
    return json.getOrNull(key)?.booleanOrNull
}

fun SecretJson.boolean(key: String, default: () -> Boolean): Boolean {
    return boolean(key) ?: default()
}

fun SecretJson.int(key: String): Int? {
    return json.getOrNull(key)?.intOrNull
}

fun SecretJson.int(key: String, default: () -> Int): Int {
    return int(key) ?: default()
}

fun SecretJson.long(key: String): Long? {
    return json.getOrNull(key)?.longOrNull
}

fun SecretJson.long(key: String, default: () -> Long): Long {
    return long(key) ?: default()
}

fun SecretJson.float(key: String): Float? {
    return json.getOrNull(key)?.floatOrNull
}

fun SecretJson.float(key: String, default: () -> Float): Float {
    return float(key) ?: default()
}

fun SecretJson.double(key: String): Double? {
    return json.getOrNull(key)?.doubleOrNull
}

fun SecretJson.double(key: String, default: () -> Double): Double {
    return double(key) ?: default()
}

fun SecretJson.string(key: String): String? {
    return json.getOrNull(key)?.stringOrNull
}

fun SecretJson.string(key: String, default: () -> String): String {
    return string(key) ?: default()
}

inline fun <reified T: JsonModel> SecretJson.model(key: String): T {
    return json[key].parse()
}

inline fun <reified T: JsonModel> SecretJson.modelOrNull(key: String): T? {
    return json.getObjectOrNull(key).parseOrNull()
}

inline fun <reified T: JsonModel> SecretJson.modelList(key: String): List<T> {
    return json[key].parseList()
}

inline fun <reified T: JsonModel> SecretJson.modelListOrNull(key: String): List<T>? {
    return json.getArrayOrNull(key).parseListOrNull()
}
