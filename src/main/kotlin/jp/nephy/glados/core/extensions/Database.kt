@file:Suppress("FUNCTIONNAME", "NOTHING_TO_INLINE")

package jp.nephy.glados.core.extensions

import com.mongodb.MongoClient
import com.mongodb.client.FindIterable
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.*
import com.mongodb.client.result.DeleteResult
import jp.nephy.glados.mongodb
import jp.nephy.jsonkt.*
import jp.nephy.jsonkt.delegation.*
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageReaction
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.json.JsonWriterSettings
import org.litote.kmongo.findOne
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

inline val JsonObject.bsonDocument: Document
    get() = Document.parse(toJsonString())

val pureJsonWriter: JsonWriterSettings = JsonWriterSettings.builder().int64Converter { value, writer ->
    writer.writeRaw(value.toString())
}.build()

inline fun MongoClient.database(name: String): MongoDatabase {
    return getDatabase(name)
}

inline fun MongoDatabase.collection(name: String): MongoCollection<Document> {
    return getCollection(name)
}

inline fun <T> MongoCollection<T>.contains(filter: Bson, options: CountOptions.() -> Unit = { }): Boolean {
    return countDocuments(filter, CountOptions().apply(options)) > 0L
}

inline fun MongoCollection<Document>.findOneAndParseJson(filter: Bson? = null): JsonObject? {
    return (if (filter != null) {
        findOne(filter)
    } else {
        findOne()
    })?.toJson(pureJsonWriter)?.toJsonObject()
}

inline fun <T> MongoCollection<T>.findAll(filter: Bson? = null): FindIterable<T> {
    return if (filter != null) {
        find(filter)
    } else {
        find()
    }
}

inline fun <reified T: JsonModel> MongoCollection<Document>.findOneAndParse(filter: Bson? = null): T? {
    return findOneAndParseJson(filter)?.parse()
}

inline fun MongoCollection<Document>.findAndParseJson(filter: Bson? = null, operation: FindIterable<Document>.() -> Unit = { }): List<JsonObject> {
    return if (filter != null) {
        find(filter)
    } else {
        find()
    }.apply(operation).toList().map { it.toJson(pureJsonWriter).toJsonObject() }
}

inline fun <reified T: JsonModel> MongoCollection<Document>.findAndParse(filter: Bson? = null, noinline operation: FindIterable<Document>.() -> Unit = { }): List<T> {
    return findAndParseJson(filter, operation).map { it.parse<T>() }
}

inline fun MongoCollection<Document>.insertModel(model: JsonModel, options: InsertOneOptions.() -> Unit = { }) {
    insertJson(model.json, options)
}

inline fun MongoCollection<Document>.insertJson(element: JsonObject, options: InsertOneOptions.() -> Unit = { }) {
    insertOne(element.bsonDocument, InsertOneOptions().apply(options))
}

inline fun MongoCollection<Document>.insertOne(vararg pairs: Pair<String, Any?>, options: InsertOneOptions.() -> Unit = { }) {
    insertOne(Document(pairs.toMap()), InsertOneOptions().apply(options))
}

inline fun MongoCollection<Document>.insertModels(models: Collection<JsonModel>, options: InsertManyOptions.() -> Unit = { }) {
    insertJsons(models.map { it.json }, options)
}

inline fun MongoCollection<Document>.insertJsons(elements: Collection<JsonObject>, options: InsertManyOptions.() -> Unit = { }) {
    insertMany(elements.map { it.bsonDocument }, InsertManyOptions().apply(options))
}

inline fun <T> MongoCollection<T>.deleteOne(filter: Bson, options: DeleteOptions.() -> Unit = { }): DeleteResult {
    return deleteOne(filter, DeleteOptions().apply(options))
}

inline fun <T> MongoCollection<T>.deleteMany(filter: Bson, options: DeleteOptions.() -> Unit = { }): DeleteResult {
    return deleteMany(filter, DeleteOptions().apply(options))
}

inline fun <T> MongoCollection<T>.deleteAll(options: DeleteOptions.() -> Unit = { }): DeleteResult {
    return deleteMany(org.bson.Document(), options)
}

@Suppress("UNCHECKED_CAST")
class MongoConfigStore2<K, ID, V>(name: String, private val autoInsert: Boolean = true, private val defaultValue: () -> V, private val serializer: (K) -> ID) {
    private val collection = mongodb.collection(name)

    operator fun contains(key: K): Boolean {
        val id = serializer(key)
        return containsById(id)
    }

    fun containsById(id: ID): Boolean {
        return collection.contains(Filters.eq("key", id))
    }

    operator fun get(key: K): V {
        val id = serializer(key)
        return getById(id)
    }

    fun getById(id: ID): V {
        val default = defaultValue.invoke()
        val result = collection.findOne(Filters.eq("key", id))
        if (autoInsert && result == null) {
            collection.insertOne("key" to id, "value" to default)
        }

        return result?.get("value") as? V ?: default
    }

