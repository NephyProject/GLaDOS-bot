package jp.nephy.glados.core.audio.player

enum class PlayerEmoji(val emoji: String) {
    Info("🔍"), TogglePlayState("⏯"),

    SkipBack("⏮"), SeekBack("⏪"), SeekForward("⏩"), SkipForward("⏭"),

    Shuffle("🔀"), RepeatTrack("🔂"), RepeatPlaylist("🔁"),

    Mute("🔇"), VolumeDown("🔉"), VolumeUp("🔊"),

    Clear("🗑"),

    SoundCloud("⛅"), NicoRanking("📺");

    companion object {
        fun fromEmoji(emoji: String): PlayerEmoji? {
            return values().find { it.emoji == emoji }
        }
    }
}
