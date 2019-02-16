package jp.nephy.glados.core

import java.nio.file.Files

object JarLoader {
    
    @JvmStatic
    fun loadPlugins() {
        Files.walk()
    }
}
