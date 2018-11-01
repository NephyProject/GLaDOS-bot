package jp.nephy.glados.plugins.command

import jp.nephy.glados.core.extensions.await
import jp.nephy.glados.core.plugins.Plugin
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object LaTexCommand: Plugin() {
    @Command(description = "LaTexコードを画像に変換します。", args = ["LaTexコード"])
    suspend fun tex(event: Command.Event) {
        val formula = TeXFormula(event.args)
        val image = formula.createBufferedImage(TeXConstants.STYLE_DISPLAY, 50f, Color.black, Color.white) as BufferedImage

        ByteArrayOutputStream().use { stream ->
            ImageIO.write(image, "png", stream)

            event.channel.sendFile(stream.toByteArray(), "latex.png").await()
        }
    }
}
