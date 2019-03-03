@file:Suppress("UNUSED")

package jp.nephy.glados.core.plugins.extensions.web

import jp.nephy.glados.core.plugins.Plugin
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/* Query Delegates */

fun Plugin.Web.AccessEvent.booleanQuery(key: String? = null, default: () -> Boolean) = object: ReadOnlyProperty<Any?, Boolean> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
        return this@booleanQuery.call.request.queryParameters[key ?: property.name]?.toBoolean() ?: default()
    }
}

fun Plugin.Web.AccessEvent.booleanQuery(key: String? = null) = object: ReadOnlyProperty<Any?, Boolean?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean? {
        return this@booleanQuery.call.request.queryParameters[key ?: property.name]?.toBoolean()
    }
}

fun Plugin.Web.AccessEvent.intQuery(key: String? = null, default: () -> Int) = object: ReadOnlyProperty<Any?, Int> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return this@intQuery.call.request.queryParameters[key ?: property.name]?.toIntOrNull() ?: default()
    }
}

fun Plugin.Web.AccessEvent.intQuery(key: String? = null) = object: ReadOnlyProperty<Any?, Int?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Int? {
        return this@intQuery.call.request.queryParameters[key ?: property.name]?.toIntOrNull()
    }
}

fun Plugin.Web.AccessEvent.longQuery(key: String? = null, default: () -> Long) = object: ReadOnlyProperty<Any?, Long> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Long {
        return this@longQuery.call.request.queryParameters[key ?: property.name]?.toLongOrNull() ?: default()
    }
}

fun Plugin.Web.AccessEvent.longQuery(key: String? = null) = object: ReadOnlyProperty<Any?, Long?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Long? {
        return this@longQuery.call.request.queryParameters[key ?: property.name]?.toLongOrNull()
    }
}

fun Plugin.Web.AccessEvent.floatQuery(key: String? = null, default: () -> Float) = object: ReadOnlyProperty<Any?, Float> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Float {
        return this@floatQuery.call.request.queryParameters[key ?: property.name]?.toFloatOrNull() ?: default()
    }
}

fun Plugin.Web.AccessEvent.floatQuery(key: String? = null) = object: ReadOnlyProperty<Any?, Float?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Float? {
        return this@floatQuery.call.request.queryParameters[key ?: property.name]?.toFloatOrNull()
    }
}

fun Plugin.Web.AccessEvent.doubleQuery(key: String? = null, default: () -> Double) = object: ReadOnlyProperty<Any?, Double> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Double {
        return this@doubleQuery.call.request.queryParameters[key ?: property.name]?.toDoubleOrNull() ?: default()
    }
}

fun Plugin.Web.AccessEvent.doubleQuery(key: String? = null) = object: ReadOnlyProperty<Any?, Double?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Double? {
        return this@doubleQuery.call.request.queryParameters[key ?: property.name]?.toDoubleOrNull()
    }
}

fun Plugin.Web.AccessEvent.query(key: String? = null, default: () -> String) = object: ReadOnlyProperty<Any?, String> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return this@query.call.request.queryParameters[key ?: property.name] ?: default()
    }
}

fun Plugin.Web.AccessEvent.query(key: String? = null) = object: ReadOnlyProperty<Any?, String?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): String? {
        return this@query.call.request.queryParameters[key ?: property.name]
    }
}

/* Fragment Delegates */

fun Plugin.Web.AccessEvent.booleanFragment(key: String? = null) = object: ReadOnlyProperty<Any?, Boolean?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean? {
        return fragments[key ?: property.name]?.toBoolean()
    }
}

fun Plugin.Web.AccessEvent.booleanFragment(key: String? = null, default: () -> Boolean) = object: ReadOnlyProperty<Any?, Boolean> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
        return fragments[key ?: property.name]?.toBoolean() ?: default()
    }
}

fun Plugin.Web.AccessEvent.intFragment(key: String? = null) = object: ReadOnlyProperty<Any?, Int?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Int? {
        return fragments[key ?: property.name]?.toIntOrNull()
    }
}

fun Plugin.Web.AccessEvent.intFragment(key: String? = null, default: () -> Int) = object: ReadOnlyProperty<Any?, Int> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return fragments[key ?: property.name]?.toIntOrNull() ?: default()
    }
}

fun Plugin.Web.AccessEvent.longFragment(key: String? = null) = object: ReadOnlyProperty<Any?, Long?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Long? {
        return fragments[key ?: property.name]?.toLongOrNull()
    }
}

fun Plugin.Web.AccessEvent.longFragment(key: String? = null, default: () -> Long) = object: ReadOnlyProperty<Any?, Long> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Long {
        return fragments[key ?: property.name]?.toLongOrNull() ?: default()
    }
}

fun Plugin.Web.AccessEvent.floatFragment(key: String? = null) = object: ReadOnlyProperty<Any?, Float?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Float? {
        return fragments[key ?: property.name]?.toFloatOrNull()
    }
}

fun Plugin.Web.AccessEvent.floatFragment(key: String? = null, default: () -> Float) = object: ReadOnlyProperty<Any?, Float> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Float {
        return fragments[key ?: property.name]?.toFloatOrNull() ?: default()
    }
}

fun Plugin.Web.AccessEvent.doubleFragment(key: String? = null) = object: ReadOnlyProperty<Any?, Double?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Double? {
        return fragments[key ?: property.name]?.toDoubleOrNull()
    }
}

fun Plugin.Web.AccessEvent.doubleFragment(key: String? = null, default: () -> Double) = object: ReadOnlyProperty<Any?, Double> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Double {
        return fragments[key ?: property.name]?.toDoubleOrNull() ?: default()
    }
}

fun Plugin.Web.AccessEvent.fragment(key: String? = null) = object: ReadOnlyProperty<Any?, String?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): String? {
        return fragments[key ?: property.name]
    }
}

fun Plugin.Web.AccessEvent.fragment(key: String? = null, default: () -> String) = object: ReadOnlyProperty<Any?, String> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return fragments[key ?: property.name] ?: default()
    }
}