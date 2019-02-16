package jp.nephy.glados.api.event.twitter

import jp.nephy.glados.api.plugin.twitter.TwitterEvent

data class UnknownDataEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val data: String): TwitterEvent.Event
