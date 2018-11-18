package jp.nephy.glados.core.audio.player

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import jp.nephy.glados.core.logger.SlackLogger
import jp.nephy.glados.core.extensions.isNoOneExceptSelf
import jp.nephy.glados.core.extensions.removeAtOrNull
import jp.nephy.glados.core.extensions.sumBy
import jp.nephy.glados.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

private val logger = SlackLogger("GLaDOS.Audio.TrackControls")

class TrackControls(private val guildPlayer: GuildPlayer, private val player: AudioPlayer): AudioEventAdapter() {
    private val userRequestQueue = CopyOnWriteArrayList<AudioTrack>()
    private val autoPlaylistQueue = CopyOnWriteArrayList<AudioTrack>()
    private val soundCloudQueue = CopyOnWriteArrayList<AudioTrack>()
    private val nicoRankingQueue = CopyOnWriteArrayList<AudioTrack>()

    operator fun plusAssign(track: AudioTrack) {
        add(track, justLoad = false)
    }

    private fun add(track: AudioTrack, justLoad: Boolean = false) {
        // ユーザリクエスト楽曲を優先して再生
        if (currentTrack?.data?.type != TrackType.UserRequest && track.data.type == TrackType.UserRequest) {
            return replace(track)
        }

        // 特殊プレイリストでグループIDが違うとき優先
        if (currentTrack?.data?.type != TrackType.UserRequest && track.data.type != TrackType.UserRequest && currentTrack?.data?.groupId != track.data.groupId) {
            if (currentTrack?.data?.type == track.data.type) {
                when (track.data.type) {
                    TrackType.SoundCloudCharts -> soundCloudQueue.clear()
                    TrackType.NicoNicoRanking -> nicoRankingQueue.clear()
                    else -> {
                    }
                }
            }

            return replace(track)
        }

        // VCに誰もいなければプレイヤーを停止しておく
        if (guildPlayer.currentVoiceChannel.isNoOneExceptSelf) {
            pause()
        }

        // ロードのみ または 既に再生中の曲があるならMusicPlayerに追加
        if (justLoad || !player.startTrack(track, true)) {
            add(track, null)
        }
    }

    fun addAll(tracks: List<AudioTrack>, justLoad: Boolean = false) {
        tracks.forEach {
            add(it, justLoad)
        }
    }

    private fun replace(newTrack: AudioTrack) {
        if (currentTrack != null) {
            add(currentTrack!!.makeCloneExactly(), 0)
        }

        player.playTrack(newTrack)
    }

    private fun AudioTrack.makeCloneExactly(): AudioTrack {
        return makeClone().also {
            it.position = position
            it.userData = data
        }
    }

    private fun skip() {
        val nextTrack = nextUserRequestTrack ?: return onQueueEmpty()

        player.playTrack(nextTrack)
    }

    private fun add(track: AudioTrack, index: Int? = null) {
        val queue = when (track.data.type) {
            TrackType.SoundCloudCharts -> soundCloudQueue
            TrackType.NicoNicoRanking -> nicoRankingQueue
            else -> userRequestQueue
        }
        if (index != null) {
            queue.add(index, track)
        } else {
            queue += track
        }
    }

    val currentTrack: AudioTrack?
        get() = player.playingTrack ?: null
    private val nextUserRequestTrack: AudioTrack?
        get() = userRequestQueue.removeAtOrNull(0).apply {
            if (isRepeatPlaylistEnabled && this != null) {
                userRequestQueue += makeCloneExactly()
            }
        }
    private val nextAutoPlaylistTrack: AudioTrack?
        get() = autoPlaylistQueue.removeAtOrNull(0).apply {
            if (this != null) {
                autoPlaylistQueue += makeCloneExactly()
            }
        }
    private val nextSoundCloudTrack: AudioTrack?
        get() = soundCloudQueue.removeAtOrNull(0).apply {
            if (isRepeatPlaylistEnabled && this != null) {
                soundCloudQueue += makeCloneExactly()
            }
        }
    private val nextNicoRankingTrack: AudioTrack?
        get() = nicoRankingQueue.removeAtOrNull(0).apply {
            if (isRepeatPlaylistEnabled && this != null) {
                nicoRankingQueue += makeCloneExactly()
            }
        }

