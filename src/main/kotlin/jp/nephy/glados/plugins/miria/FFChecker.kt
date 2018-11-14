package jp.nephy.glados.plugins.miria

import jp.nephy.glados.config
import jp.nephy.glados.core.GLaDOSConfig
import jp.nephy.glados.core.plugins.Plugin
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.PenicillinException
import jp.nephy.penicillin.core.TwitterErrorMessage
import jp.nephy.penicillin.core.allIds
import java.util.concurrent.TimeUnit

object FFChecker: Plugin() {
    private val account = config.twitterAccount("MiriaYannaiyo_Official")
    private val account2 = config.twitterAccount("Miria_Feedback")

    @Loop(1, TimeUnit.MINUTES)
    suspend fun checkShallowly() {
        account.checkShallowly()
        account2.checkShallowly()
    }

    @Loop(3, TimeUnit.MINUTES)
    suspend fun checkDeeply() {
        account.checkDeeply()
        account2.checkDeeply()
    }

    private suspend fun GLaDOSConfig.Accounts.TwitterAccount.checkShallowly() {
        officialClient.use { client ->
            val follows = client.friend.list(count = 200, skipStatus = true, includeUserEntities = false).await().result.users
            val followers = client.follower.list(count = 200, skipStatus = true, includeUserEntities = false).await().result.users
            val (shouldUnfollow, shouldFollow) = follows.filter { it.followedBy == false } to followers.filter { it.following == false && it.followRequestSent == false }

            if (shouldUnfollow.size < 100) {
                for (target in shouldUnfollow) {
                    client.wrapUnfollow(target.id, user.screenName)
                }

                for (target in shouldFollow) {
                    if (!client.wrapFollow(target.id, user.screenName)) {
                        break
                    }
                }
            } else {
                logger.error { "アカウントロックと考えられるため, リムーブ/フォロー処理を中止します. (@${user.screenName})" }
            }
        }
    }

    private suspend fun GLaDOSConfig.Accounts.TwitterAccount.checkDeeply() {
        officialClient.use { client ->
            val follows = client.friend.listIds(count = 5000).untilLast().allIds
            val followers = client.follower.listIds(count = 5000).untilLast().allIds
            val (shouldUnfollow, shouldFollow) = (follows - followers) to (followers - follows)

            if (shouldUnfollow.size < 100) {
                for (userId in shouldUnfollow) {
                    client.wrapUnfollow(userId, user.screenName)
                }

                for (userId in shouldFollow) {
                    if (!client.wrapFollow(userId, user.screenName)) {
                        break
                    }
                }
            } else {
                logger.error { "アカウントロックと考えられるため, リムーブ/フォロー処理を中止します. (@${user.screenName})" }
            }
        }
    }

    private suspend fun PenicillinClient.wrapFollow(id: Long, sn: String): Boolean {
        try {
            val user = friendship.create(userId = id).await()
            logger.info { "@${user.result.screenName} をフォローしました。(@$sn)" }
            return true
        } catch (e: PenicillinException) {
            return when (e.error) {
                TwitterErrorMessage.YouAreUnableToFollowMorePeopleAtThisTime -> {
                    false
                }
                TwitterErrorMessage.YouHaveAlreadyRequestedToFollowUser, TwitterErrorMessage.CannotFindSpecifiedUser, TwitterErrorMessage.UserNotFound, TwitterErrorMessage.SorryThatPageDoesNotExist -> {
                    true
                }
                else -> {
                    logger.error(e) { "UserID: $id のフォローに失敗しました。(@$sn)" }
                    true
                }
            }
        }
    }

    private suspend fun PenicillinClient.wrapUnfollow(id: Long, sn: String) {
        try {
            val user = friendship.destroy(userId = id).await()
            logger.info { "@${user.result.screenName} のフォローを解除しました。(@$sn)" }
        } catch (e: PenicillinException) {
            when (e.error) {
                TwitterErrorMessage.UserNotFound, TwitterErrorMessage.SorryThatPageDoesNotExist -> {
                }
                else -> {
                    logger.error(e) { "UserID: $id のフォロー解除に失敗しました。(@$sn)" }
                }
            }
        }
    }
}
