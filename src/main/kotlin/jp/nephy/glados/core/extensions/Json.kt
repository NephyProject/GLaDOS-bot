package jp.nephy.glados.core.extensions

import jp.nephy.jsonkt.*
import kotlinx.serialization.json.JsonObject

val EmptyJsonObject: JsonObject
    get() = jsonObjectOf()

fun JsonObject.edit(editor: MutableMap<String, Any?>.() -> Unit): JsonObject {
    return JsonEditor(toMutableMap()).apply(editor).toJsonObject()
}

class JsonEditor(initial: MutableMap<String, Any?>): MutableMap<String, Any?> by initial
