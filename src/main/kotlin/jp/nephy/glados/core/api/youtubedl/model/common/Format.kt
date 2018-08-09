package jp.nephy.glados.core.api.youtubedl.model.common

import com.google.gson.JsonObject
import jp.nephy.jsonkt.*


class Format(override val json: JsonObject): JsonModel {
    val abr by json.byInt  // 50
    val acodec by json.byNullableString  // "opus"
    val container by json.byNullableString  // "m4a_dash"
    val downloaderOptions by json.byModel<DownloaderOptions?>(key = "downloader_options")  // {...}
    val ext by json.byString  // "webm"
    val filesize by json.byNullableInt  // 559420
    val format by json.byString  // "249 - audio only (DASH audio)"
    val formatId by json.byString("format_id")  // "249"
    val formatNote by json.byNullableString("format_note")  // "DASH audio"
    val fps by json.byNullableInt  // 24
    val height by json.byInt  // 144
    val httpHeaders by json.byModel<HttpHeaders>(key = "http_headers")  // {...}
    val playerUrl by json.byNullableString("player_url")  // "/yts/jsbin/player-vflpGF_3J/en_US/base.js"
    val protocol by json.byString  // "https"
    val resolution by json.byNullableString  // "176x144"
    val tbr by json.byFloat  // 51.512
    val url by json.byString  // "https://r4---sn-oguesnze.googlevideo.com/videoplayback?fvip=2&dur=94.501&clen=559420&itag=249&c=WEB&mime=audio%2Fwebm&key=yt6&ms=au%2Crdu&mt=1520871953&mv=m&id=o-AGr4FtWah2SbehvINTEaYoIx_opii3foCnLwkEiw7DSl&signature=7AE8BC99930EDDE8E4B9CA753405D1F6D5ECB317.06C9F69AB4ED016B9733E44C73414EE2914BC801&gir=yes&pl=34&expire=1520893679&mm=31%2C29&ip=240b%3A11%3Ae780%3A2400%3Ac934%3A3db%3A5ec7%3Aa912&mn=sn-oguesnze%2Csn-ogueln7r&ei=j6qmWs6yA8TSqQHB9YGYAQ&keepalive=yes&source=youtube&initcwndbps=608750&requiressl=yes&gcr=jp&ipbits=0&sparams=clen%2Cdur%2Cei%2Cgcr%2Cgir%2Cid%2Cinitcwndbps%2Cip%2Cipbits%2Citag%2Ckeepalive%2Clmt%2Cmime%2Cmm%2Cmn%2Cms%2Cmv%2Cpl%2Crequiressl%2Csource%2Cexpire&lmt=1515911114125039&ratebypass=yes"
    val vbr by json.byNullableFloat  // 300.0
    val vcodec by json.byNullableString  // "none"
    val width by json.byInt  // 256
}
