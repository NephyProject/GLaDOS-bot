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

package jp.nephy.glados.clients.discord

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import jp.nephy.glados.clients.discord.config.intOption
import jp.nephy.glados.clients.discord.extensions.config
import jp.nephy.glados.clients.discord.listener.audio.DiscordAudioEventSubscriptionClient
import jp.nephy.glados.clients.discord.listener.connection.DiscordConnectionEventSubscriptionClient
import jp.nephy.glados.clients.discord.listener.receive.DiscordReceiveAudioEventSubscriptionClient
import net.dv8tion.jda.api.audio.AudioSendHandler
import net.dv8tion.jda.api.entities.Guild
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

private val players = ConcurrentHashMap<Guild, GuildPlayer>()

val Guild.player: GuildPlayer
    get() = players.getOrPut(this) {
        val initialVolume = config.intOption("player_volume", 10)
        GuildPlayer(this, initialVolume)
    }

class GuildPlayer internal constructor(val guild: Guild, initialVolume: Int) {
    companion object {
        val audioPlayerManager: AudioPlayerManager = DefaultAudioPlayerManager()
    }

    val audioPlayer: AudioPlayer = audioPlayerManager.createPlayer()

    init {
        guild.audioManager.connectionListener = DiscordConnectionEventSubscriptionClient.Listener(guild)
        guild.audioManager.receivingHandler = DiscordReceiveAudioEventSubscriptionClient.Listener(this)
        guild.audioManager.sendingHandler = object: AudioSendHandler {
            private var frame: AudioFrame? = null

            override fun canProvide(): Boolean {
                frame = audioPlayer.provide()
                return frame != null
            }

            override fun provide20MsAudio() = ByteBuffer.wrap(frame!!.data)

            override fun isOpus() = true
        }

        audioPlayer.addListener(DiscordAudioEventSubscriptionClient.Listener(this))
        audioPlayer.volume = initialVolume
    }
}
