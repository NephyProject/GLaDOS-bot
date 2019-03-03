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

package jp.nephy.glados.clients.discord.config

import jp.nephy.jsonkt.*
import jp.nephy.jsonkt.delegation.*
import kotlinx.serialization.json.*
import kotlinx.serialization.json.JsonElement

inline fun <T> Discord.GuildConfig?.option(key: String, operation: (JsonElement) -> T): T? {
    return this?.options?.getOrNull(key)?.let(operation)
}

fun Discord.GuildConfig?.booleanOption(key: String): Boolean? {
    return option(key) { it.booleanOrNull }
}

fun Discord.GuildConfig?.booleanOption(key: String, default: Boolean): Boolean {
    return booleanOption(key) ?: default
}

fun Discord.GuildConfig?.intOption(key: String): Int? {
    return option(key) { it.intOrNull }
}

fun Discord.GuildConfig?.intOption(key: String, default: Int): Int {
    return intOption(key) ?: default
}

fun Discord.GuildConfig?.longOption(key: String): Long? {
    return option(key) { it.longOrNull }
}

fun Discord.GuildConfig?.longOption(key: String, default: Long): Long {
    return longOption(key) ?: default
}

fun Discord.GuildConfig?.floatOption(key: String): Float? {
    return option(key) { it.floatOrNull }
}

fun Discord.GuildConfig?.floatOption(key: String, default: Float): Float {
    return floatOption(key) ?: default
}

fun Discord.GuildConfig?.doubleOption(key: String): Double? {
    return option(key) { it.doubleOrNull }
}

fun Discord.GuildConfig?.doubleOption(key: String, default: Double): Double {
    return doubleOption(key) ?: default
}

fun Discord.GuildConfig?.stringOption(key: String): String? {
    return option(key) { it.stringOrNull }
}

fun Discord.GuildConfig?.stringOption(key: String, default: String): String {
    return stringOption(key) ?: default
}

inline fun <reified T: JsonModel> Discord.GuildConfig?.modelOption(key: String): T? {
    return option(key) { it.jsonObject.parse<T>() }
}

inline fun <reified T: JsonModel> Discord.GuildConfig?.modelListOption(key: String): List<T>? {
    return option(key) { it.jsonArray.parseList<T>() }
}
