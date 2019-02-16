package jp.nephy.glados.api.event.twitter

import jp.nephy.glados.api.GLaDOS
import jp.nephy.jsonkt.*

data class AnyJsonEvent(
    override val glados: GLaDOS,
    override val account: GLaDOSConfig.Accounts.TwitterAccount,
    val json: JsonObject
): TwitterEvent
