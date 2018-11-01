package jp.nephy.glados.core.extensions.messages

data class HexColor(private val hex: String) {
    companion object {
        val Good = HexColor("2dbe51")
        val Bad = HexColor("1e90ff")
        val Change = HexColor("10993b")
        val Neutral = HexColor("ffa500")
        val Plain = HexColor("d8d8d8")

        val YouTube = HexColor("ff0000")
        val SoundCloud = HexColor("ff5500")
        val Niconico = HexColor("dbdbdb")
        val Twitch = HexColor("6441a4")

        val Twitter = HexColor("1da1f2")
    }

    val rgb: Int
        get() = hex.toInt(16)
}
