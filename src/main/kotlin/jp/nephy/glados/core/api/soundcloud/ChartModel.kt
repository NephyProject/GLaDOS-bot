package jp.nephy.glados.core.api.soundcloud

import jp.nephy.jsonkt.ImmutableJsonObject
import jp.nephy.jsonkt.delegation.*


data class ChartModel(override val json: ImmutableJsonObject): JsonModel {
    val collection by modelList<Collection>()  // [...]
    val genre by string  // "soundcloud:genres:all-music"
    val kind by string  // "top"
    val lastUpdated by string("last_updated")  // "2018-03-13T06:23:53Z"
    val nextHref by string("next_href")  // "https://api-v2.soundcloud.com/charts?genre=soundcloud%3Agenres%3Aall-music&query_urn=soundcloud%3Acharts%3A9e5b391ed3284f3c9016bf63c038bfb7&offset=20&high_tier_only=false&kind=top&limit=20"
    val queryUrn by string("query_urn")  // "soundcloud:charts:9e5b391ed3284f3c9016bf63c038bfb7"

    data class Collection(override val json: ImmutableJsonObject): JsonModel {
        val score by float  // 4175197.0
        val track by model<Track>()  // {...}

        data class Track(override val json: ImmutableJsonObject): JsonModel {
            val artworkUrl by nullableString("artwork_url")  // "https://i1.sndcdn.com/artworks-000306529515-38iu6a-large.jpg"
            val commentCount by int("comment_count")  // 2144
            val commentable by boolean  // true
            val createdAt by string("created_at")  // "2018-02-22T20:26:48Z"
            val description by string  // "Prod by: Flamm"
            val displayDate by string("display_date")  // "2018-02-23T05:00:05Z"
            val downloadCount by int("download_count")  // 0
            val downloadable by boolean  // false
            val duration by long  // 112807
            val embeddableBy by string("embeddable_by")  // "all"
            val fullDuration by int("full_duration")  // 112807
            val genre by string  // "Hip-hop & Rap"
            val hasDownloadsLeft by boolean("has_downloads_left")  // true
            val id by int  // 403657665
            val kind by string  // "track"
            val labelName by nullableString("label_name")  // "TenThousand Projects, LLC"
            val lastModified by string("last_modified")  // "2018-03-13T06:52:33Z"
            val license by string  // "all-rights-reserved"
            val likesCount by int("likes_count")  // 255282
            val monetizationModel by string("monetization_model")  // "NOT_APPLICABLE"
            val permalink by string  // "billy"
            val permalinkUrl by string("permalink_url")  // "https://soundcloud.com/scumgang6ix9ine/billy"
            val playbackCount by int("playback_count")  // 12331003
            val policy by string  // "ALLOW"
            val public by boolean  // true
            val publisherMetadata by model<PublisherMetadata>(key = "publisher_metadata")  // {...}
            val repostsCount by int("reposts_count")  // 9495
            val sharing by string  // "public"
            val state by string  // "finished"
            val streamable by boolean  // true
            val tagList by string("tag_list")  // ""
            val title by string  // "Billy"
            val uri by string  // "https://api.soundcloud.com/tracks/403657665"
            val urn by string  // "soundcloud:queue:403657665"
            val user by model<User>()  // {...}
            val userId by int("user_id")  // 307809061
            val waveformUrl by string("waveform_url")  // "https://wis.sndcdn.com/9BWTKm0MHBrj_m.json"

            data class User(override val json: ImmutableJsonObject): JsonModel {
                val avatarUrl by string("avatar_url")  // "https://i1.sndcdn.com/avatars-000338809424-572092-large.jpg"
                val firstName by string("first_name")  // ""
                val fullName by string("full_name")  // ""
                val id by int  // 307809061
                val kind by string  // "user"
                val lastModified by string("last_modified")  // "2018-02-23T20:52:14Z"
                val lastName by string("last_name")  // ""
                val permalink by string  // "scumgang6ix9ine"
                val permalinkUrl by string("permalink_url")  // "https://soundcloud.com/scumgang6ix9ine"
                val uri by string  // "https://api.soundcloud.com/users/307809061"
                val urn by string  // "soundcloud:users:307809061"
                val username by string  // "6IX9INE"
                val verified by boolean  // false
            }

            data class PublisherMetadata(override val json: ImmutableJsonObject): JsonModel {
                val albumTitle by string("album_title")  // "Day69"
                val artist by string  // "6ix9ine"
                val explicit by boolean  // true
                val id by int  // 403657665
                val isrc by string  // "QMEU31802751"
                val iswc by string  // ""
                val pLine by string("p_line")  // ""
                val pLineForDisplay by string("p_line_for_display")  // "℗ "
                val publisher by string  // "TenThousand Projects, LLC"
                val releaseTitle by string("release_title")  // ""
                val upcOrEan by string("upc_or_ean")  // ""
                val urn by string  // "soundcloud:queue:403657665"
                val writerComposer by string("writer_composer")  // ""
            }
        }
    }
}