    val queue: List<AudioTrack>
        get() = when (currentTrack?.data?.type) {
            TrackType.SoundCloudCharts -> soundCloudQueue
            TrackType.NicoNicoRanking -> nicoRankingQueue
            else -> userRequestQueue
        }
    val isEmptyQueue: Boolean
        get() = queue.isEmpty()

    val totalDuration: Long
        get() = (currentTrack?.remainingMillis ?: 0) + queue.sumBy { it.duration }

    val isPlaying: Boolean
        get() = !player.isPaused

    fun resume() {
        player.isPaused = false
        if (currentTrack == null) {
            skip()
        }
        logger.info { "MusicPlayer: リジューム" }
    }

    fun pause() {
        if (!player.isPaused) {
            player.isPaused = true
            logger.info { "MusicPlayer: ポーズ" }
        }
    }

    val position: Long
        get() = currentTrack?.position ?: 0

    fun skipBack() {
        currentTrack?.position = 0
        logger.info { "MusicPlayer: 後方スキップ" }
    }

    fun skipForward() {
        skip()
        logger.info { "MusicPlayer: 前方スキップ" }
    }

    fun seekBack(sec: Int) {
        if (currentTrack != null) {
            currentTrack!!.position -= sec * 1000L
        }
        logger.info { "MusicPlayer: 後方シーク (${sec}秒)" }
    }

    fun seekForward(sec: Int) {
        if (currentTrack != null) {
            currentTrack!!.position += sec * 1000L
        }
        logger.info { "MusicPlayer: 前方シーク (${sec}秒)" }
    }

    val volume: Int
        get() = player.volume
    val isMuted: Boolean
        get() = player.volume == 0
    private var previousVolume = 5
    fun mute() {
        previousVolume = volume
        player.volume = 0
        logger.info { "MusicPlayer: ミュート" }
    }

    fun unmute() {
        player.volume = previousVolume
        logger.info { "MusicPlayer: ミュート解除" }
    }

    fun volumeUp(amount: Int): Int {
        player.volume += amount
        return volume.apply {
            logger.info { "MusicPlayer: ボリューム -> $this" }
        }
    }

    fun volumeDown(amount: Int): Int {
        player.volume -= amount
        return volume.apply {
            logger.info { "MusicPlayer: ボリューム -> $this" }
        }
    }

    fun shuffle() {
        userRequestQueue.shuffle()
        autoPlaylistQueue.shuffle()
        soundCloudQueue.shuffle()
        logger.info { "MusicPlayer: シャッフル" }
    }

    private var trackRepeat: Boolean = false
    val isRepeatTrackEnabled: Boolean
        get() = trackRepeat

    fun enableRepeatTrack() {
        trackRepeat = true
        logger.info { "MusicPlayer: トラックリピート -> 有効化" }
    }

    fun disableRepeatTrack() {
        trackRepeat = false
        logger.info { "MusicPlayer: トラックリピート -> 無効化" }
    }

    private var playlistRepeat: Boolean = false
    val isRepeatPlaylistEnabled: Boolean
        get() = playlistRepeat

    fun enableRepeatPlaylist() {
        playlistRepeat = true
        logger.info { "MusicPlayer: プレイリストリピート -> 有効化" }
    }

    fun disableRepeatPlaylist() {
        playlistRepeat = false
        logger.info { "MusicPlayer: プレイリストリピート -> 無効化" }
    }

    fun clear() {
        userRequestQueue.clear()
        autoPlaylistQueue.clear()
        soundCloudQueue.clear()
        nicoRankingQueue.clear()
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if (endReason.mayStartNext) {
            if (isRepeatTrackEnabled) {
                player.playTrack(track.makeCloneExactly())
            } else {
                skip()
            }
        }

        GlobalScope.launch(dispatcher) {
            track.data.onTrackEnd(track)
        }
    }

    private fun playFromNicoRanking(): Boolean {
        if (nicoRankingQueue.isNotEmpty()) {
            val track = nextNicoRankingTrack
            if (track != null) {
                player.playTrack(track)
                return true
            }
        }
        return false
    }

    private fun playFromSoundCloud(): Boolean {
        if (soundCloudQueue.isNotEmpty()) {
            val track = nextSoundCloudTrack
            if (track != null) {
                player.playTrack(track)
                return true
            }
        }
        return false
    }

    private fun onQueueEmpty() {
        if (playFromNicoRanking() || playFromSoundCloud()) {
            return
        }

        player.stopTrack()
        logger.info { "MusicPlayerの再生キューが空になりました。" }
    }
}
