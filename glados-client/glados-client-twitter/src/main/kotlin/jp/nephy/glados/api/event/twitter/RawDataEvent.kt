package jp.nephy.glados.api.event.twitter

import jp.nephy.glados.api.GLaDOS

data class RawDataEvent(
    override val glados: GLaDOS,
    override val account: GLaDOSConfig.Accounts.TwitterAccount,
    val data: String
): TwitterEvent
