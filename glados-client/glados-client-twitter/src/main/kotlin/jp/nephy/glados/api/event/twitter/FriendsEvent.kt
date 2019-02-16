package jp.nephy.glados.api.event.twitter

import jp.nephy.glados.api.plugin.twitter.TwitterEvent

data class FriendsEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val friends: Stream.Friends): TwitterEvent.Event
