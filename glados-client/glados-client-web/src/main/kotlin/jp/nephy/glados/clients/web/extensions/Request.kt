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

package jp.nephy.glados.clients.web.extensions

import jp.nephy.glados.clients.web.event.WebAccessEvent
import jp.nephy.glados.clients.web.event.call
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/* Query Delegates */

fun WebAccessEvent.booleanQuery(key: String? = null, default: () -> Boolean) = object: ReadOnlyProperty<Any?, Boolean> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
        return this@booleanQuery.call.request.queryParameters[key ?: property.name]?.toBoolean() ?: default()
    }
}

fun WebAccessEvent.booleanQuery(key: String? = null) = object: ReadOnlyProperty<Any?, Boolean?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean? {
        return this@booleanQuery.call.request.queryParameters[key ?: property.name]?.toBoolean()
    }
}

fun WebAccessEvent.intQuery(key: String? = null, default: () -> Int) = object: ReadOnlyProperty<Any?, Int> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return this@intQuery.call.request.queryParameters[key ?: property.name]?.toIntOrNull() ?: default()
    }
}

fun WebAccessEvent.intQuery(key: String? = null) = object: ReadOnlyProperty<Any?, Int?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Int? {
        return this@intQuery.call.request.queryParameters[key ?: property.name]?.toIntOrNull()
    }
}

fun WebAccessEvent.longQuery(key: String? = null, default: () -> Long) = object: ReadOnlyProperty<Any?, Long> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Long {
        return this@longQuery.call.request.queryParameters[key ?: property.name]?.toLongOrNull() ?: default()
    }
}

fun WebAccessEvent.longQuery(key: String? = null) = object: ReadOnlyProperty<Any?, Long?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Long? {
        return this@longQuery.call.request.queryParameters[key ?: property.name]?.toLongOrNull()
    }
}

fun WebAccessEvent.floatQuery(key: String? = null, default: () -> Float) = object: ReadOnlyProperty<Any?, Float> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Float {
        return this@floatQuery.call.request.queryParameters[key ?: property.name]?.toFloatOrNull() ?: default()
    }
}

fun WebAccessEvent.floatQuery(key: String? = null) = object: ReadOnlyProperty<Any?, Float?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Float? {
        return this@floatQuery.call.request.queryParameters[key ?: property.name]?.toFloatOrNull()
    }
}

fun WebAccessEvent.doubleQuery(key: String? = null, default: () -> Double) = object: ReadOnlyProperty<Any?, Double> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Double {
        return this@doubleQuery.call.request.queryParameters[key ?: property.name]?.toDoubleOrNull() ?: default()
    }
}

fun WebAccessEvent.doubleQuery(key: String? = null) = object: ReadOnlyProperty<Any?, Double?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Double? {
        return this@doubleQuery.call.request.queryParameters[key ?: property.name]?.toDoubleOrNull()
    }
}

fun WebAccessEvent.query(key: String? = null, default: () -> String) = object: ReadOnlyProperty<Any?, String> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return this@query.call.request.queryParameters[key ?: property.name] ?: default()
    }
}

fun WebAccessEvent.query(key: String? = null) = object: ReadOnlyProperty<Any?, String?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): String? {
        return this@query.call.request.queryParameters[key ?: property.name]
    }
}

/* Fragment Delegates */

fun WebAccessEvent.booleanFragment(key: String? = null) = object: ReadOnlyProperty<Any?, Boolean?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean? {
        return fragments[key ?: property.name]?.toBoolean()
    }
}

fun WebAccessEvent.booleanFragment(key: String? = null, default: () -> Boolean) = object: ReadOnlyProperty<Any?, Boolean> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
        return fragments[key ?: property.name]?.toBoolean() ?: default()
    }
}

fun WebAccessEvent.intFragment(key: String? = null) = object: ReadOnlyProperty<Any?, Int?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Int? {
        return fragments[key ?: property.name]?.toIntOrNull()
    }
}

fun WebAccessEvent.intFragment(key: String? = null, default: () -> Int) = object: ReadOnlyProperty<Any?, Int> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return fragments[key ?: property.name]?.toIntOrNull() ?: default()
    }
}

fun WebAccessEvent.longFragment(key: String? = null) = object: ReadOnlyProperty<Any?, Long?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Long? {
        return fragments[key ?: property.name]?.toLongOrNull()
    }
}

fun WebAccessEvent.longFragment(key: String? = null, default: () -> Long) = object: ReadOnlyProperty<Any?, Long> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Long {
        return fragments[key ?: property.name]?.toLongOrNull() ?: default()
    }
}

fun WebAccessEvent.floatFragment(key: String? = null) = object: ReadOnlyProperty<Any?, Float?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Float? {
        return fragments[key ?: property.name]?.toFloatOrNull()
    }
}

fun WebAccessEvent.floatFragment(key: String? = null, default: () -> Float) = object: ReadOnlyProperty<Any?, Float> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Float {
        return fragments[key ?: property.name]?.toFloatOrNull() ?: default()
    }
}

fun WebAccessEvent.doubleFragment(key: String? = null) = object: ReadOnlyProperty<Any?, Double?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Double? {
        return fragments[key ?: property.name]?.toDoubleOrNull()
    }
}

fun WebAccessEvent.doubleFragment(key: String? = null, default: () -> Double) = object: ReadOnlyProperty<Any?, Double> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Double {
        return fragments[key ?: property.name]?.toDoubleOrNull() ?: default()
    }
}

fun WebAccessEvent.fragment(key: String? = null) = object: ReadOnlyProperty<Any?, String?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): String? {
        return fragments[key ?: property.name]
    }
}

fun WebAccessEvent.fragment(key: String? = null, default: () -> String) = object: ReadOnlyProperty<Any?, String> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return fragments[key ?: property.name] ?: default()
    }
}
