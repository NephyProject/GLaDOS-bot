package jp.nephy.glados.features

import jp.nephy.glados.core.builder.Color
import jp.nephy.glados.core.builder.edit
import jp.nephy.glados.core.builder.reply
import jp.nephy.glados.core.feature.BotFeature
import jp.nephy.glados.core.feature.subscription.Command
import jp.nephy.glados.core.feature.subscription.CommandEvent
import jp.nephy.glados.core.feature.subscription.CommandPermission
import jp.nephy.glados.jda
import net.dv8tion.jda.core.entities.Icon
import java.net.URL

class IconCommand: BotFeature() {
    @Command(permission = CommandPermission.OwnerOnly, description = "GLaDOSのアイコンを変更します。", args = "<アイコンURL>")
    fun icon(event: CommandEvent) {
        event.reply {
            embed {
                title("アイコンを変更しています...")
                color(Color.Plain)
                timestamp()
            }
        }.queue {
            val bytes = URL(event.args).readBytes()
            jda.selfUser.manager.setAvatar(Icon.from(bytes)).queue { _ ->
                it.edit {
                    embed {
                        title("アイコンを変更しました！")
                        color(Color.Good)
                    }
                }.queue()
            }
        }
    }
}
