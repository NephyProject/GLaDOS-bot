package jp.nephy.glados.api.event.twitter

import jp.nephy.glados.api.plugin.twitter.TwitterEvent

data class HeartbeatEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount): TwitterEvent.Event
