package com.letify.app

import android.app.Application
import android.content.Context
import android.util.Log
import com.letify.app.ui.icons.SolarIconLoader
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Application entry point.
 *
 * Beyond the usual lifecycle hook, this class installs a process-wide
 * `Thread.UncaughtExceptionHandler` that serialises every fatal stack
 * trace to a file in the app's cache directory. The next time the app
 * launches it can detect the file, show the user a one-tap "Copy logs"
 * dialog, and forward the captured stack trace to the clipboard so they
 * can paste it back to us. After capture we still call through to the
 * platform-default handler so the OS gets to do its own crash reporting
 * (and the process actually dies).
 *
 * onCreate also kicks off the background icon prewarm so the work
 * starts overlapping with MainActivity's window inflation instead of
 * waiting for setContent. MainActivity then briefly waits on a latch
 * for the 5 navbar icons to be decoded before calling setContent —
 * see SolarIconLoader for the full design.
 */
class LetifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
        // Fire-and-forget — populates SolarIconLoader.bitmaps on a
        // daemon worker. Idempotent; MainActivity calls it again
        // defensively in case Application.onCreate was skipped (e.g.
        // by some constrained runtime).
        SolarIconLoader.prewarmAll(this)
    }
}

object CrashReporter {

    private const val TAG = "LetifyCrash"
    private const val CRASH_FILE = "last_crash.txt"
    private const val MAX_RETAINED_BYTES = 256 * 1024

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeReport(appContext, thread, throwable)
            } catch (t: Throwable) {
                Log.e(TAG, "failed to write crash report", t)
            }
            // Defer to the platform handler so Android still surfaces the
            // crash dialog / kills the process. Swallowing here would
            // leave the process zombied with a torn-down UI.
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun crashFile(context: Context): File = File(context.cacheDir, CRASH_FILE)

    private fun writeReport(context: Context, thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        PrintWriter(sw).use { pw ->
            pw.println("Letify crash log")
            pw.println(
                "Time: " +
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date()),
            )
            pw.println("Package: ${context.packageName}")
            pw.println("Thread: ${thread.name}")
            pw.println("Android: ${android.os.Build.VERSION.RELEASE} (sdk ${android.os.Build.VERSION.SDK_INT})")
            pw.println("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            pw.println()
            throwable.printStackTrace(pw)
        }
        val text = sw.toString()
            .let { if (it.length > MAX_RETAINED_BYTES) it.take(MAX_RETAINED_BYTES) else it }
        crashFile(context).writeText(text)
    }

    /**
     * Returns the previously-captured crash report, or `null` if the
     * previous session terminated cleanly. The file is left on disk —
     * call [consume] after the user has had a chance to copy it.
     */
    fun read(context: Context): String? {
        val f = crashFile(context.applicationContext)
        return if (f.exists() && f.length() > 0L) f.readText() else null
    }

    fun consume(context: Context) {
        crashFile(context.applicationContext).delete()
    }
}
