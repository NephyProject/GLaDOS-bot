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

package jp.nephy.glados.clients.discord.config

import jp.nephy.glados.api.ConfigJson
import jp.nephy.jsonkt.*
import jp.nephy.jsonkt.delegation.*

data class Discord(override val json: JsonObject): JsonModel {
    val token by string
    val ownerId by nullableLong("owner_id")
    val prefix by string { "!" }

    val guilds by lambda { it.jsonObject.map { guild -> guild.value.jsonObject.parse<GuildConfig>() } }

    data class GuildConfig(override val json: JsonObject): JsonModel {
        val id by long
        val isMain by boolean("is_main") { false }

        val textChannels by jsonObject("text_channels") { jsonObjectOf() }
        val voiceChannels by jsonObject("voice_channels") { jsonObjectOf() }
        val roles by jsonObject { jsonObjectOf() }
        val emotes by jsonObject { jsonObjectOf() }
        val options by jsonObject { jsonObjectOf() }
    }
}

val ConfigJson.discord: Discord
    get() = json["discord"].parse()
