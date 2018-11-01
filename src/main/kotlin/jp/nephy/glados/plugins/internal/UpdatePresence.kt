package jp.nephy.glados.plugins.internal

import jp.nephy.glados.core.plugins.Plugin
import jp.nephy.glados.jda
import net.dv8tion.jda.core.entities.Game
import java.util.*

object UpdatePresence: Plugin() {
    @Schedule(multipleHours = [1], multipleMinutes = [1])
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
