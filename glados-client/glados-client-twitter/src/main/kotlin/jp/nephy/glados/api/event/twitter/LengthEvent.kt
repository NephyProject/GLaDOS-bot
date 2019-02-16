package jp.nephy.glados.api.event.twitter

import jp.nephy.glados.api.plugin.twitter.TwitterEvent

data class LengthEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val length: Int): TwitterEvent.Event
