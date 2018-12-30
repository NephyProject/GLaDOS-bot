package jp.nephy.glados.core.plugins.extensions.jda.messages.prompt

interface PromptEnum {
    val promptTitle: String
    val promptDescription: String?
        get() = null
}
