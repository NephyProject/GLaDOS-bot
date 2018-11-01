package jp.nephy.glados.plugins.kaigen

import com.mongodb.client.model.Filters
import jp.nephy.glados.core.extensions.launch
import jp.nephy.glados.core.extensions.message
import jp.nephy.glados.core.extensions.messages.HexColor
import jp.nephy.glados.core.extensions.textChannelsLazy
import jp.nephy.glados.core.plugins.Plugin
import jp.nephy.glados.mongodb
import jp.nephy.jsonkt.ImmutableJsonObject
import jp.nephy.jsonkt.collection
import jp.nephy.jsonkt.delegation.*
import jp.nephy.jsonkt.findAndParse
import java.util.*

object HappyBirthday: Plugin() {
    private val textChannels by textChannelsLazy("happy_birthday")

    @Schedule(hours = [0], minutes = [0])
    fun check() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"))
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DATE)

        val birthdayMembers = mongodb.collection("Member").findAndParse<MemberModel>(Filters.and(Filters.eq("profile.birthday.month", month), Filters.eq("profile.birthday.day", day)))
        for (member in birthdayMembers) {
            val discord = member.accounts.discord.firstOrNull { !it.private && !it.internal }
            val twitter = member.accounts.twitter.firstOrNull { !it.private && !it.internal }

            for (channel in textChannels) {
                channel.message {
                    embed {
                        title(":birthday: お誕生日おめでとうございます！")
                        descriptionBuilder {
                            val discordMember = channel.guild.members.find { discord?.tag == "${it.user.name}#${it.user.discriminator}" }

                            appendln("本日 $month/$day は ${member.effectiveName} ${discordMember?.asMention.orEmpty()} さんの誕生日です。")
                            if (twitter?.sn != null) {
                                append("クソリプでお祝いしましょう！ https://kusoripu.nephy.jp/${twitter.sn}")
                            }
                        }
                        color(HexColor.Good)
                    }
                }.launch()
            }
        }
    }
}

private data class MemberModel(override val json: ImmutableJsonObject): JsonModel {
    val index by int
    val importFrom by nullableString("import_from")
    val id by string
    val name by string
    val nickname by nullableString
    val effectiveName by lazy { nickname ?: name }
    val alias by stringList
    val roles by enumList(Role::class, unknown = Role.Undefined)
    val profile by model<Profile>()
    val skills by modelList<Skill>()
    val accounts by model<Accounts>()

    enum class Role(val description: String): JsonEnum<String> {
        InitialKaigen("初期かいげんメンバー"),

        ProjectManager("プロジェクトマネージャー"),
        Contributor("プロジェクト支援者"),
        Developer("デベロッパー"),

        DiscordAdmin("Discord管理者"),

        Undefined("未定義");

        override val value: String
            get() = name
    }

    data class Profile(override val json: ImmutableJsonObject): JsonModel {
        val bio by nullableString
        val birthday by model<Birthday>()
        val email by stringList
        val iconUrl by string("icon_url")
        val location by nullableString
        val prefecture by string
        val url by stringList
        val amazonWishlistUrl by nullableString("amazon_wishlist_url")

        data class Birthday(override val json: ImmutableJsonObject): JsonModel {
            val day by nullableInt
            val month by nullableInt
            val year by nullableInt
        }
    }

    data class Skill(override val json: ImmutableJsonObject): JsonModel {
        val degree by int
        val name by string
    }

    data class Accounts(override val json: ImmutableJsonObject): JsonModel {
        val twitter by modelList<Twitter>()
        val steam by modelList<Steam>()
        val discord by modelList<Discord>()
        val github by modelList<GitHub>()
        val twitch by modelList<Twitch>()
        val instagram by modelList<Instagram>()
        val niconico by modelList<Niconico>()
        val niconicoCommunity by modelList<NiconicoCommunity>(key = "niconico_community")
        val youtube by modelList<YouTube>()
        val flickr by modelList<Flickr>()
        val minecraft by modelList<Minecraft>()
        val ff14 by modelList<FF14>()
        val spotify by modelList<Spotify>()
        val vrchat by modelList<VRChat>()
        val psn by modelList<PlayStationNetwork>()
        val qiita by modelList<Qiita>()
        val bllizard by modelList<Bllizard>()
        val atcoder by modelList<AtCoder>()
        val annict by modelList<Annict>()
        val codepen by modelList<CodePen>()
        val pinterest by modelList<Pinterest>()
        val foursquare by modelList<Foursquare>()
        val soundcloud by modelList<SoundCloud>()
        val tumblr by modelList<Tumblr>()
        val reddit by modelList<Reddit>()
        val weixin by modelList<Weixin>()
        val alipay by modelList<Alipay>()
        val other by modelList<Other>()

