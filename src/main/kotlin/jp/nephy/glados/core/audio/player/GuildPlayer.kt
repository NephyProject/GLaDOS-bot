package jp.nephy.glados.core.audio.player

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.source.nico.NicoAudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import jp.nephy.glados.config
import jp.nephy.glados.core.GLaDOSConfig
import jp.nephy.glados.core.Logger
import jp.nephy.glados.core.api.niconico.NiconicoClient
import jp.nephy.glados.core.api.youtube.YouTubeClient
import jp.nephy.glados.core.audio.AudioReceiveHandlerImpl
import jp.nephy.glados.core.audio.AudioSendHandlerImpl
import jp.nephy.glados.core.audio.SilenceAudioSendHandler
import jp.nephy.glados.core.extensions.toMilliSecondString
import jp.nephy.glados.core.plugins.SubscriptionClient
import jp.nephy.glados.secret
import jp.nephy.utils.sumBy
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.VoiceChannel
import java.util.concurrent.ConcurrentHashMap

private val logger = Logger("GLaDOS.Audio.GuildPlayer")

private val players = ConcurrentHashMap<Guild, GuildPlayer>()
// TODO
val Guild.player: GuildPlayer?
    get() = synchronized(players) {
        players.getOrPut(this) {
            val guildConfig = config.forGuild(this) ?: return null
            val defaultVoiceChannel = guildConfig.voiceChannel("default") ?: return null

            GuildPlayer(this, guildConfig, defaultVoiceChannel).also {
                audioManager.isAutoReconnect = true
                audioManager.connectionListener = SubscriptionClient.ConnectionEvent.create(this)
                audioManager.sendingHandler = SilenceAudioSendHandler {
                    // Hotfix: https://github.com/DV8FromTheWorld/JDA/issues/789
                    audioManager.sendingHandler = AudioSendHandlerImpl(it.audioPlayer)
                    audioManager.setReceivingHandler(AudioReceiveHandlerImpl(it))
                    it.audioPlayer.addListener(SubscriptionClient.AudioEvent.create(it))
                    logger.info { "[${defaultVoiceChannel.name} ($name)] 無音の送信が終了しました。" }
                }
            }
        }
    }

class GuildPlayer(val guild: Guild, val guildConfig: GLaDOSConfig.GuildConfig, private val defaultVoiceChannel: VoiceChannel) {
    private val audioPlayerManager = DefaultAudioPlayerManager().also {
        val (niconicoEmail, niconicoPassword) = secret.forKey<String?>("niconico_email") to secret.forKey<String?>("niconico_email")
        if (niconicoEmail != null && niconicoPassword != null) {
            it.registerSourceManager(NicoAudioSourceManager(niconicoEmail, niconicoPassword))
        }

        AudioSourceManagers.registerRemoteSources(it)
        AudioSourceManagers.registerLocalSource(it)
    }

    val audioPlayer = audioPlayerManager.createPlayer().apply {
        volume = guildConfig.intOption("player_volume", 5)
    }!!

    val controls = TrackControls(this, audioPlayer)

    init {
        audioPlayer.addListener(controls)
    }

    suspend fun searchTrack(query: String, priority: SearchPriority, limit: Int = 20, handler: PlayerSearchResultHandler) {
        val googleApiKey = secret.forKey<String?>("google_api_key")
        if ((priority == SearchPriority.YouTube || priority == SearchPriority.Undefined) && googleApiKey != null) {
            val youtubeClient = YouTubeClient(googleApiKey)
            val youtubeResult = youtubeClient.search(query, limit)
            if (youtubeResult.isNotEmpty()) {
                return handler.onFoundYouTubeResult(youtubeResult)
            }
        }

        if (priority == SearchPriority.Niconico || priority == SearchPriority.Undefined) {
            val niconicoClient = NiconicoClient()
            val nicoResult = niconicoClient.search(query, limit = limit)
            if (nicoResult.data.isNotEmpty()) {
                return handler.onFoundNiconicoResult(nicoResult)
            }
        }

        handler.onEmptyResult()
    }

    fun loadTrack(identifier: String, trackType: TrackType, handler: PlayerLoadResultHandler) {
        audioPlayerManager.loadItemOrdered(this, identifier, object: AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                track.userData = TrackUserData(track, trackType)

                handler.onLoadTrack(track)
                logger.info { "[${track.sourceManager.javaClass.simpleName}] トラック \"${track.info.effectiveTitle}\" by ${track.info.author} (${track.duration.toMilliSecondString()}) をキューに追加しました。" }
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                if (playlist.tracks.isEmpty()) {
                    return noMatches()
                }

                for (track in playlist.tracks) {
                    track.userData = TrackUserData(track, trackType)
                }

                handler.onLoadPlaylist(playlist)
                logger.info {
                    buildString {
                        appendln("[${playlist.tracks.first().sourceManager.javaClass.simpleName}] プレイリスト \"${playlist.name}\" (${playlist.tracks.size}曲, ${playlist.tracks.sumBy { it.duration }.toMilliSecondString()}) をキューに追加しました。")
                        playlist.tracks.forEachIndexed { i, it ->
                            appendln("#${(i + 1).toString().padEnd(playlist.tracks.size.toString().length)}: ${it.info.effectiveTitle} (${it.duration.toMilliSecondString()})")
                        }
                    }
                }
            }

            override fun noMatches() {
                handler.onEmptyResult()
                logger.warn { "トラック \"$identifier\" は見つかりませんでした。" }
            }

            override fun loadFailed(exception: FriendlyException) {
                handler.onFailed(exception)
                logger.error { "トラック \"$identifier\" の読み込み中にエラーが発生しました。" }
            }
        })
    }

    fun joinVoiceChannel(channel: VoiceChannel) {
        if (guild.audioManager.isConnected || guild.audioManager.isAttemptingToConnect) {
            guild.audioManager.closeAudioConnection()
        }

        guild.audioManager.openAudioConnection(channel)

        logger.info { "サーバ ${guild.name} で ボイスチャンネル \"${channel.name}\" への接続を開始しました。" }
    }

    val currentVoiceChannel: VoiceChannel
        get() {
            if (!guild.audioManager.isConnected && !guild.audioManager.isAttemptingToConnect) {
                joinVoiceChannel(defaultVoiceChannel)
            }

            return guild.audioManager.connectedChannel ?: guild.audioManager.queuedAudioConnection
        }
}
