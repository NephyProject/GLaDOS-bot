package jp.nephy.glados.api.event.twitter

import jp.nephy.glados.api.GLaDOS
import jp.nephy.penicillin.models.DirectMessage

data class DirectMessageEvent(
    override val glados: GLaDOS,
    override val account: GLaDOSConfig.Accounts.TwitterAccount,
    val message: DirectMessage
): TwitterEvent