        abstract class Account: JsonModel {
            val private by boolean { false }
            val internal by boolean { false }
        }

        interface WithProfileUrl {
            val profileUrl: String
        }

        interface WithTag {
            val tag: String
            val username: String
                get() = tag.split("#").first()
            val discriminator: String
                get() = tag.split("#").last()
        }

        data class Twitter(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val id by long
            val sn by string
            override val profileUrl: String
                get() = "https://twitter.com/$sn"
        }

        data class Discord(override val json: ImmutableJsonObject): Account(), WithTag {
            override val tag by string
        }

        data class Steam(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val familySharing by longList("family_sharing")
            val id by long
            val username by nullableString
            override val profileUrl: String
                get() = if (username != null) {
                    "https://steamcommunity.com/id/$username"
                } else {
                    "https://steamcommunity.com/profiles/$id"
                }
        }

        data class GitHub(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val username by string
            override val profileUrl: String
                get() = "https://github.com/$username"
        }

        data class Minecraft(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val id by nullableString
            val uuid by string
            override val profileUrl: String
                get() = "https://ja.namemc.com/profile/$uuid"
        }

        data class Twitch(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val id by string
            override val profileUrl: String
                get() = "https://twitch.tv/$id"
        }

        data class Instagram(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val id by string
            override val profileUrl: String
                get() = "https://www.instagram.com/$id"
        }

        data class Niconico(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val id by long
            override val profileUrl: String
                get() = "http://www.nicovideo.jp/user/$id"
        }

        data class NiconicoCommunity(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val id by string
            override val profileUrl: String
                get() = "http://com.nicovideo.jp/community/$id"
        }

        data class YouTube(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val id by string
            override val profileUrl: String
                get() = "https://www.youtube.com/channel/$id"
        }

        data class Flickr(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val id by string
            val username by string
            override val profileUrl: String
                get() = "https://www.flickr.com/photos/$id"
        }

        data class FF14(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val id by long
            override val profileUrl: String
                get() = "https://jp.finalfantasyxiv.com/lodestone/character/$id"
        }

        data class VRChat(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val id by string
            val username by string
            override val profileUrl: String
                get() = "https://www.vrchat.net/home/user/$id"
        }

        data class Spotify(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val id by string
            override val profileUrl: String
                get() = "https://open.spotify.com/user/$id"
        }

        data class PlayStationNetwork(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val id by string
            override val profileUrl: String
                get() = "https://my.playstation.com/profile/$id"
        }

        data class Qiita(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val id by string
            override val profileUrl: String
                get() = "https://qiita.com/$id"
        }

        data class Bllizard(override val json: ImmutableJsonObject): Account(), WithTag {
            override val tag by string
        }

        data class AtCoder(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val username by string
            override val profileUrl: String
                get() = "http://atcoder.jp/user/$username"
        }

        data class Annict(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val username by string
            override val profileUrl: String
                get() = "https://annict.jp/@$username"
        }

        data class CodePen(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val username by string
            override val profileUrl: String
                get() = "https://codepen.io/$username"
        }

        data class Pinterest(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val username by string
            override val profileUrl: String
                get() = "https://www.pinterest.jp/$username"
        }

        data class Foursquare(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val id by long
            override val profileUrl: String
                get() = "https://ja.foursquare.com/user/$id"
        }

        data class SoundCloud(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val username by string
            override val profileUrl: String
                get() = "https://soundcloud.com/$username"
        }

        data class Tumblr(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val username by string
            override val profileUrl: String
                get() = "https://www.tumblr.com/blog/$username"
        }

        data class Reddit(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val username by string
            override val profileUrl: String
                get() = "https://www.reddit.com/user/$username"
        }

        data class Weixin(override val json: ImmutableJsonObject): Account() {
            val username by string
        }

        data class Alipay(override val json: ImmutableJsonObject): Account() {
            val username by string
        }

        data class Other(override val json: ImmutableJsonObject): Account(), WithProfileUrl {
            val id by nullableString
            override val profileUrl by string("profile_url")
            val service by model<UnknownService>()
            val username by nullableString

            data class UnknownService(override val json: ImmutableJsonObject): JsonModel {
                val name by string
                val url by string
            }
        }
    }
}
