package jp.nephy.glados.api.plugin.loop

import jp.nephy.glados.api.plugin.Priority
import java.util.concurrent.TimeUnit

@Target(AnnotationTarget.FUNCTION)
annotation class Loop(
    val interval: Long, val unit: TimeUnit, val priority: Priority = Priority.Normal
)
