package jp.nephy.glados.api.event.twitter

import jp.nephy.glados.api.GLaDOS

data class ConnectEvent(
    override val glados: GLaDOS,
    override val account: GLaDOSConfig.Accounts.TwitterAccount
): TwitterEvent
