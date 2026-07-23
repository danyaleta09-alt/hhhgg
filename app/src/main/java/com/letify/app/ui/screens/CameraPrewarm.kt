package com.letify.app.ui.screens

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * `ProcessCameraProvider.getInstance()` is what actually costs time when the
 * camera screen opens — it touches the camera HAL and can take well over a
 * frame or two on a mid-range phone. Doing that for the first time exactly
 * when the slide-up animation starts is what caused the visible stutter.
 *
 * This holds one cached [ListenableFuture] for the whole process lifetime.
 * Call [warm] as early as possible (app start, and again defensively right
 * before the slide-up begins) so that by the time [CameraCaptureScreen] asks
 * for it, the provider is already resolved (or resolving in the background)
 * instead of starting cold.
 *
 * It also holds the [ImageCapture] / [Recorder] / [VideoCapture] use-cases
 * and the capture [ExecutorService]. These used to live in `remember {}`
 * inside CameraCaptureScreen, which meant every open rebuilt them from
 * scratch (the screen is fully unmounted on close) — extra main-thread work
 * landing on exactly the same frames as the slide-up. Building them once for
 * the whole process and reusing them on every open removes that cost too.
 */
object CameraPrewarm {
    @Volatile
    private var cached: ListenableFuture<ProcessCameraProvider>? = null

    fun warm(context: Context) {
        if (cached == null) {
            synchronized(this) {
                if (cached == null) {
                    cached = ProcessCameraProvider.getInstance(context.applicationContext)
                }
            }
        }
        // Touch the lazies so the use-cases/executor are built now, off the
        // camera-screen's first composition.
        imageCapture
        videoCapture
        executor
    }

    fun future(context: Context): ListenableFuture<ProcessCameraProvider> {
        warm(context)
        return cached!!
    }

    val imageCapture: ImageCapture by lazy {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    // A selector locked to a single Quality silently produces a broken/empty
    // output file on any device (or lens — the front camera is a frequent
    // offender) that doesn't support that exact profile. Give it a fallback
    // chain so it always resolves to *something* recordable instead of
    // failing quietly.
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

    /** One long-lived background thread for capture callbacks — never shut down. */
    val executor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
}
