package jp.nephy.glados.core.extensions.messages

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.IMentionable
import net.dv8tion.jda.core.entities.MessageEmbed
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
        builder.setDescription(descriptionRaw.toString())
        return builder.build()
    }
}
