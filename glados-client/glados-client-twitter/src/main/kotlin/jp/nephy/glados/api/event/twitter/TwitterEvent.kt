package jp.nephy.glados.api.event.twitter

import jp.nephy.glados.api.event.Event

interface TwitterEvent: Event {
    val account: GLaDOSConfig.Accounts.TwitterAccount
}
