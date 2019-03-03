/*
 * The MIT License (MIT)
 *
 *     Copyright (c) 2017-2019 Nephy Project Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package jp.nephy.glados.clients.discord.command

import jp.nephy.glados.api.Plugin
import jp.nephy.glados.clients.GLaDOSSubscriptionClient
import jp.nephy.glados.clients.discord.config.textChannel
import jp.nephy.glados.clients.discord.extensions.awaitAndDelete
import jp.nephy.glados.clients.discord.extensions.config
import jp.nephy.glados.clients.discord.extensions.isBotOrSelfUser
import jp.nephy.glados.clients.discord.extensions.launchAndDelete
import jp.nephy.glados.clients.discord.extensions.messages.HexColor
import jp.nephy.glados.clients.discord.extensions.messages.emojiEnumPrompt
import jp.nephy.glados.clients.discord.extensions.messages.reply
import jp.nephy.glados.clients.utils.fullname
import jp.nephy.glados.clients.utils.invoke
import jp.nephy.glados.clients.utils.isExperimental
import jp.nephy.glados.clients.utils.subscriptions
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.GenericMessageEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation

object DiscordCommandSubscriptionClient: GLaDOSSubscriptionClient<DiscordCommand, DiscordCommandEvent, DiscordCommandSubscription>(), EventListener {
    override fun create(plugin: Plugin, function: KFunction<*>, eventClass: KClass<*>): DiscordCommandSubscription? {
        if (eventClass != DiscordCommandEvent::class) {
            return null
        }

        val annotation = function.findAnnotation<DiscordCommand>()
        if (annotation == null) {
            logger.warn { "関数: \"${plugin.name}#${function.name}\" は @DiscordCommand が付与されていません。スキップします。" }
            return null
        }
        
        return DiscordCommandSubscription(plugin, function, annotation).also {
            if (it.description == null) {
                logger.warn { "Command: ${it.fullname} は description が設定されていません。" }
            }
        }
    }
    
    override fun start() {}

    override fun stop() {}
    
    val spaceRegex: Regex = "\\s+".toRegex()

    @SubscribeEvent
    fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBotOrSelfUser) {
            return
        }

        launch {
            handleMessage(event, event.message, event.channelType)
        }
    }

    @SubscribeEvent
    fun onMessageUpdate(event: MessageUpdateEvent) {
        if (event.author.isBotOrSelfUser) {
            return
        }

        launch {
            handleMessage(event, event.message, event.channelType)
        }
    }

    private suspend fun handleMessage(event: GenericMessageEvent, message: Message, channelType: ChannelType) {
        val text = message.contentDisplay.trim()

        for (subscription in subscriptions) {
            val args = subscription.parseArgs(text) ?: continue
            val commandEvent = when (event) {
                is MessageReceivedEvent -> DiscordCommandEvent(args, subscription, event)
                is MessageUpdateEvent -> DiscordCommandEvent(args, subscription, event)
                else -> throw UnsupportedOperationException("Unknown event: ${event.javaClass.canonicalName}.")
            }

            when {
                !subscription.satisfyChannelTypeRequirement(channelType) -> {
                    commandEvent.reply {
                        embed {
                            title("コマンドエラー: `${commandEvent.command.primaryCommandSyntax}`")
                            description { "このコマンドは ${subscription.targetChannelType.types.joinToString(", ") { it.name }} チャンネルでのみ実行可能です。" }
                            color(HexColor.Bad)
                            timestamp()
                        }
                    }.launchAndDelete(15, TimeUnit.SECONDS)

                    logger.warn { "\"$text\": チャンネルタイプが対象外のため実行されませんでした。 (${commandEvent.authorName})" }
                }
                !subscription.satisfyBotChannelRequirement(event.channel) -> {
                    commandEvent.reply {
                        embed {
                            title("コマンドエラー: `${commandEvent.command.primaryCommandSyntax}`")
                            description { "このコマンドは ${event.guild.config?.textChannel("bot")?.asMention ?: "#bot"} チャンネルでのみ実行可能です。" }
                            color(HexColor.Bad)
                            timestamp()
                        }
                    }.launchAndDelete(15, TimeUnit.SECONDS)

                    logger.warn { "\"$text\": Bot チャンネルではないため実行されませんでした。 (${commandEvent.authorName})" }
                }
                !satisfyCommandAvailabilityForGuildRequirement(event.guild) -> {
                    commandEvent.reply {
                        embed {
                            title("コマンドエラー: `${commandEvent.command.primaryCommandSyntax}`")
                            description { "サーバ ${event.guild.name} ではGLaDOSのコマンド機能は利用できません。サーバ管理者またはGLaDOS開発者にご連絡ください。" }
                            color(HexColor.Bad)
                            timestamp()
                        }
                    }.launchAndDelete(30, TimeUnit.SECONDS)

                    logger.warn { "\"$text\": サーバ ${event.guild.name} ではコマンド機能が無効なので実行されませんでした。 (${commandEvent.authorName})" }
                }
                !subscription.satisfyArgumentsRequirement(commandEvent.argList) -> {
                    commandEvent.reply {
                        embed {
                            title("コマンドエラー: `${commandEvent.command.primaryCommandSyntax}`")
                            descriptionBuilder {
                                appendln("コマンドの引数の数が一致しません。`!help`コマンドも必要に応じてご確認ください。")
                                append("実行例: `${subscription.primaryCommandSyntax} ${subscription.arguments.joinToString(" ") { "<$it>" }}`")
                            }
                            color(HexColor.Bad)
                            timestamp()
                        }
                    }.launchAndDelete(30, TimeUnit.SECONDS)

                    logger.warn { "\"$text\": コマンドの引数が足りません。 (${commandEvent.authorName})" }
                }
                !subscription.satisfyCommandConditionOfWhileInAnyVoiceChannel(message.member?.voiceState) -> {
                    commandEvent.reply {
                        embed {
                            title("コマンドエラー: `${commandEvent.command.primaryCommandSyntax}`")
                            description { "このコマンドはボイスチャンネルに参加中のみ実行できます。" }
                            color(HexColor.Bad)
                            timestamp()
                        }
                    }.launchAndDelete(30, TimeUnit.SECONDS)

                    logger.warn { "\"$text\": コマンド実行の要件(WhileInAnyVoiceChannel)が足りません。 (${commandEvent.authorName})" }
                }
                !subscription.satisfyCommandConditionOfWhileInSameVoiceChannel(message.member) -> {
                    commandEvent.reply {
                        embed {
                            title("コマンドエラー: `${commandEvent.command.primaryCommandSyntax}`")
                            description { "このコマンドはGLaDOSと同じボイスチャンネルに参加中のみ実行できます。" }
                            color(HexColor.Bad)
                            timestamp()
                        }
                    }.launchAndDelete(30, TimeUnit.SECONDS)

                    logger.warn { "\"$text\": コマンド実行の要件(WhileInSameVoiceChannel)が足りません。 (${commandEvent.authorName})" }
                }
                !subscription.satisfyCommandPermissionOfAdminOnly(event) -> {
                    if (event.channelType == ChannelType.TEXT) {
                        commandEvent.reply {
                            embed {
                                title("コマンドエラー: `${commandEvent.command.primaryCommandSyntax}`")
                                description { "このコマンドは`${event.guild.name}`の管理者ロールが付与されているメンバーのみが実行できます。判定に問題がある場合はサーバのオーナーにご連絡ください。" }
                                color(HexColor.Bad)
                                timestamp()
                            }
                        }.launchAndDelete(30, TimeUnit.SECONDS)

                        logger.warn { "\"$text\": 管理者ロールがないため実行されませんでした。 (${commandEvent.authorName})" }
                    } else {
                        commandEvent.reply {
                            embed {
                                title("コマンドエラー: `${commandEvent.command.primaryCommandSyntax}`")
                                description { "このコマンドは管理者ロールが必要であるため, DMでは実行できません。" }
                                color(HexColor.Bad)
                                timestamp()
                            }
                        }.launchAndDelete(30, TimeUnit.SECONDS)

                        logger.warn { "\"$text\": 管理者ロールがないため(DM)実行されませんでした。 (${commandEvent.authorName})" }
                    }
                }
                !subscription.satisfyCommandPermissionOfOwnerOnly(event) -> {
                    commandEvent.reply {
                        embed {
                            title("コマンドエラー: `${commandEvent.command.primaryCommandSyntax}`")
                            description { "このコマンドはGLaDOSのオーナーのみが実行できます。" }
                            color(HexColor.Bad)
                            timestamp()
                        }
                    }.launchAndDelete(30, TimeUnit.SECONDS)

                    logger.warn { "\"$text\": オーナーではないため実行されませんでした。 (${commandEvent.authorName})" }
                }
                else -> {
                    if (subscription.isExperimental) {
                        commandEvent.message.emojiEnumPrompt<ExperimentalConsent> {
                            title("`${commandEvent.command.primaryCommandSyntax}`")
                            description { "⚠ この機能は現在 試験中(Experimental) です。予期しない不具合が発生する可能性がありますが, ご理解の上ご利用ください。" }
                            color(HexColor.Change)
                            timeout(30, TimeUnit.SECONDS)
                        }.onSuccess {
                            if (it.selected == ExperimentalConsent.Agree) {
                                subscription.invoke(commandEvent)
                                subscription.logger.info { "同意したので実行されました。(${event.guild?.name})" }
                            } else {
                                commandEvent.message.reply {
                                    embed {
                                        title("`${commandEvent.command.primaryCommandSyntax}`")
                                        description { "キャンセルしました。" }
                                        color(HexColor.Bad)
                                        timestamp()
                                    }
                                }.awaitAndDelete(15, TimeUnit.SECONDS)
                            }
                        }
                    } else {
                        subscription.invoke(commandEvent)
                        subscription.logger.trace { "実行されました。(${event.guild?.name})" }
                    }

                    return
                }
            }
        }
    }
}
