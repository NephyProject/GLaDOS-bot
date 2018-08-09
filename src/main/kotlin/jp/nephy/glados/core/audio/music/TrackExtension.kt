package jp.nephy.glados.core.audio.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import jp.nephy.glados.core.api.niconico.model.SearchData
import jp.nephy.glados.core.api.niconico.model.Video
import jp.nephy.glados.core.api.soundcloud.model.Collection
import jp.nephy.glados.core.api.youtubedl.YouTubeDL
import jp.nephy.utils.sumBy


val AudioTrack.remaining: Long
    get() = duration - position
val List<AudioTrack>.totalDuration: Long
    get() = sumBy { it.duration }

val AudioTrackInfo.effectiveTitle: String
    get() = title ?: identifier

private val trackTypeMap = mutableMapOf<AudioTrack, TrackType>()
val AudioTrack.type: TrackType
    get() = trackTypeMap.getOrPut(this) { TrackType.UserRequest }
var AudioTrack.typeSetter: TrackType
    get() = throw IllegalStateException()
    set(value) = trackTypeMap.set(this, value)

private val trackGroupIdMap = mutableMapOf<AudioTrack, Long>()
val AudioTrack.groupId: Long?
    get() = trackGroupIdMap[this]
var AudioTrack.groupIdSetter: Long
    get() = throw IllegalStateException()
    set(value) = trackGroupIdMap.set(this, value)

private val youtubedlCacheMap = mutableMapOf<AudioTrack, YouTubeDL>()
val AudioTrack.youtubedl: YouTubeDL
    get() = youtubedlCacheMap.getOrPut(this) { YouTubeDL(this.info.uri) }
var AudioTrack.youtubedlSetter: YouTubeDL
    get() = throw IllegalStateException()
    set(value) = youtubedlCacheMap.set(this, value)

private val soundCloudCacheMap = mutableMapOf<AudioTrack, Collection>()
val AudioTrack.soundCloudCache: Collection?
    get() = soundCloudCacheMap[this]
var AudioTrack.soundCloudCacheSetter: Collection
    get() = throw IllegalStateException()
    set(value) = soundCloudCacheMap.set(this, value)

private val nicoCacheMap = mutableMapOf<AudioTrack, SearchData>()
val AudioTrack.nicoCache: SearchData?
    get() = nicoCacheMap[this]
var AudioTrack.nicoCacheSetter: SearchData
    get() = throw IllegalStateException()
    set(value) = nicoCacheMap.set(this, value)

private val nicoRankingCacheMap = mutableMapOf<AudioTrack, Video>()
val AudioTrack.nicoRankingCache: Video?
    get() = nicoRankingCacheMap[this]
var AudioTrack.nicoRankingCacheSetter: Video
    get() = throw IllegalStateException()
    set(value) = nicoRankingCacheMap.set(this, value)

private val youtubeCacheMap = mutableMapOf<AudioTrack, com.google.api.services.youtube.model.SearchResult>()
val AudioTrack.youtubeCache: com.google.api.services.youtube.model.SearchResult?
    get() = youtubeCacheMap[this]
var AudioTrack.youtubeCacheSetter: com.google.api.services.youtube.model.SearchResult
    get() = throw IllegalStateException()
    set(value) = youtubeCacheMap.set(this, value)
