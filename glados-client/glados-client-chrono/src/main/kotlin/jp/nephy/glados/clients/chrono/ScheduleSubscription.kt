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

package jp.nephy.glados.clients.chrono

import jp.nephy.glados.api.Plugin
import jp.nephy.glados.api.annotations.Priority
import jp.nephy.glados.clients.GLaDOSSubscription
import java.util.*
import kotlin.reflect.KFunction

class ScheduleSubscription(
    override val plugin: Plugin, override val function: KFunction<*>, override val annotation: Schedule
): GLaDOSSubscription<Schedule, ScheduleEvent>() {
    override val priority: Priority
        get() = annotation.priority

    private val timing = object {
        val hours: Set<Int>
        val minutes: Set<Int>

        init {
            val hoursADay = 0 until 24
            val h = annotation.hours.toMutableSet()
            if (annotation.multipleHours.isNotEmpty()) {
                annotation.multipleHours.forEach { multipleHour ->
                    hoursADay.map {
                        it * multipleHour
                    }.filter {
                        it in hoursADay
                    }.forEach {
                        h.add(it)
                    }
                }
            }
            hours = h.ifEmpty { hoursADay }.toSortedSet()

            val minutesAnHour = 0 until 60
            val m = annotation.minutes.toMutableSet()
            if (annotation.multipleMinutes.isNotEmpty()) {
                annotation.multipleMinutes.forEach { multipleMinute ->
                    minutesAnHour.map {
                        it * multipleMinute
                    }.filter {
                        it in minutesAnHour
                    }.forEach {
                        m.add(it)
                    }
                }
            }
            minutes = m.ifEmpty { minutesAnHour }.toSortedSet()
        }
    }

    fun matches(calendar: Calendar): Boolean {
        return calendar.get(Calendar.HOUR_OF_DAY) in timing.hours && calendar.get(Calendar.MINUTE) in timing.minutes
    }
}
