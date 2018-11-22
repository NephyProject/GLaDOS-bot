package jp.nephy.glados.core.plugins.extensions.jda.messages

data class HexColor(private val hex: String) {
    companion object {
        val Good = HexColor("2dbe51")
        val Bad = HexColor("1e90ff")
        val Change = HexColor("10993b")
        val Neutral = HexColor("ffa500")
        val Plain = HexColor("d8d8d8")
    }

    val rgb: Int
        get() = hex.toInt(16)
}
