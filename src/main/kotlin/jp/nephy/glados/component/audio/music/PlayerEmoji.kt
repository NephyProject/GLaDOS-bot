package jp.nephy.glados.component.audio.music

enum class PlayerEmoji(val emoji: String) {
    Info("🔍"), TogglePlayState("⏯"),

    SkipBack("⏮"), SeekBack("⏪"), SeekForward("⏩"), SkipForward("⏭"),

    Shuffle("🔀"), RepeatTrack("🔂"), RepeatPlaylist("🔁"),

    Mute("🔇"), VolumeDown("🔉"), VolumeUp("🔊"),

    ToggleAutoPlaylist("💿"), Clear("🗑"),

    SoundCloud("⛅"), NicoRanking("📺");

    companion object {
        fun fromEmoji(emoji: String): PlayerEmoji? {
            return values().find { it.emoji == emoji }
        }
    }
}
