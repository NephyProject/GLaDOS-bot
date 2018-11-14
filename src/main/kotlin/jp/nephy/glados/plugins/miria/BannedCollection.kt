package jp.nephy.glados.plugins.miria

import jp.nephy.glados.core.extensions.eq
import jp.nephy.glados.mongodb
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollectionOfName

object BannedCollection {
    private val banWords = mongodb.getCollectionOfName<BanWord>("MiriaYannaiyoBanWord")
    fun checkWordRules(tweetText: String): BanWord? {
        return banWords.find().find { it.word in tweetText }
    }

    private val banUsers = mongodb.getCollectionOfName<BanUser>("MiriaYannaiyoBanUser")
    fun checkUserRules(screenName: String): BanUser? {
        return banUsers.findOne { BanUser::screen_name eq screenName }
    }

    private val banClients = mongodb.getCollectionOfName<BanClient>("MiriaYannaiyoBanClient")
    fun checkClientRules(name: String): BanClient? {
        return banClients.findOne { BanClient::name eq name }
    }

    data class BanWord(val word: String, val category: String)

    data class BanUser(val screen_name: String, val reason: String)

    data class BanClient(val name: String, val category: String)
}
