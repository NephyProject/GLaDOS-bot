package jp.nephy.glados.core.builder

enum class Color(private val hex: String) {
    Good("2dbe51"), Bad("1e90ff"), Change("10993b"), Neutral("ffa500"), Plain("d8d8d8"),
    YouTube("ff0000"), SoundCloud("ff5500"), Niconico("dbdbdb"), Twitch("6441a4"),
    Giveaway("f6546a"), Twitter("1da1f2");

    val rgb: Int
        get() = hex.toInt(16)
}
