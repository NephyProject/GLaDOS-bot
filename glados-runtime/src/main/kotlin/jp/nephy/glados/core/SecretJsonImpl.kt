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

@file:Suppress("UNUSED")

package jp.nephy.glados.core

import jp.nephy.glados.api.FileSystemEventListener
import jp.nephy.glados.api.GLaDOS
import jp.nephy.glados.api.Logger
import jp.nephy.glados.api.SecretJson
import jp.nephy.glados.clients.logger.of
import jp.nephy.jsonkt.*
import kotlinx.serialization.json.JsonObject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal class SecretJsonImpl(override val json: JsonObject): SecretJson {
    companion object {
        private val secretJsonPath: Path = Paths.get("config.secret.json")

        private const val emptyJson = "{\n    \n}\n"
        
        fun load(): SecretJson {
            val logger = Logger.of("GLaDOS.Config.SecretJson")
            
            if (!Files.exists(secretJsonPath)) {
                logger.info { "$secretJsonPath は存在しません。空の JSON を新たに作成します。" }
                
                secretJsonPath.toFile().writeText(emptyJson)
            }
            
            logger.info { "シークレット設定をロードします。" }
            
            return secretJsonPath.parse<SecretJsonImpl>()
        }

        fun installFileSystemListener() {
            FileSystemWatcherImpl.addListener(object: FileSystemEventListener {
                override fun onCreated(path: Path) {
                    handleFileChange(path)
                }

                override fun onModified(path: Path) {
                    handleFileChange(path)
                }
            })
        }

        @Suppress("DEPRECATION")
        private fun handleFileChange(path: Path) {
            if (Files.isSameFile(path, secretJsonPath)) {
                GLaDOS.Instance.secret = load()
            }
        }
    }
}
