package jp.nephy.glados.core.audio

import net.dv8tion.jda.core.audio.AudioSendHandler
import java.util.*

class SilenceAudioSendHandler(private val onStop: () -> Unit): AudioSendHandler {
    private val startTime = Date().time
    private var canProvide = true

    private val silenceData = byteArrayOf(0xF8.toByte(), 0xFF.toByte(), 0xFE.toByte())

    override fun canProvide() = canProvide
    override fun provide20MsAudio(): ByteArray {
        if ((Date().time - startTime) > 3000) {
            canProvide = false
        }

        return silenceData.apply {
            if (!canProvide) {
                onStop()
            }
        }
    }

    override fun isOpus() = true
}
