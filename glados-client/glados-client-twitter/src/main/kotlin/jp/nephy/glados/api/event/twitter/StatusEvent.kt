package jp.nephy.glados.api.event.twitter

import jp.nephy.glados.api.plugin.twitter.TwitterEvent

data class StatusEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val status: Status): TwitterEvent.Event
