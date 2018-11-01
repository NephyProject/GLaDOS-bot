package jp.nephy.glados.core.audio.speech

import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readBytes
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import jp.nephy.glados.httpClient
import jp.nephy.glados.secret
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.writeStringUtf8

class Maki {
    private val apiKey = secret.forKey<String>("docomo_api_key")

    suspend fun speak(text: () -> String) {
        val response = httpClient.post<HttpResponse>("https://api.apigw.smt.docomo.ne.jp/aiTalk/v1/textToSpeech?APIKEY=$apiKey") {
            accept(ContentType.parse("audio/L16"))
            body = object: OutgoingContent.WriteChannelContent() {
                override val contentType = ContentType.parse("application/ssml+xml")

                override suspend fun writeTo(channel: ByteWriteChannel) {
                    channel.writeStringUtf8("<?xml version=\"1.0\" encoding=\"utf-8\" ?><speak version=\"1.1\"><voice name=\"maki\">${text.invoke()}</voice></speak>")
                }
            }
        }
        val result = response.readBytes()
        result.size
        // TODO
    }
}
