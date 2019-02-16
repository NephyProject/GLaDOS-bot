package jp.nephy.glados.api.event.twitter

import jp.nephy.glados.api.plugin.twitter.TwitterEvent

data class UnhandledJsonEvent(override val account: GLaDOSConfig.Accounts.TwitterAccount, val json: JsonObject): TwitterEvent.Event
