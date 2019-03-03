/*
 * The MIT License (MIT)
 *
 *     Copyright (c) 2017-2019 Nephy Project Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package jp.nephy.glados.clients.web.extensions.meta

import java.util.*

data class PageMeta(
    val openGraphType: OpenGraph.Type?,
    val title: String,
    val description: String?,
    val author: String?,
    val email: String?,
    val siteName: String?,
    val locale: OpenGraph.Locale?,
    val determiner: OpenGraph.Determiner?,
    val favicon: String?,
    val image: List<OpenGraph.Image>,
    val audio: List<OpenGraph.Audio>,
    val video: List<OpenGraph.Video>,

    val song: OpenGraph.Extended.Music.Song?,
    val album: OpenGraph.Extended.Music.Album?,
    val playlist: OpenGraph.Extended.Music.Playlist?,
    val movie: OpenGraph.Extended.Video.Movie?,
    val episode: OpenGraph.Extended.Video.Episode?,
    val tvShow: OpenGraph.Extended.Video.TVShow?,
    val videoOther: OpenGraph.Extended.Video.Other?,
    val article: OpenGraph.Extended.Article?,
    val book: OpenGraph.Extended.Book?,
    val profile: OpenGraph.Extended.Profile?,

    val twitterCardType: TwitterCard.Type,
    val twitterCardSite: String?,
    val twitterCardCreator: String?
) {
    class Builder {
        operator fun invoke(operation: Builder.() -> Unit) {
            apply(operation)
        }

        private var openGraphType: OpenGraph.Type? = null
        fun openGraphType(value: OpenGraph.Type) = apply {
            openGraphType = value
        }

        private var title = ""
        fun title(value: String) = apply {
            title = value
        }

        private var description: String? = null
        fun description(value: String) = apply {
            description = value
        }

        fun descriptionBuilder(builder: StringBuilder.() -> Unit) = apply {
            val stringBuilder = StringBuilder()
            builder(stringBuilder)
            description = stringBuilder.toString()
        }

        private var author: String? = null
        fun author(value: String) = apply {
            author = value
        }

        private var email: String? = null
        fun email(value: String) = apply {
            email = value
        }

        private var siteName: String? = null
        fun siteName(value: String) = apply {
            siteName = value
        }

        private var locale = Locale.getDefault().toString()
        fun locale(value: String) = apply {
            locale = value
        }

        private var alternateLocale: String? = null
        fun alternateLocale(value: String) = apply {
            alternateLocale = value
        }

        private var determiner: OpenGraph.Determiner? = null
        fun determiner(value: OpenGraph.Determiner) = apply {
            determiner = value
        }

        private var favicon: String? = null
        fun favicon(value: String) = apply {
            favicon = value
        }

        private val images = mutableListOf<OpenGraph.Image>()
        fun image(value: OpenGraph.Image) = apply {
            images.add(value)
        }

        private val audios = mutableListOf<OpenGraph.Audio>()
        fun audio(value: OpenGraph.Audio) = apply {
            audios.add(value)
        }

        private val videos = mutableListOf<OpenGraph.Video>()
        fun video(value: OpenGraph.Video) = apply {
            videos.add(value)
        }

        private var song: OpenGraph.Extended.Music.Song? = null
        fun song(value: OpenGraph.Extended.Music.Song) = apply {
            song = value
        }

        private var album: OpenGraph.Extended.Music.Album? = null
        fun album(value: OpenGraph.Extended.Music.Album) = apply {
            album = value
        }

        private var playlist: OpenGraph.Extended.Music.Playlist? = null
        fun playlist(value: OpenGraph.Extended.Music.Playlist) = apply {
            playlist = value
        }

        private var movie: OpenGraph.Extended.Video.Movie? = null
        fun movie(value: OpenGraph.Extended.Video.Movie) = apply {
            movie = value
        }

        private var episode: OpenGraph.Extended.Video.Episode? = null
        fun episode(value: OpenGraph.Extended.Video.Episode) = apply {
            episode = value
        }

        private var tvShow: OpenGraph.Extended.Video.TVShow? = null
        fun tvShow(value: OpenGraph.Extended.Video.TVShow) = apply {
            tvShow = value
        }

        private var videoOther: OpenGraph.Extended.Video.Other? = null
        fun videoOther(value: OpenGraph.Extended.Video.Other) = apply {
            videoOther = value
        }

        private var article: OpenGraph.Extended.Article? = null
        fun article(value: OpenGraph.Extended.Article) = apply {
            article = value
        }

        private var book: OpenGraph.Extended.Book? = null
        fun book(value: OpenGraph.Extended.Book) = apply {
            book = value
        }

        private var profile: OpenGraph.Extended.Profile? = null
        fun profile(value: OpenGraph.Extended.Profile) = apply {
            profile = value
        }

        private var twitterCardType = TwitterCard.Type.Summary
        fun twitterCardType(value: TwitterCard.Type) = apply {
            twitterCardType = value
        }

        private var twitterCardSite: String? = null
        fun twitterCardSite(value: String) = apply {
            twitterCardSite = value
        }

        private var twitterCardCreator: String? = null
        fun twitterCardCreator(value: String) = apply {
            twitterCardCreator = value
        }

        fun build(): PageMeta {
            return PageMeta(
                openGraphType, title, description, author, email, siteName, OpenGraph.Locale(locale, alternateLocale), determiner, favicon, images, audios, videos, song, album, playlist, movie, episode, tvShow, videoOther, article, book, profile, twitterCardType, twitterCardSite, twitterCardCreator
            )
        }
    }
}
