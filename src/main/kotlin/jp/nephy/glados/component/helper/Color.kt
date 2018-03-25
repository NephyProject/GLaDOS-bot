package jp.nephy.glados.component.helper

enum class Color(private val hex: String) {
    Good("2dbe51"), Bad("1e90ff"), Neutral("ffa500"), Plain("d8d8d8"),
    YouTube("ff0000"), SoundCloud("ff5500"), Niconico("dbdbdb"), Twitch("6441a4"),
    Giveaway("f6546a");

    val rgb: Int
        get() = hex.toInt(16)
}