    operator fun set(key: K, value: V) {
        val id = serializer(key)
        setById(id, value)
    }

    fun setById(id: ID, value: V) {
        if (!collection.contains(Filters.eq("key", id))) {
            collection.insertOne("key" to id, "value" to value)
        } else {
            collection.updateOne(Filters.eq("key", id), Updates.set("value", value))
        }
    }

    fun findAll(): Map<ID, V> {
        return collection.find().toList().map {
            it["key"] as ID to it["value"] as V
        }.toMap()
    }

    fun remove(key: K) {
        collection.deleteOne(Filters.eq("key", serializer(key)))
    }

    fun removeById(id: ID) {
        collection.deleteOne(Filters.eq("key", id))
    }

    fun removeAll(key: K) {
        collection.deleteMany(Filters.eq("key", serializer(key)))
    }

    fun removeAll() {
        collection.deleteAll()
    }

    fun removeAllById(id: Long) {
        collection.deleteMany(Filters.eq("key", id))
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = this
}

@Suppress("UNCHECKED_CAST")
class MongoConfigStore3<K1, ID1, K2, ID2, V>(name: String, private val autoInsert: Boolean = true, private val defaultValue: () -> V, private val serializer1: (K1) -> ID1, private val serializer2: (K2) -> ID2) {
    private val collection = mongodb.collection(name)

    operator fun contains(keys: Pair<K1, K2>): Boolean {
        val ids = serializer1(keys.first) to serializer2(keys.second)
        return containsById(ids)
    }

    fun containsById(ids: Pair<ID1, ID2>): Boolean {
        return collection.contains(Filters.and(Filters.eq("key1", ids.first), Filters.eq("key2", ids.second)))
    }

    operator fun get(key1: K1, key2: K2): V {
        val (id1, id2) = serializer1(key1) to serializer2(key2)
        return getById(id1, id2)
    }

    fun getById(id1: ID1, id2: ID2): V {
        val default = defaultValue.invoke()
        val result = collection.findOne(Filters.and(Filters.eq("key1", id1), Filters.eq("key2", id2)))
        if (autoInsert && result == null) {
            collection.insertOne("key1" to id1, "key2" to id2, "value" to default)
        }

        return result?.get("value") as? V ?: default
    }

    operator fun set(key1: K1, key2: K2, value: V) {
        val (id1, id2) = serializer1(key1) to serializer2(key2)
        setById(id1, id2, value)
    }

    fun setById(id1: ID1, id2: ID2, value: V) {
        if (!collection.contains(Filters.and(Filters.eq("key1", id1), Filters.eq("key2", id2)))) {
            collection.insertOne("key1" to id1, "key2" to id2, "value" to value)
        } else {
            collection.updateOne(Filters.and(Filters.eq("key1", id1), Filters.eq("key2", id2)), Updates.set("value", value))
        }
    }

    fun setAll(key1: K1, value: V) {
        val id1 = serializer1(key1)
        collection.updateMany(Filters.eq("key1", id1), Updates.set("value", value))
    }

    fun findAll(): List<Triple<ID1, ID2, V>> {
        return collection.find().toList().map {
            Triple(it["key1"] as ID1, it["key2"] as ID2, it["value"] as V)
        }
    }

    fun remove(key1: K1, key2: K2) {
        val (id1, id2) = serializer1(key1) to serializer2(key2)
        removeById(id1, id2)
    }

    fun removeById(id1: ID1, id2: ID2) {
        collection.deleteOne(Filters.and(Filters.eq("key1", id1), Filters.eq("key2", id2)))
    }

    fun removeAll(key1: K1, key2: K2) {
        val (id1, id2) = serializer1(key1) to serializer2(key2)
        removeAllById(id1, id2)
    }

    fun removeAllById(id1: ID1, id2: ID2) {
        collection.deleteMany(Filters.and(Filters.eq("key1", id1), Filters.eq("key2", id2)))
    }

    fun removeAll(key1: K1) {
        val id1 = serializer1(key1)
        removeAllById(id1)
    }

