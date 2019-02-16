package jp.nephy.glados.api.plugin.twitter

import jp.nephy.glados.api.plugin.Priority

@Target(AnnotationTarget.FUNCTION)
annotation class TwitterEvent(
    val accounts: Array<String>,
    val priority: Priority = Priority.Normal
)
