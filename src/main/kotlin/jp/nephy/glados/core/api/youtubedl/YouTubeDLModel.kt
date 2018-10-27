package jp.nephy.glados.core.api.youtubedl

import jp.nephy.jsonkt.ImmutableJsonObject
import jp.nephy.jsonkt.delegation.*
import kotlin.math.roundToLong

data class YouTubeDLModel(override val json: ImmutableJsonObject): JsonModel {
    val title by string
    val fulltitle by string
    val description by nullableString

    private val durationSec by double("duration")
    val durationMs: Long
        get() = durationSec.times(1000).roundToLong()

    val thumbnailUrl by nullableString("thumbnail")
    val height by int
    val width by int

    val webpageUrl by string
    val uploader by string
    val videoId by string("id")
    val formats by modelList<Format>()
    val ext by string

    data class Format(override val json: ImmutableJsonObject): JsonModel {
        val abr by int  // 50
        val acodec by nullableString  // "opus"
        val container by nullableString  // "m4a_dash"
        val downloaderOptions by model<DownloaderOptions?>(key = "downloader_options")  // {...}
        val ext by string  // "webm"
        val filesize by nullableInt  // 559420
        val format by string  // "249 - audio only (DASH audio)"
        val formatId by string("format_id")  // "249"
        val formatNote by nullableString("format_note")  // "DASH audio"
        val fps by nullableInt  // 24
        val height by int  // 144
        val httpHeaders by model<HttpHeaders>(key = "http_headers")  // {...}
        val playerUrl by nullableString("player_url")  // "/yts/jsbin/player-vflpGF_3J/en_US/base.js"
        val protocol by string  // "https"
        val resolution by nullableString  // "176x144"
        val tbr by float  // 51.512
        val url by string  // "https://r4---sn-oguesnze.googlevideo.com/videoplayback?fvip=2&dur=94.501&clen=559420&itag=249&c=WEB&mime=audio%2Fwebm&key=yt6&ms=au%2Crdu&mt=1520871953&mv=m&id=o-AGr4FtWah2SbehvINTEaYoIx_opii3foCnLwkEiw7DSl&signature=7AE8BC99930EDDE8E4B9CA753405D1F6D5ECB317.06C9F69AB4ED016B9733E44C73414EE2914BC801&gir=yes&pl=34&expire=1520893679&mm=31%2C29&ip=240b%3A11%3Ae780%3A2400%3Ac934%3A3db%3A5ec7%3Aa912&mn=sn-oguesnze%2Csn-ogueln7r&ei=j6qmWs6yA8TSqQHB9YGYAQ&keepalive=yes&source=youtube&initcwndbps=608750&requiressl=yes&gcr=jp&ipbits=0&sparams=clen%2Cdur%2Cei%2Cgcr%2Cgir%2Cid%2Cinitcwndbps%2Cip%2Cipbits%2Citag%2Ckeepalive%2Clmt%2Cmime%2Cmm%2Cmn%2Cms%2Cmv%2Cpl%2Crequiressl%2Csource%2Cexpire&lmt=1515911114125039&ratebypass=yes"
        val vbr by nullableFloat  // 300.0
        val vcodec by nullableString  // "none"
        val width by int  // 256

        class DownloaderOptions(override val json: ImmutableJsonObject): JsonModel {
            val httpChunkSize by int("http_chunk_size")  // 10485760
        }

        class HttpHeaders(override val json: ImmutableJsonObject): JsonModel {
            val accept by string("Accept")  // "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
            val acceptCharset by string("Accept-Charset")  // "ISO-8859-1,utf-8;q=0.7,*;q=0.7"
            val acceptEncoding by string("Accept-Encoding")  // "gzip, deflate"
            val acceptLanguage by string("Accept-Language")  // "en-us,en;q=0.5"
            val userAgent by string("User-Agent")  // "Mozilla/5.0 (X11; Linux x86_64; rv:59.0) Gecko/20100101 Firefox/59.0 (Chrome)"
        }
    }
}