    fun removeAllById(id1: ID1) {
        collection.deleteMany(Filters.eq("key1", id1))
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = this
}

fun <T> MongoGuildConfigStore(name: String, autoInsert: Boolean = true, defaultValue: () -> T) = MongoConfigStore2<Guild, Long, T>(name, autoInsert, defaultValue) { it.idLong }
fun <T> MongoUserConfigStore(name: String, autoInsert: Boolean = true, defaultValue: () -> T) = MongoConfigStore2<Member, Long, T>(name, autoInsert, defaultValue) { it.user.idLong }
fun <T> MongoMemberConfigStore(name: String, autoInsert: Boolean = true, defaultValue: () -> T) = MongoConfigStore3<Guild, Long, Member, Long, T>(name, autoInsert, defaultValue, { it.idLong }, { it.user.idLong })
fun <T> MongoNullableMemberConfigStore(name: String, autoInsert: Boolean = true, defaultValue: () -> T? = { null }) = MongoConfigStore3<Guild, Long, Member, Long, T?>(name, autoInsert, defaultValue, { it.idLong }, { it.user.idLong })
fun <T> MongoMessageConfigStore(name: String, autoInsert: Boolean = true, defaultValue: () -> T) = MongoConfigStore2<Message, Long, T>(name, autoInsert, defaultValue) { it.idLong }
fun <T> MongoReactionConfigStore(name: String, autoInsert: Boolean = true, defaultValue: () -> T) = MongoConfigStore3<Message, Long, MessageReaction, String, T>(name, autoInsert, defaultValue, { it.idLong }, { it.reactionEmote.name })

inline fun <T, R> MongoCollection<T>.transaction(operation: MongoCollection<T>.() -> R): R {
    return run(operation)
}

fun <T> MongoCollection<T>.contains(filter: () -> Bson): Boolean {
    return countDocuments(filter.invoke()) > 0L
}

infix fun Bson.and(other: Bson): Bson {
    return Filters.and(this, other)
}

infix fun Bson.or(other: Bson): Bson {
    return Filters.or(this, other)
}

infix fun String.eq(value: Any?): Bson {
    return Filters.eq(this, value)
}

infix fun <T, R> KProperty1<T, R>.eq(value: R): Bson {
    return Filters.eq(name, value)
}

infix fun String.inc(value: Int): Bson {
    return Updates.inc(this, value)
}

infix fun String.set(value: Any?): Bson {
    return Updates.set(this, value)
}

data class Selection<T>(val collection: MongoCollection<T>, val filter: Bson)

inline fun <T> MongoCollection<T>.select(filter: () -> Bson): Selection<T> {
    return Selection(this, filter.invoke())
}

inline fun <T> Selection<T>.isEmpty(): Boolean {
    return !collection.contains(filter)
}

inline fun Selection<Document>.ifEmpty(vararg elements: Pair<String, Any?>): Selection<Document> {
    return apply {
        if (isEmpty()) {
            collection.insertOne(*elements)
        }
    }
}

inline fun <T> Selection<T>.ifEmpty(data: () -> T): Selection<T> {
    return apply {
        if (isEmpty()) {
            collection.insertOne(data.invoke())
        }
    }
}

inline fun <reified M: JsonModel> Selection<Document>.updateOne(update: () -> Bson): M {
    collection.updateOne(filter, update.invoke())
    return collection.findOneAndParse(filter)!!
}

inline fun <T> Selection<T>.updateOne(update: () -> Bson): T {
    collection.updateOne(filter, update.invoke())
    return collection.findOne(filter)!!
}

@Suppress("UNCHECKED_CAST")
object MongoDynamicConfigStore {
    private val collection = mongodb.collection("GLaDOSDynamicConfigStore")

    private operator fun contains(key: Any): Boolean {
        return collection.contains("key" eq key)
    }

    private operator fun get(key: Any): Any? {
        val result = collection.findOne("key" eq key)

        return result?.get("value")
    }

    private operator fun set(key: Any, value: Any?) {
        if (!contains(key)) {
            collection.insertOne("key" to key, "value" to value)
        } else {
            collection.updateOne("key" eq key, "value" set value)
        }
    }

    private fun <T> property(key: Any?, default: () -> T): ReadWriteProperty<Any?, T> = object: ReadWriteProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return get(key ?: property.name) as? T ?: default()
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            set(key ?: property.name, value)
        }
    }

    fun Boolean(key: Any? = null, default: () -> Boolean) = property(key, default)
    fun Boolean(key: Any? = null) = property(key) { null as Boolean? }
    val Boolean
        get() = Boolean()

    fun Int(key: Any? = null, default: () -> Int) = property(key, default)
    fun Int(key: Any? = null) = property(key) { null as Int? }
    val Int
        get() = Int()

    fun Long(key: Any? = null, default: () -> Long) = property(key, default)
    fun Long(key: Any? = null) = property(key) { null as Long? }
    val Long
        get() = Long()

    fun Float(key: Any? = null, default: () -> Float) = property(key, default)
    fun Float(key: Any? = null) = property(key) { null as Float? }
    val Float
        get() = Float()

    fun Double(key: Any? = null, default: () -> Double) = property(key, default)
    fun Double(key: Any? = null) = property(key) { null as Double? }
    val Double
        get() = Double()

    fun String(key: Any? = null, default: () -> String) = property(key, default)
    fun String(key: Any? = null) = property(key) { null as String? }
    val String
        get() = String()
}

fun <K, V> MongoConfigMap(key: String, autoInsert: Boolean = true, defaultValue: () -> V) = MongoConfigStore2<K, K, V>(key, autoInsert, defaultValue, { it })
