package jp.nephy.glados.core.audio.player

import jp.nephy.glados.core.builder.Color

enum class PlayableURL(val friendlyName: String, val url: String, val faviconUrl: String, val color: Color, vararg val regexes: Regex) {
    YouTube(
            "YouTube",
            "https://www.youtube.com", "https://s.ytimg.com/yts/img/favicon_48-vflVjB_Qk.png", Color.YouTube,
            "(?:http(?:s)?://)?(?:www\\.|m\\.)?youtube\\.com/watch\\?v=[\\w-]{11}".toRegex(),
            "(?:http(?:s)?://)?(?:www\\.)?youtu\\.be/[\\w-]{11}".toRegex(),
            "(?:http(?:s)?://)?(?:www\\.|m\\.)?youtube\\.com/playlist\\?list=(?:PL|LL|FL|UU)[\\w-]+".toRegex(),
            "[\\w-]{11}".toRegex()
    ),
    Niconico(
            "ニコニコ動画",
            "http://www.nicovideo.jp", "https://nicovideo.cdn.nimg.jp/web/img/favicon.ico", Color.Niconico,
            "(?:http(?:s)?://)?(?:www\\.|sp\\.|m\\.)?nicovideo\\.jp/watch/sm\\d+".toRegex(),
            "(?:http(?:s)?://)?nico\\.ms/sm\\d+".toRegex(),
            "sm\\d+".toRegex()
    ),
    SoundCloud(
            "SoundCloud",
            "https://soundcloud.com", "https://a-v2.sndcdn.com/assets/images/sc-icons/favicon-2cadd14b.ico", Color.SoundCloud,
            "(?:http(?:s)?://)?(?:www\\.)?(?:m\\.)?soundcloud\\.com/(?:[\\w-]+)/(?:[\\w-]+)".toRegex(),
            "(?:http(?:s)?://)?(?:www\\.)?(?:m\\.)?soundcloud\\.com/(?:[\\w-]+)/sets/(?:[\\w-]+)".toRegex()
    ),
    Twitch(
            "Twitch",
            "https://www.twitch.tv", "https://static.twitchcdn.net/assets/favicon-75270f9df2b07174c23ce844a03d84af.ico", Color.Twitch,
            "(?:http(?:s)://)?(?:www\\.|go\\.)?twitch.tv/(?:[^/]+)".toRegex()
    );

    fun match(input: String): Boolean {
        return regexes.any { it.containsMatchIn(input) }
    }
}
