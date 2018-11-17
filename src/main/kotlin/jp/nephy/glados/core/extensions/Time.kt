package jp.nephy.glados.core.extensions

import org.apache.commons.lang3.time.FastDateFormat
import java.util.*
import kotlin.math.roundToLong

fun Long.toDeltaMilliSecondString(): String {
    return (Date().time - this).toMilliSecondString()
}

fun Long.toMilliSecondString(): String {
    val sec = div(1000.0).roundToLong()
    return buildString {
        val (q1, r1) = sec.divMod(60 * 60 * 24)
        if (q1 > 0) {
            append("${q1}日")
        }

        val (q2, r2) = r1.divMod(60 * 60)
        if (q2 > 0) {
            append("${q2}時間")
        }

        val (q3, r3) = r2.divMod(60)
        if (q3 > 0) {
            append("${q3}分")
        }
        append("${r3}秒")
    }
}

fun Date.format(pattern: String): String {
    return FastDateFormat.getInstance(pattern).format(this)
}

fun Calendar.format(pattern: String): String {
    return FastDateFormat.getInstance(pattern).format(this)
}

val Calendar.month: Int
    get() = get(Calendar.MONTH) + 1

val Calendar.date: Int
    get() = get(Calendar.DATE)
