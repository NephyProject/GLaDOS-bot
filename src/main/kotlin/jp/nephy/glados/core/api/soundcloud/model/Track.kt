package jp.nephy.glados.core.api.soundcloud.model

import com.google.gson.JsonObject
import jp.nephy.jsonkt.*


class Track(override val json: JsonObject): JsonModel {
    val artworkUrl by json.byNullableString("artwork_url")  // "https://i1.sndcdn.com/artworks-000306529515-38iu6a-large.jpg"
    val commentCount by json.byInt("comment_count")  // 2144
    val commentable by json.byBool  // true
    val createdAt by json.byString("created_at")  // "2018-02-22T20:26:48Z"
    val description by json.byString  // "Prod by: Flamm"
    val displayDate by json.byString("display_date")  // "2018-02-23T05:00:05Z"
    val downloadCount by json.byInt("download_count")  // 0
    val downloadUrl by json.byNullableJsonElement("download_url")  // null
    val downloadable by json.byBool  // false
    val duration by json.byLong  // 112807
    val embeddableBy by json.byString("embeddable_by")  // "all"
    val fullDuration by json.byInt("full_duration")  // 112807
    val genre by json.byString  // "Hip-hop & Rap"
    val hasDownloadsLeft by json.byBool("has_downloads_left")  // true
    val id by json.byInt  // 403657665
    val kind by json.byString  // "track"
    val labelName by json.byNullableString("label_name")  // "TenThousand Projects, LLC"
    val lastModified by json.byString("last_modified")  // "2018-03-13T06:52:33Z"
    val license by json.byString  // "all-rights-reserved"
    val likesCount by json.byInt("likes_count")  // 255282
    val monetizationModel by json.byString("monetization_model")  // "NOT_APPLICABLE"
    val permalink by json.byString  // "billy"
    val permalinkUrl by json.byString("permalink_url")  // "https://soundcloud.com/scumgang6ix9ine/billy"
    val playbackCount by json.byInt("playback_count")  // 12331003
    val policy by json.byString  // "ALLOW"
    val public by json.byBool  // true
    val publisherMetadata by json.byModel<PublisherMetadata>(key = "publisher_metadata")  // {...}
    val purchaseTitle by json.byNullableJsonElement("purchase_title")  // null
    val purchaseUrl by json.byNullableJsonElement("purchase_url")  // null
    val releaseDate by json.byNullableJsonElement("release_date")  // null
    val repostsCount by json.byInt("reposts_count")  // 9495
    val secretToken by json.byNullableJsonElement("secret_token")  // null
    val sharing by json.byString  // "public"
    val state by json.byString  // "finished"
    val streamable by json.byBool  // true
    val tagList by json.byString("tag_list")  // ""
    val title by json.byString  // "Billy"
    val uri by json.byString  // "https://api.soundcloud.com/tracks/403657665"
    val urn by json.byString  // "soundcloud:queue:403657665"
    val user by json.byModel<User>()  // {...}
    val userId by json.byInt("user_id")  // 307809061
    val visuals by json.byNullableJsonElement  // null
    val waveformUrl by json.byString("waveform_url")  // "https://wis.sndcdn.com/9BWTKm0MHBrj_m.json"
}
