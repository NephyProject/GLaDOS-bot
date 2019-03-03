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

package jp.nephy.glados.clients.discord.extensions.messages

import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import java.time.Instant
import java.time.temporal.TemporalAccessor

class EmbedBuilder {
    private val builder = EmbedBuilder()

    fun title(title: String, url: String? = null) = apply {
        builder.setTitle(title, url)
    }

    fun author(name: String, url: String? = null, iconUrl: String? = null) = apply {
        builder.setAuthor(name, url, iconUrl)
    }

    private val descriptionRaw = StringBuilder()
    fun asMention(target: IMentionable) = apply {
        descriptionRaw.insert(0, "${target.asMention}\n")
    }

    fun description(description: () -> Any?) = apply {
        descriptionRaw.append(description().toString())
    }

    fun descriptionBuilder(description: StringBuilder.() -> Unit) = apply {
        description(descriptionRaw)
    }

    fun image(url: String) = apply {
        builder.setImage(url)
    }

    fun thumbnail(url: String) = apply {
        builder.setThumbnail(url)
    }

    fun footer(text: String, iconUrl: String? = null) = apply {
        builder.setFooter(text, iconUrl)
    }

    fun timestamp(temporal: TemporalAccessor? = null) = apply {
        builder.setTimestamp(temporal ?: Instant.now())
    }

    fun color(color: HexColor) = apply {
        builder.setColor(color.rgb)
    }

    fun blankField(inline: Boolean = false) = apply {
        builder.addBlankField(inline)
    }

    fun field(name: String, inline: Boolean = false, value: () -> Any) = apply {
        builder.addField(name, value().toString(), inline)
    }

    fun build(): MessageEmbed {
        builder.setDescription(descriptionRaw.toString().trimEnd())
        return builder.build()
    }
}
