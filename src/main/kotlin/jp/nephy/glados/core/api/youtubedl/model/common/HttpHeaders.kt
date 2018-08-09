package jp.nephy.glados.core.api.youtubedl.model.common

import com.google.gson.JsonObject
import jp.nephy.jsonkt.JsonModel
import jp.nephy.jsonkt.byString


class HttpHeaders(override val json: JsonObject): JsonModel {
    val accept by json.byString("Accept")  // "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
    val acceptCharset by json.byString("Accept-Charset")  // "ISO-8859-1,utf-8;q=0.7,*;q=0.7"
    val acceptEncoding by json.byString("Accept-Encoding")  // "gzip, deflate"
    val acceptLanguage by json.byString("Accept-Language")  // "en-us,en;q=0.5"
    val userAgent by json.byString("User-Agent")  // "Mozilla/5.0 (X11; Linux x86_64; rv:59.0) Gecko/20100101 Firefox/59.0 (Chrome)"
}
