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

package jp.nephy.glados.core

import jp.nephy.glados.api.AttributeMap
import jp.nephy.glados.api.Logger
import jp.nephy.glados.clients.logger.of
import java.util.concurrent.ConcurrentHashMap

internal object AttributeMapImpl: AttributeMap {
    private val logger = Logger.of("GLaDOS.AttributeMap")
    private val map = ConcurrentHashMap<String, Any>()
    
    @Suppress("UNCHECKED_CAST")
    override operator fun <T: Any> get(key: String): T {
        val attribute = map[key]

        return if (attribute != null) {
            attribute as? T ?: throw IllegalArgumentException("Attribute[\"$key\"] type is ${attribute::class.qualifiedName}.")
        } else {
            throw UninitializedPropertyAccessException("Attribute[\"$key\"] has not been initialized yet.")
        }
    }

    override operator fun set(key: String, block: () -> Any) {
        if (key in this) {
            throw IllegalStateException("Attribute[\"$key\"] is already initialized.")
        }
        
        map[key] = block()
        logger.debug { "Attribute: \"$key\" が作成されました。" }
    }

    override operator fun contains(key: String): Boolean {
        return map.containsKey(key)
    }

    override fun dispose(key: String) {
        map.remove(key)
    }
}
