package jp.nephy.glados.core.builder

data class Color(private val hex: String) {
    companion object {
        val Good = Color("2dbe51")
        val Bad = Color("1e90ff")
        val Change = Color("10993b")
        val Neutral = Color("ffa500")
        val Plain = Color("d8d8d8")

        val YouTube = Color("ff0000")
        val SoundCloud = Color("ff5500")
        val Niconico = Color("dbdbdb")
        val Twitch = Color("6441a4")

        val Twitter = Color("1da1f2")
    }

    val rgb: Int
        get() = hex.toInt(16)
}
