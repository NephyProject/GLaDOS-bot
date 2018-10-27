package jp.nephy.glados.features.internal

import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Loop
import jp.nephy.glados.jda
import net.dv8tion.jda.core.entities.Game
import java.util.*
import java.util.concurrent.TimeUnit

class UpdatePresence: BotFeature() {
    @Loop(30, TimeUnit.SECONDS)
    fun updateTestChamber() {
        val rnd = Random()
        val message = "Test Chamber ${rnd.nextInt(21) + 1}"

        jda.presence.game = when (rnd.nextInt(3)) {
            0 -> Game.watching(message)
            1 -> Game.listening(message)
            else -> Game.streaming(message, "https://www.twitch.tv/slashnephy")
        }
    }
}
