package jp.nephy.glados.api.plugin.chrono

import jp.nephy.glados.api.plugin.Priority

@Target(AnnotationTarget.FUNCTION)
annotation class Schedule(
    val hours: IntArray = [],
    val minutes: IntArray = [],
    val multipleHours: IntArray = [],
    val multipleMinutes: IntArray = [],
    val priority: Priority = Priority.Normal
)
