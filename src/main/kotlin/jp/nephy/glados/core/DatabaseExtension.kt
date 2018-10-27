@file:Suppress("FUNCTIONNAME")
package jp.nephy.glados.core

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import jp.nephy.glados.mongodb
import jp.nephy.jsonkt.collection
import jp.nephy.jsonkt.insertOne
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import org.bson.conversions.Bson
import org.litote.kmongo.findOne
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <T> MongoCollection<T>.contains(filter: Bson): Boolean {
    return countDocuments(filter) > 0L
}

@Suppress("UNCHECKED_CAST")
class MongoConfigStore<K, V>(name: String, private val autoInsert: Boolean = true, private val defaultValue: () -> V, private val serializer: (K) -> Long): ReadOnlyProperty<Any?, MongoConfigStore<K, V>> {
    private val collection = mongodb.collection(name)

    operator fun get(key: K): V {
        val id = serializer(key)
        val default = defaultValue.invoke()
        val result = collection.findOne(Filters.eq("key", id))
        if (autoInsert && result == null) {
            collection.insertOne("key" to id, "value" to default)
        }

        return result?.get("value") as? V ?: default
    }

    operator fun set(key: K, value: V) {
        val id = serializer(key)
        val default = defaultValue.invoke()
        if (!collection.contains(Filters.eq("key", id))) {
            collection.insertOne("key" to id, "value" to default)
        } else {
            collection.updateOne(Filters.eq("key", id), Updates.set("value", value))
        }
    }

    fun findAll(): Map<Long, V> {
        return collection.find().toList().map {
            it["key"] as Long to it["value"] as V
        }.toMap()
    }

    fun remove(key: K) {
        collection.deleteOne(Filters.eq("key", serializer(key)))
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>) = this
}

fun <T> MongoGuildConfigStore(name: String, autoInsert: Boolean = true, defaultValue: () -> T) = MongoConfigStore<Guild, T>(name, autoInsert, defaultValue) { it.idLong }
fun <T> MongoUserConfigStore(name: String, autoInsert: Boolean = true, defaultValue: () -> T) = MongoConfigStore<Member, T>(name, autoInsert, defaultValue) { it.user.idLong }
