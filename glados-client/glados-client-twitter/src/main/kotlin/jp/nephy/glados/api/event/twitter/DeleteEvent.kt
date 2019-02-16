package jp.nephy.glados.api.event.twitter

import jp.nephy.glados.api.GLaDOS
import jp.nephy.penicillin.models.Stream

data class DeleteEvent(
    override val glados: GLaDOS,
    override val account: GLaDOSConfig.Accounts.TwitterAccount,
    val delete: Stream.Delete
): TwitterEvent
