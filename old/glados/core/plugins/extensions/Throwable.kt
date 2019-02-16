package jp.nephy.glados.core.plugins.extensions

import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.InvocationTargetException

val Throwable.stackTraceString: String
    get() {
        StringWriter().use { sw ->
            PrintWriter(sw).use { pw ->
                printStackTrace(pw)
                pw.flush()
            }
            return sw.toString()
        }
    }

val Throwable.invocationException: Throwable
    get() = if (this is InvocationTargetException) {
        targetException
    } else {
        this
    }
