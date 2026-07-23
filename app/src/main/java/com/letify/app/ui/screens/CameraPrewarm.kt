package com.letify.app.ui.screens

import android.content.Context
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.common.util.concurrent.ListenableFuture

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
    }

    fun future(context: Context): ListenableFuture<ProcessCameraProvider> {
        warm(context)
        return cached!!
    }
}
