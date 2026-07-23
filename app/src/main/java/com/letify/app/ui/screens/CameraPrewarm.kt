package com.letify.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * ProcessCameraProvider + shared use-cases, and a disk-backed "last frame"
 * used as the Telegram-style blurred open placeholder.
 */
object CameraPrewarm {
    @Volatile
    private var cached: ListenableFuture<ProcessCameraProvider>? = null

    /** Absolute path of the last saved preview/capture frame, or null. */
    @Volatile
    var placeholderPath: String? = null
        private set

    fun warm(context: Context) {
        if (cached == null) {
            synchronized(this) {
                if (cached == null) {
                    cached = ProcessCameraProvider.getInstance(context.applicationContext)
                }
            }
        }
        imageCapture
        videoCapture
        executor
        // Resolve any previously saved placeholder on warm.
        val f = placeholderFile(context)
        if (f.exists() && f.length() > 64) {
            placeholderPath = f.absolutePath
        }
    }

    fun future(context: Context): ListenableFuture<ProcessCameraProvider> {
        warm(context)
        return cached!!
    }

    fun placeholderFile(context: Context): File =
        File(context.applicationContext.cacheDir, "camera_open_placeholder.jpg")

    /**
     * Persist a still for the next camera open. Runs encode on [executor]
     * so the UI thread is never blocked by JPEG compression.
     */
    fun savePlaceholder(context: Context, bitmap: Bitmap?) {
        if (bitmap == null || bitmap.isRecycled || bitmap.width < 2) return
        val copy = runCatching {
            bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false)
        }.getOrNull() ?: return
        val out = placeholderFile(context)
        executor.execute {
            try {
                // Downscale so decode+blur on open is cheap and soft.
                val maxSide = 480
                val w = copy.width
                val h = copy.height
                val scaled = if (w > maxSide || h > maxSide) {
                    val s = maxSide.toFloat() / maxOf(w, h)
                    Bitmap.createScaledBitmap(
                        copy,
                        (w * s).toInt().coerceAtLeast(1),
                        (h * s).toInt().coerceAtLeast(1),
                        true,
                    ).also { if (it !== copy) copy.recycle() }
                } else copy
                out.outputStream().use { stream ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 72, stream)
                }
                if (scaled !== copy) scaled.recycle() else copy.recycle()
                placeholderPath = out.absolutePath
            } catch (_: Throwable) {
                runCatching { copy.recycle() }
            }
        }
    }

    /** Also accept a photo file path (after capture) as the next placeholder. */
    fun savePlaceholderFromFile(context: Context, path: String) {
        executor.execute {
            try {
                val src = android.graphics.BitmapFactory.decodeFile(path) ?: return@execute
                // Re-use the same downscale path via a one-shot on this thread.
                val maxSide = 480
                val w = src.width
                val h = src.height
                val scaled = if (w > maxSide || h > maxSide) {
                    val s = maxSide.toFloat() / maxOf(w, h)
                    Bitmap.createScaledBitmap(
                        src,
                        (w * s).toInt().coerceAtLeast(1),
                        (h * s).toInt().coerceAtLeast(1),
                        true,
                    ).also { src.recycle() }
                } else src
                val out = placeholderFile(context)
                out.outputStream().use { stream ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 72, stream)
                }
                scaled.recycle()
                placeholderPath = out.absolutePath
            } catch (_: Throwable) {
            }
        }
    }

    val imageCapture: ImageCapture by lazy {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val recorder: Recorder by lazy {
        Recorder.Builder()
            .setQualitySelector(
                QualitySelector.fromOrderedList(
                    listOf(Quality.SD, Quality.LOWEST, Quality.HD),
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD),
                ),
            )
            .build()
    }

    val videoCapture: VideoCapture<Recorder> by lazy { VideoCapture.withOutput(recorder) }

    val executor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
}
