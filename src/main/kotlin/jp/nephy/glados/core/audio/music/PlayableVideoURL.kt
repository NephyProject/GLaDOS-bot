package jp.nephy.glados.core.audio.music

enum class PlayableVideoURL(val friendlyName: String, vararg val regexes: Regex) {
    YouTube(
            "YouTube",
            "(?:http(?:s)?://)?(?:www\\.|m\\.)?youtube\\.com/watch\\?v=[\\w-]{11}".toRegex(),
            "(?:http(?:s)?://)?(?:www\\.)?youtu\\.be/[\\w-]{11}".toRegex(),
            "(?:http(?:s)?://)?(?:www\\.|m\\.)?youtube\\.com/playlist\\?list=(?:PL|LL|FL|UU)[\\w-]+".toRegex(),
            "[\\w-]{11}".toRegex()
    ),
    Niconico(
            "ニコニコ動画",
            "(?:http(?:s)?://)?(?:www\\.|sp\\.|m\\.)?nicovideo\\.jp/watch/sm\\d+".toRegex(),
            "(?:http(?:s)?://)?nico\\.ms/sm\\d+".toRegex(),
            "sm\\d+".toRegex()
    ),
    SoundCloud(
            "SoundCloud",
            "(?:http(?:s)?://)?(?:www\\.)?(?:m\\.)?soundcloud\\.com/(?:[\\w-]+)/(?:[\\w-]+)".toRegex(),
            "(?:http(?:s)?://)?(?:www\\.)?(?:m\\.)?soundcloud\\.com/(?:[\\w-]+)/sets/(?:[\\w-]+)".toRegex()
    ),
    Twitch(
            "Twitch",
            "(?:http(?:s)://)?(?:www\\.|go\\.)?twitch.tv/(?:[^/]+)".toRegex()
    );

    fun match(input: String): Boolean {
        return regexes.any { it.containsMatchIn(input) }
    }
}
