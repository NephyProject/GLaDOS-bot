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

package jp.nephy.glados.clients.twitter.config

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO
import io.ktor.util.KtorExperimentalAPI
import jp.nephy.glados.api.ConfigJson
import jp.nephy.glados.api.GLaDOS
import jp.nephy.glados.clients.logger.installHttpClientLogger
import jp.nephy.jsonkt.*
import jp.nephy.jsonkt.delegation.*
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.emulation.EmulationMode
import jp.nephy.penicillin.core.session.ApiClient
import jp.nephy.penicillin.core.session.config.*
import jp.nephy.penicillin.endpoints.account
import jp.nephy.penicillin.endpoints.account.verifyCredentials
import jp.nephy.penicillin.extensions.complete

data class TwitterAccount(override val json: JsonObject, val key: String): JsonModel {
    val ck by string
    val cs by string
    val at by string
    val ats by string

    val user by lazy {
        client.use {
            it.account.verifyCredentials.complete().use { response ->
                response.result
            }
        }
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }
    
    override fun equals(other: Any?): Boolean {
        return key == (other as? TwitterAccount)?.key
    }
}

val TwitterAccount.client: ApiClient
    get() = client()

val TwitterAccount.officialClient: ApiClient
    get() = client(EmulationMode.TwitterForiPhone)

@UseExperimental(KtorExperimentalAPI::class)
fun TwitterAccount.client(mode: EmulationMode = EmulationMode.None, engine: HttpClientEngineFactory<*> = CIO): ApiClient {
    return PenicillinClient {
        account {
            application(ck, cs)
            token(at, ats)
        }
        dispatcher {
            coroutineContext = GLaDOS.coroutineContext
        }
        httpClient(engine) {
            installHttpClientLogger()
        }
        api {
            emulationMode = mode
        }
    }
}

val ConfigJson.twitter: Map<String, TwitterAccount>
    get() = json["twitter"].jsonObject.map { it.key to it.value.jsonObject.parse<TwitterAccount>(it.key) }.toMap()

fun ConfigJson.twitterAccount(name: String): TwitterAccount {
    return twitter[name] ?: throw IllegalArgumentException("$name is not found in config.json.")
}
