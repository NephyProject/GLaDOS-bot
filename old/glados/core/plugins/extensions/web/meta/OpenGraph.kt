package jp.nephy.glados.core.plugins.extensions.web.meta

import java.time.temporal.Temporal

// http://ogp.me
object OpenGraph {
    enum class Type(val tagName: String, val namespaceUri: String) {
        Website("website", "http://ogp.me/ns/website#"), Article("article", "http://ogp.me/ns/article#"), Book("book", "http://ogp.me/ns/book#"), Profile("profile", "http://ogp.me/ns/profile#"),

        MusicSong("music.song", "http://ogp.me/ns/music#"), MusicAlbum("music.album", "http://ogp.me/ns/music#"), MusicPlaylist("music.playlist", "http://ogp.me/ns/music#"), MusicRadioStation("music.radio_station", "http://ogp.me/ns/music#"),

        VideoMovie("video.movie", "http://ogp.me/ns/video#"), VideoEpisode("video.episode", "http://ogp.me/ns/video#"), VideoTVShow("video.tv_show", "http://ogp.me/ns/video#"), VideoOther("video.other", "http://ogp.me/ns/video#")
    }

    data class Locale(val main: String, val alternate: String?)
    data class Image(val url: String, val secureUrl: String? = null, val type: String? = null, val width: Int? = null, val height: Int? = null, val alt: String? = null)
    data class Video(val url: String, val secureUrl: String?, val type: String?, val width: Int?, val height: Int?)
    data class Audio(val url: String, val secureUrl: String?, val type: String?)

    class Extended {
        class Music {
            data class Song(val duration: Int, val album: List<Album>, val musician: List<Profile>, val disc: Int?, val track: Int?)
            data class Album(val song: Song, val musician: Profile, val releaseDate: Temporal, val disc: Int?, val track: Int?)
            data class Playlist(val song: Song, val creator: Profile)
        }

        class Video {
            data class Actor(val firstName: String, val lastName: String, val username: String, val gender: Gender, val role: String)
            data class Movie(val actor: List<Actor>, val director: List<Profile>, val writer: List<Profile>, val duration: Int, val releaseDate: Temporal, val tag: List<String>)
            data class Episode(val actor: List<Actor>, val director: List<Profile>, val writer: List<Profile>, val duration: Int, val releaseDate: Temporal, val tag: List<String>, val series: TVShow)
            data class TVShow(val actor: List<Actor>, val director: List<Profile>, val writer: List<Profile>, val duration: Int, val releaseDate: Temporal, val tag: List<String>)
            data class Other(val actor: List<Actor>, val director: List<Profile>, val writer: List<Profile>, val duration: Int, val releaseDate: Temporal, val tag: List<String>)
        }

        data class Article(val publishedTime: Temporal, val modifiedTime: Temporal, val expirationTime: Temporal, val author: List<Profile>, val section: String, val tag: List<String>)
        data class Book(val author: List<Profile>, val isbn: String, val releaseDate: Temporal, val tag: List<String>)
        data class Profile(val firstName: String, val lastName: String, val username: String, val gender: Gender)
    }

    enum class Gender(val content: String) {
        Male("male"), Female("female")
    }

    enum class Determiner(val content: String) {
        Default(""), A("a"), An("an"), The("the"), Auto("auto")
    }
}
