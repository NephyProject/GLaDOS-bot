@file:Suppress("FUNCTIONNAME")
package jp.nephy.glados.core.extensions

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import jp.nephy.glados.mongodb
import jp.nephy.jsonkt.collection
import jp.nephy.jsonkt.contains
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
class MongoConfigStore2<K, V>(name: String, private val autoInsert: Boolean = true, private val defaultValue: () -> V, private val serializer: (K) -> Long): ReadOnlyProperty<Any?, MongoConfigStore2<K, V>> {
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

@Suppress("UNCHECKED_CAST")
class MongoConfigStore3<K1, K2, V>(name: String, private val autoInsert: Boolean = true, private val defaultValue: () -> V, private val serializer1: (K1) -> Long, private val serializer2: (K2) -> Long): ReadOnlyProperty<Any?, MongoConfigStore3<K1, K2, V>> {
    private val collection = mongodb.collection(name)

    operator fun get(key1: K1, key2: K2): V {
        val (id1, id2) = serializer1(key1) to serializer2(key2)
        val default = defaultValue.invoke()
        val result = collection.findOne(Filters.and(Filters.eq("key1", id1), Filters.eq("key2", id2)))
        if (autoInsert && result == null) {
            collection.insertOne("key1" to id1, "key2" to id2, "value" to default)
        }

        return result?.get("value") as? V ?: default
    }

    operator fun set(key1: K1, key2: K2, value: V) {
        val (id1, id2) = serializer1(key1) to serializer2(key2)
        val default = defaultValue.invoke()
        if (!collection.contains(Filters.and(Filters.eq("key1", id1), Filters.eq("key2", id2)))) {
            collection.insertOne("key1" to id1, "key2" to id2, "value" to default)
        } else {
            collection.updateOne(Filters.and(Filters.eq("key1", id1), Filters.eq("key2", id2)), Updates.set("value", value))
        }
    }

    fun findAll(): Map<Pair<Long, Long>, V> {
        return collection.find().toList().map {
            (it["key1"] as Long to it["key2"] as Long)  to it["value"] as V
        }.toMap()
    }

    fun remove(key1: K1, key2: K2) {
        collection.deleteOne(Filters.and(Filters.eq("key1", serializer1(key1)), Filters.eq("key2", serializer2(key2))))
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>) = this
}

fun <T> MongoGuildConfigStore(name: String, autoInsert: Boolean = true, defaultValue: () -> T) = MongoConfigStore2<Guild, T>(name, autoInsert, defaultValue) { it.idLong }
fun <T> MongoUserConfigStore(name: String, autoInsert: Boolean = true, defaultValue: () -> T) = MongoConfigStore2<Member, T>(name, autoInsert, defaultValue) { it.user.idLong }
fun <T> MongoMemberConfigStore(name: String, autoInsert: Boolean = true, defaultValue: () -> T) = MongoConfigStore3<Guild, Member, T>(name, autoInsert, defaultValue, { it.idLong }, { it.user.idLong })
