package jp.nephy.glados.features

import com.mongodb.client.model.Filters
import jp.nephy.glados.config
import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.message
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Listener
import jp.nephy.glados.core.isFalseOrNull
import jp.nephy.glados.model.MemberModel
import jp.nephy.glados.secret
import jp.nephy.jsonkt.collection
import jp.nephy.jsonkt.database
import jp.nephy.jsonkt.findAndParse
import jp.nephy.jsonkt.mongodb
import jp.nephy.utils.StringLinkedSingleCache
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.ReadyEvent
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class HappyBirthday: BotFeature() {
    private val db = mongodb(secret.forKey("mongodb_host")).database("bot")
    private var lastDate by StringLinkedSingleCache { "" }

    @Listener
    override fun onReady(event: ReadyEvent) {
        for (guild in event.jda.guilds) {
            val guildConfig = config.forGuild(guild)
            val defaultChannel = guildConfig?.textChannel("default") ?: continue
            if (guildConfig.boolOption("enable_happy_birthday").isFalseOrNull()) {
                continue
            }
            watch(guild, defaultChannel)
        }
    }

    private val timezone = TimeZone.getTimeZone("Asia/Tokyo")
    private val date: String
        get() = SimpleDateFormat("MM/dd").apply {
            timeZone = timezone
        }.format(Date())

    private fun watch(guild: Guild, channel: TextChannel) {
        launch {
            while (isActive) {
                try {
                    if (lastDate != date) {
                        val calendar = Calendar.getInstance(timezone)
                        val month = calendar.get(Calendar.MONTH) + 1
                        val day = calendar.get(Calendar.DATE)

                        db.collection("Member").findAndParse<MemberModel>(Filters.and(Filters.eq("profile.birthday.month", month), Filters.eq("profile.birthday.day", day))).forEach {
                            val discord = it.accounts.discord.firstOrNull { !it.private && !it.internal }
                            val discordMember = guild.members.find { discord?.tag == "${it.user.name}#${it.user.discriminator}" }
                            val twitter = it.accounts.twitter.firstOrNull { !it.private && !it.internal }

                            channel.message {
                                embed {
                                    title(":birthday: 誕生日おめでとうございます！")
                                    descriptionBuilder {
                                        appendln("今日 $month/$day は ${it.effectiveName} ${discordMember?.asMention.orEmpty()} さんの誕生日です。")
                                        if (twitter?.sn != null) {
                                            append("クソリプでお祝いしましょう！ https://kusoripu.nephy.jp/${twitter.sn}")
                                        }
                                    }
                                    color(Color.Good)
                                    timestamp()
                                }
                            }.queue()
                        }

                        lastDate = date
                    }
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    logger.error(e) { "誕生日のチェック中にエラーが発生しました." }
                }

                try {
                    delay(1, TimeUnit.MINUTES)
                } catch (e: CancellationException) {
                    break
                }
            }
        }
    }
}
