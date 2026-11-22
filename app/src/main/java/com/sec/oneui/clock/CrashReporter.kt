/*
 * TEMPORARY - remove after debugging
 */
package com.sec.oneui.clock

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

// TEMPORARY - remove after debugging
object CrashReporter {
    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                pw.flush()
                val stackTrace = sw.toString()

                val crashFile = File(appContext.filesDir, FILE_NAME)
                crashFile.writeText(stackTrace)
            } catch (ignored: Throwable) {
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    fun getCrashFile(context: Context): File {
        return File(context.applicationContext.filesDir, FILE_NAME)
    }

    fun readCrash(context: Context): String? {
        val file = getCrashFile(context)
        return if (file.exists() && file.length() > 0) {
            try {
                file.readText()
            } catch (e: Throwable) {
                null
            }
        } else {
            null
        }
    }

    fun clear(context: Context) {
        try {
            val file = getCrashFile(context)
            if (file.exists()) {
                file.delete()
            }
        } catch (ignored: Throwable) {
        }
    }

    fun readAndClear(context: Context): String? {
        val content = readCrash(context)
        clear(context)
        return content
    }
}
