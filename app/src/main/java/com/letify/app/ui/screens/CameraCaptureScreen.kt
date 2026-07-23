package com.letify.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.letify.app.ui.components.NoFeedbackButton
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.theme.Letify
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "CameraCapture"
private const val LENS_FADE_MS = 130
/** Never fully blank the feed on lens-flip — dip and recover instead of flashing to black. */
private const val LENS_FADE_MIN_ALPHA = 0.35f

@OptIn(ExperimentalCamera2Interop::class)
@Composable
fun CameraCaptureScreen(
    onBack: () -> Unit,
    onCaptured: () -> Unit = {},
    // False while the sheet is sliding open — no PreviewView, no bind.
    // Flipped true only after open settles. Stays true during close slide;
    // host disposes the screen after it's off-screen.
    readyToBind: Boolean = true,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = LocalAppState.current
    val scope = rememberCoroutineScope()

    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED,
        )
    }
    var hasAudio by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasCamera = result[Manifest.permission.CAMERA] == true
        hasAudio = result[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(Unit) {
        if (!hasCamera) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
            )
        }
    }

    // Live preview opacity — 0 until StreamState.STREAMING, then eased in.
    val previewAlpha = remember { Animatable(0f) }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isRecording by remember { mutableStateOf(false) }
    var captureBusy by remember { mutableStateOf(false) }
    val shutterFlash = remember { Animatable(0f) }
    var lastThumb by remember { mutableStateOf<String?>(null) }
    var sessionCaptureCount by remember { mutableIntStateOf(0) }
    // Telegram-style open placeholder. Resolved once per screen mount from:
    //  1) disk cache written on previous close/capture (most reliable)
    //  2) newest photo in the media library
    val placeholderUri = remember {
        val cache = CameraPrewarm.placeholderFile(context)
        when {
            cache.exists() && cache.length() > 64L -> cache.toURI().toString()
            else -> state.mediaItems.firstOrNull { !it.isVideo }?.uri
        }
    }
    // Exposure compensation (яркость кадра) — CameraX EV index.
    var exposureIndex by remember { mutableIntStateOf(0) }
    var exposureMin by remember { mutableIntStateOf(0) }
    var exposureMax by remember { mutableIntStateOf(0) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var minZoom by remember { mutableFloatStateOf(1f) }
    var maxZoom by remember { mutableFloatStateOf(1f) }
    // Ultra-wide is often a *separate* lens (minZoom stays 1.0 on main).
    var useUltraWide by remember { mutableStateOf(false) }
    var ultraWideAvailable by remember { mutableStateOf(false) }
    // Tools island: flash / timer / zoom / exposure / dual
    var showExposure by remember { mutableStateOf(false) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var timerSec by remember { mutableIntStateOf(0) } // 0, 3, 10
    var timerRemaining by remember { mutableIntStateOf(0) }
    var recordSeconds by remember { mutableIntStateOf(0) }
    // Video lock (Telegram-style): drag to padlock while holding to keep recording.
    var recordingLocked by remember { mutableStateOf(false) }
    var lockArmed by remember { mutableStateOf(false) }
    // Dual-camera: concurrent front+back when the device supports it.
    var dualMode by remember { mutableStateOf(false) }
    var dualSupported by remember { mutableStateOf(false) }
    var pipOffset by remember { mutableStateOf(Offset(48f, 160f)) } // px inside preview
    var secondaryPreviewView by remember { mutableStateOf<PreviewView?>(null) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordSeconds = 0
            while (true) {
                delay(1000)
                recordSeconds++
            }
        } else {
            recordingLocked = false
            lockArmed = false
        }
    }

    val imageCapture = CameraPrewarm.imageCapture
    val videoCapture = CameraPrewarm.videoCapture
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    val cameraExecutor = CameraPrewarm.executor
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var bindGeneration by remember { mutableIntStateOf(0) }

    // Reset fade when bind is dropped (host closed / open cancelled).
    LaunchedEffect(readyToBind) {
        if (!readyToBind) {
            previewAlpha.snapTo(0f)
            previewView = null
        }
    }

    // Bind only while readyToBind (after open slide settles). Fade-in waits
    // for StreamState.STREAMING so the first painted frame eases in.
    DisposableEffect(
        readyToBind, lensFacing, previewView, lifecycleOwner,
        bindGeneration, hasCamera, useUltraWide, dualMode, secondaryPreviewView,
    ) {
        val view = previewView
        if (!readyToBind || view == null || !hasCamera) {
            onDispose { }
        } else {
            val mainExecutor = ContextCompat.getMainExecutor(context)
            val future = CameraPrewarm.future(context)
            var provider: ProcessCameraProvider? = null
            var boundPreview: Preview? = null
            var boundSecondaryPreview: Preview? = null
            var cancelled = false
            val streamObserver = Observer<PreviewView.StreamState> { state ->
                if (!cancelled && state == PreviewView.StreamState.STREAMING) {
                    scope.launch {
                        previewAlpha.animateTo(1f, tween(200))
                    }
                }
            }
            view.previewStreamState.observe(lifecycleOwner, streamObserver)

            future.addListener({
                if (cancelled) return@addListener
                try {
                    provider = future.get()
                    val p = provider ?: return@addListener

                    val backInfos = p.availableCameraInfos.filter {
                        it.lensFacing == CameraSelector.LENS_FACING_BACK
                    }
                    ultraWideAvailable = backInfos.size > 1 || backInfos.any { info ->
                        runCatching {
                            val focals = Camera2CameraInfo.from(info).getCameraCharacteristic(
                                android.hardware.camera2.CameraCharacteristics
                                    .LENS_INFO_AVAILABLE_FOCAL_LENGTHS,
                            )
                            (focals?.minOrNull() ?: 99f) < 2.5f
                        }.getOrDefault(false)
                    }
                    // Only trust real concurrent camera pairs. The system feature
                    // flag alone is a false positive on many OEMs and led to
                    // bind → fail → dualMode=false flicker loops.
                    dualSupported = p.availableConcurrentCameraInfos.any { combo ->
                        combo.any { it.lensFacing == CameraSelector.LENS_FACING_BACK } &&
                            combo.any { it.lensFacing == CameraSelector.LENS_FACING_FRONT }
                    }

                    fun selectorFor(facing: Int, ultra: Boolean): CameraSelector {
                        val b = CameraSelector.Builder().requireLensFacing(facing)
                        if (ultra && facing == CameraSelector.LENS_FACING_BACK) {
                            b.addCameraFilter { infos ->
                                val sorted = infos.sortedBy { info ->
                                    runCatching {
                                        Camera2CameraInfo.from(info).getCameraCharacteristic(
                                            android.hardware.camera2.CameraCharacteristics
                                                .LENS_INFO_AVAILABLE_FOCAL_LENGTHS,
                                        )?.minOrNull() ?: 99f
                                    }.getOrDefault(99f)
                                }
                                listOfNotNull(sorted.firstOrNull())
                            }
                        }
                        return b.build()
                    }

                    val selector = selectorFor(
                        lensFacing,
                        useUltraWide && lensFacing == CameraSelector.LENS_FACING_BACK,
                    )
                    val preview = Preview.Builder().build().also { pr ->
                        boundPreview = pr
                        pr.setSurfaceProvider(view.surfaceProvider)
                    }

                    p.unbindAll()
                    if (cancelled) return@addListener

                    val secView = secondaryPreviewView
                    val canDual = dualMode && dualSupported && secView != null
                    val cam: Camera? = if (canDual) {
                        val secondaryFacing =
                            if (lensFacing == CameraSelector.LENS_FACING_BACK)
                                CameraSelector.LENS_FACING_FRONT
                            else CameraSelector.LENS_FACING_BACK
                        val secPreview = Preview.Builder().build().also { pr ->
                            boundSecondaryPreview = pr
                            pr.setSurfaceProvider(secView.surfaceProvider)
                        }
                        // Prefer an exact concurrent pair from the provider.
                        val combo = p.availableConcurrentCameraInfos.firstOrNull { c ->
                            c.any { it.lensFacing == lensFacing } &&
                                c.any { it.lensFacing == secondaryFacing }
                        }
                        fun selectorFromInfo(info: androidx.camera.core.CameraInfo): CameraSelector {
                            val id = runCatching {
                                Camera2CameraInfo.from(info).cameraId
                            }.getOrNull()
                            return CameraSelector.Builder().addCameraFilter { infos ->
                                if (id == null) {
                                    infos.filter { it.lensFacing == info.lensFacing }.take(1)
                                } else {
                                    infos.filter {
                                        runCatching { Camera2CameraInfo.from(it).cameraId == id }
                                            .getOrDefault(false)
                                    }
                                }
                            }.build()
                        }
                        val primarySelector = if (combo != null) {
                            selectorFromInfo(combo.first { it.lensFacing == lensFacing })
                        } else selector
                        val secSelector = if (combo != null) {
                            selectorFromInfo(combo.first { it.lensFacing == secondaryFacing })
                        } else selectorFor(secondaryFacing, false)

                        fun tryConcurrent(primaryUseCases: List<androidx.camera.core.UseCase>): Camera? {
                            val primaryGroup = androidx.camera.core.UseCaseGroup.Builder()
                            primaryUseCases.forEach { primaryGroup.addUseCase(it) }
                            val primaryConfig = androidx.camera.core.ConcurrentCamera.SingleCameraConfig(
                                primarySelector,
                                primaryGroup.build(),
                                lifecycleOwner,
                            )
                            val secondaryConfig = androidx.camera.core.ConcurrentCamera.SingleCameraConfig(
                                secSelector,
                                androidx.camera.core.UseCaseGroup.Builder()
                                    .addUseCase(secPreview)
                                    .build(),
                                lifecycleOwner,
                            )
                            val concurrent = p.bindToLifecycle(listOf(primaryConfig, secondaryConfig))
                            return concurrent.cameras.firstOrNull {
                                it.cameraInfo.lensFacing == lensFacing
                            }
                        }

                        try {
                            // Full set first; some devices reject 3 use-cases on concurrent primary.
                            tryConcurrent(listOf(preview, imageCapture, videoCapture))
                                ?: tryConcurrent(listOf(preview, videoCapture))
                                ?: tryConcurrent(listOf(preview, imageCapture))
                        } catch (e: Exception) {
                            Log.w(TAG, "concurrent bind failed, falling back", e)
                            dualMode = false
                            dualSupported = false
                            p.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture, videoCapture)
                        }
                    } else {
                        p.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture, videoCapture)
                    }

                    if (cancelled) {
                        try { p.unbindAll() } catch (_: Exception) {}
                        return@addListener
                    }
                    boundCamera = cam
                    val zState = cam?.cameraInfo?.zoomState?.value
                    minZoom = zState?.minZoomRatio ?: 1f
                    maxZoom = zState?.maxZoomRatio ?: 1f
                    val zr = if (useUltraWide) 1f else zoomRatio.coerceIn(minZoom, maxZoom)
                    zoomRatio = zr
                    cam?.cameraControl?.setZoomRatio(zr)
                    imageCapture.flashMode = flashMode
                    val expState = cam?.cameraInfo?.exposureState
                    if (expState != null && expState.isExposureCompensationSupported) {
                        val range = expState.exposureCompensationRange
                        exposureMin = range.lower
                        exposureMax = range.upper
                        val idx = exposureIndex.coerceIn(range.lower, range.upper)
                        exposureIndex = idx
                        cam.cameraControl.setExposureCompensationIndex(idx)
                    } else {
                        exposureMin = 0
                        exposureMax = 0
                    }
                } catch (e: Exception) {
                    if (!cancelled) Log.e(TAG, "bind failed", e)
                }
            }, mainExecutor)

            onDispose {
                cancelled = true
                runCatching { CameraPrewarm.savePlaceholder(context, view.bitmap) }
                view.previewStreamState.removeObserver(streamObserver)
                try { boundPreview?.setSurfaceProvider(null) } catch (_: Exception) {}
                try { boundSecondaryPreview?.setSurfaceProvider(null) } catch (_: Exception) {}
                try { provider?.unbindAll() } catch (_: Exception) {}
                boundCamera = null
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activeRecording?.stop()
        }
    }

    fun mediaDir(): File {
        val dir = File(context.filesDir, "media")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun stamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())

    fun doTakePhoto() {
        if (captureBusy || isRecording || boundCamera == null) return
        captureBusy = true
        imageCapture.flashMode = flashMode
        scope.launch {
            shutterFlash.snapTo(0.55f)
            shutterFlash.animateTo(0f, tween(220))
        }
        val file = File(mediaDir(), "IMG_${stamp()}.jpg")
        val opts = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            opts,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    state.addMedia(path = file.absolutePath, isVideo = false, aspectRatio = 3f / 4f)
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        captureBusy = false
                        lastThumb = file.absolutePath
                        sessionCaptureCount++
                    }
                    CameraPrewarm.savePlaceholderFromFile(context, file.absolutePath)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "photo failed", exception)
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        captureBusy = false
                    }
                }
            },
        )
    }

    fun takePhoto() {
        if (captureBusy || isRecording || boundCamera == null || timerRemaining > 0) return
        if (timerSec <= 0) {
            doTakePhoto()
            return
        }
        scope.launch {
            for (i in timerSec downTo 1) {
                timerRemaining = i
                delay(1000)
            }
            timerRemaining = 0
            doTakePhoto()
        }
    }

    fun startVideo() {
        if (isRecording || captureBusy || boundCamera == null) return
        // Flip this the instant recording is requested, not when the async
        // VideoRecordEvent.Start callback lands. A quick long-press could
        // otherwise release *before* that callback fired — isRecording was
        // still false, so the release handler's "if (isRecording) stopVideo()"
        // never ran, and the recording kept going invisibly in the
        // background with no UI evidence anything was captured.
        isRecording = true
        val file = File(mediaDir(), "VID_${stamp()}.mp4")
        val opts = FileOutputOptions.Builder(file).build()
        try {
            activeRecording = videoCapture.output
                .prepareRecording(context, opts)
                .apply { if (hasAudio) withAudioEnabled() }
                .start(ContextCompat.getMainExecutor(context)) { event ->
                    when (event) {
                        is VideoRecordEvent.Finalize -> {
                            isRecording = false
                            activeRecording = null
                            if (!event.hasError()) {
                                state.addMedia(
                                    path = file.absolutePath,
                                    isVideo = true,
                                    aspectRatio = 3f / 4f,
                                    durationLabel = "видео",
                                )
                                lastThumb = file.absolutePath
                                sessionCaptureCount++
                                // Video thumb is still a useful open-placeholder.
                                CameraPrewarm.savePlaceholderFromFile(context, file.absolutePath)
                            } else {
                                Log.e(TAG, "video finalize error: ${event.error}", event.cause)
                                runCatching { file.delete() }
                            }
                        }
                        else -> Unit
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "start video failed", e)
            isRecording = false
            activeRecording = null
        }
    }

    fun stopVideo() {
        activeRecording?.stop()
        activeRecording = null
        isRecording = false
        recordingLocked = false
        lockArmed = false
    }

    fun flipCamera() {
        if (isRecording || captureBusy) return
        scope.launch {
            // Dip (never fully blank) → switch lens → bind fades back to full.
            previewAlpha.animateTo(LENS_FADE_MIN_ALPHA, tween(LENS_FADE_MS))
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            bindGeneration++
        }
    }

    fun setExposure(index: Int) {
        if (exposureMax <= exposureMin) return
        val idx = index.coerceIn(exposureMin, exposureMax)
        if (idx == exposureIndex) return
        exposureIndex = idx
        // Apply immediately — EV steps are few (−4…+4); lag came from
        // spamming the HAL on every drag pixel, not from instantaneous apply.
        runCatching {
            boundCamera?.cameraControl?.setExposureCompensationIndex(idx)
        }
    }

    fun cycleFlash() {
        flashMode = when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }
        imageCapture.flashMode = flashMode
        // Torch for continuous light when ON and not using capture flash only
        runCatching {
            boundCamera?.cameraControl?.enableTorch(flashMode == ImageCapture.FLASH_MODE_ON)
        }
    }

    fun cycleTimer() {
        timerSec = when (timerSec) {
            0 -> 5
            5 -> 10
            else -> 0
        }
    }

    fun setZoom(ratio: Float) {
        // 0.6× on most phones is a separate ultra-wide lens, not a zoom ratio.
        if (ratio < 0.95f && ultraWideAvailable) {
            if (!useUltraWide) {
                useUltraWide = true
                zoomRatio = 1f
                bindGeneration++
            }
            return
        }
        if (useUltraWide) {
            useUltraWide = false
            zoomRatio = 1f
            bindGeneration++
            return
        }
        val r = ratio.coerceIn(minZoom, maxZoom)
        zoomRatio = r
        runCatching { boundCamera?.cameraControl?.setZoomRatio(r) }
    }

    fun toggleDual() {
        if (!dualSupported) return // no concurrent pair on this device
        dualMode = !dualMode
        bindGeneration++
    }

    // Telegram-style layout: preview card sits BELOW the status bar with a
    // small gap (not flush against the top edge), mild 14dp rounding, and a
    // dedicated control bar underneath in the black area.
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                // Gap under the status bar so the card isn't glued to the top edge.
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(14.dp)),
        ) {
            // Telegram open sequence:
            //  1) Blurred last photo fills the plate (also during the slide-up,
            //     while readyToBind is still false — zero HAL work).
            //  2) Live PreviewView mounts after settle, alpha 0.
            //  3) StreamState.STREAMING → previewAlpha 0→1, blur fades out.
            val liveA = previewAlpha.value
            // Blurred still is visible for the whole open (and until live
            // fully fades in). Uses disk-cached last frame so it shows even
            // on the first composition of a fresh CameraCaptureScreen.
            if (placeholderUri != null && liveA < 0.995f) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(placeholderUri)
                        .size(360)
                        .crossfade(false)
                        .allowHardware(false)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(22.dp)
                        .graphicsLayer { alpha = (1f - liveA).coerceIn(0f, 1f) },
                )
                // Very light veil — enough to read as "preview", not a black plate.
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.06f * (1f - liveA))),
                )
            }
            if (hasCamera && readyToBind) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }.also { previewView = it }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = liveA },
                )
            }

            // Shutter flash — soft pulse, clipped to the frame so it reads
            // as "this is the shot" rather than a full-screen white blink.
            if (shutterFlash.value > 0.01f) {
                Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = shutterFlash.value)))
            }

            // Recording timer pill — red dot + running mm:ss, Telegram-style.
            // FadeScaleVisibility is a top-level helper so AnimatedVisibility
            // resolves to the non-scoped overload (calling it directly here
            // would pick ColumnScope.AnimatedVisibility from the outer Column
            // and fail to compile inside this Box).
            FadeScaleVisibility(
                visible = isRecording,
                enter = fadeIn(tween(140)) + scaleIn(initialScale = 0.85f, animationSpec = tween(140)),
                exit = fadeOut(tween(120)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
            ) {
                Row(
                    Modifier
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Box(Modifier.size(8.dp).background(Color.Red, CircleShape))
                    Text(
                        "%d:%02d".format(recordSeconds / 60, recordSeconds % 60),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
            }

            if (!hasCamera) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Нужен доступ к камере", color = Color.White, style = Letify.typography.titleMedium)
                        Spacer(Modifier.height(14.dp))
                        NoFeedbackButton(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                                )
                            },
                        ) {
                            Box(
                                Modifier
                                    .background(Color.White, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                            ) {
                                Text("Разрешить", color = Color.Black, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Close — top-left.
            NoFeedbackButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 10.dp)
                    .size(40.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    SolarIcon(name = "alt-arrow-left-outline", tint = Color.White, size = 20.dp)
                }
            }

            // Dual-camera PIP — small fixed circle, only when concurrent is real.
            if (dualMode && dualSupported) {
                val pipSize = 104.dp
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 56.dp, end = 12.dp)
                        .offset { IntOffset(pipOffset.x.toInt(), pipOffset.y.toInt()) }
                        .requiredSize(pipSize)
                        .clip(CircleShape)
                        .background(Color.Black, CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                pipOffset = Offset(
                                    pipOffset.x + drag.x,
                                    pipOffset.y + drag.y,
                                )
                            }
                        },
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            }.also { secondaryPreviewView = it }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                    )
                }
            }

            // Timer countdown
            if (timerRemaining > 0) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(88.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = timerRemaining.toString(),
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // ── Capture controls INSIDE the bottom of the preview frame ──
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 16.dp),
            ) {
                // Left: lock while recording, else thumbnail
                if (isRecording && !recordingLocked) {
                    val lockScale = if (lockArmed) 1.15f else 1f
                    NoFeedbackButton(
                        onClick = {
                            recordingLocked = true
                            lockArmed = false
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(52.dp)
                            .graphicsLayer { scaleX = lockScale; scaleY = lockScale },
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    if (lockArmed) Color.Black.copy(alpha = 0.45f)
                                    else Color.Black.copy(alpha = 0.35f),
                                    CircleShape,
                                )
                                .border(
                                    width = if (lockArmed) 2.dp else 1.dp,
                                    color = if (lockArmed) Color.White else Color.White.copy(alpha = 0.4f),
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("🔒", fontSize = 20.sp)
                        }
                    }
                } else if (!isRecording) {
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.35f)),
                    ) {
                        val thumb = lastThumb
                        if (thumb != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(thumb)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        if (sessionCaptureCount > 0) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-6).dp)
                                    .size(18.dp)
                                    .background(Color.White, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    sessionCaptureCount.toString(),
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // Shutter
                val shutterColor by animateColorAsState(
                    targetValue = when {
                        isRecording -> Color(0xFFE53935)
                        else -> Color.White
                    },
                    animationSpec = tween(220),
                    label = "shutterColor",
                )
                // Morph: big white circle → small red rounded square → back.
                val innerSize by animateDpAsState(
                    targetValue = if (isRecording) 28.dp else 58.dp,
                    animationSpec = tween(240),
                    label = "shutterSize",
                )
                val innerCorner by animateDpAsState(
                    targetValue = when {
                        isRecording && recordingLocked -> 6.dp
                        isRecording -> 8.dp
                        else -> 29.dp // half of 58 → full circle
                    },
                    animationSpec = tween(240),
                    label = "shutterCorner",
                )

                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(74.dp)
                        .border(3.dp, Color.White, CircleShape)
                        .pointerInput(isRecording, recordingLocked) {
                            detectTapGestures(
                                onTap = {
                                    when {
                                        isRecording && recordingLocked -> stopVideo()
                                        !isRecording -> takePhoto()
                                    }
                                },
                                onLongPress = {
                                    if (!isRecording) startVideo()
                                },
                                onPress = {
                                    if (isRecording && recordingLocked) return@detectTapGestures
                                    tryAwaitRelease()
                                    if (isRecording && !recordingLocked) {
                                        if (lockArmed) {
                                            recordingLocked = true
                                            lockArmed = false
                                        } else {
                                            stopVideo()
                                        }
                                    }
                                },
                            )
                        }
                        .pointerInput(isRecording, recordingLocked) {
                            if (!isRecording || recordingLocked) return@pointerInput
                            detectDragGestures(
                                onDragEnd = {
                                    if (lockArmed) {
                                        recordingLocked = true
                                        lockArmed = false
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    lockArmed = change.position.x < -36f
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(innerSize)
                            .background(shutterColor, RoundedCornerShape(innerCorner)),
                    )
                }

                // Flip — right
                if (!isRecording) {
                    NoFeedbackButton(
                        onClick = { flipCamera() },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(48.dp),
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            SolarIcon(name = "restart-bold", tint = Color.White, size = 22.dp)
                        }
                    }
                }
            }
        }

        // Horizontal icon-only tools row (no container).
        CameraToolsBar(
            flashMode = flashMode,
            timerSec = timerSec,
            ultraWideAvailable = ultraWideAvailable,
            useUltraWide = useUltraWide,
            showExposure = showExposure,
            exposureSupported = exposureMax > exposureMin,
            exposureIndex = exposureIndex,
            exposureMin = exposureMin,
            exposureMax = exposureMax,
            dualMode = dualMode,
            dualSupported = dualSupported,
            accent = state.accent,
            onFlash = { cycleFlash() },
            onTimer = { cycleTimer() },
            onZoom = {
                if (useUltraWide || !ultraWideAvailable) setZoom(1f) else setZoom(0.6f)
            },
            onExposureToggle = {
                showExposure = !showExposure
                if (!showExposure) setExposure(0)
            },
            onExposureChange = { setExposure(it) },
            onDual = { toggleDual() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 4.dp),
        )

        Text(
            if (isRecording) {
                if (recordingLocked) "Тап по кнопке — стоп"
                else "Отпусти или замок — продолжить"
            } else {
                "Тап — фото · Удерживай — видео"
            },
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 10.dp),
        )
    }
}

/**
 * Bottom tools: icon-only horizontal row, no container.
 * Brightness opens a tailed horizontal slider bubble above the sun icon.
 * Other tools cycle on each tap; active = accent tint.
 */
@Composable
private fun CameraToolsBar(
    flashMode: Int,
    timerSec: Int,
    ultraWideAvailable: Boolean,
    useUltraWide: Boolean,
    showExposure: Boolean,
    exposureSupported: Boolean,
    exposureIndex: Int,
    exposureMin: Int,
    exposureMax: Int,
    dualMode: Boolean,
    dualSupported: Boolean,
    accent: Color,
    onFlash: () -> Unit,
    onTimer: () -> Unit,
    onZoom: () -> Unit,
    onExposureToggle: () -> Unit,
    onExposureChange: (Int) -> Unit,
    onDual: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val flashActive = flashMode != ImageCapture.FLASH_MODE_OFF
    val timerActive = timerSec > 0
    val zoomActive = useUltraWide
    val dualActive = dualMode

    // Fixed-height row — the exposure bubble is drawn ABOVE the sun icon via
    // offset and never participates in layout (no jump when it opens).
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolIcon(
            icon = "fire-outline",
            active = flashActive,
            accent = accent,
            badge = when (flashMode) {
                ImageCapture.FLASH_MODE_ON -> "1"
                ImageCapture.FLASH_MODE_AUTO -> "A"
                else -> null
            },
            onClick = onFlash,
        )
        ToolIcon(
            icon = "stopwatch-bold-duotone",
            active = timerActive,
            accent = accent,
            badge = if (timerSec > 0) "$timerSec" else null,
            onClick = onTimer,
        )
        if (ultraWideAvailable) {
            ToolIcon(
                icon = "camera-bold-duotone",
                active = zoomActive,
                accent = accent,
                badge = if (useUltraWide) "0.6" else "1×",
                onClick = onZoom,
            )
        }
        // Always visible from the first frame (unlike before, when it waited
        // for camera bind to report EV support). Dimmed until supported.
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            ToolIcon(
                icon = "sun-bold",
                active = showExposure || exposureIndex != 0,
                accent = accent,
                dimmed = !exposureSupported,
                onClick = {
                    if (exposureSupported) onExposureToggle()
                },
            )
            if (showExposure && exposureSupported && exposureMax > exposureMin) {
                ExposureBubble(
                    index = exposureIndex,
                    min = exposureMin,
                    max = exposureMax,
                    accent = accent,
                    onChange = onExposureChange,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        // Float above the 44dp icon; does not affect row layout.
                        .offset(y = (-52).dp),
                )
            }
        }
        ToolIcon(
            icon = "user-circle-bold-duotone",
            active = dualActive,
            accent = accent,
            dimmed = !dualSupported,
            onClick = onDual, // always clickable; toggleDual no-ops if unsupported
        )
    }
}

@Composable
private fun ToolIcon(
    icon: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
    badge: String? = null,
    dimmed: Boolean = false,
) {
    NoFeedbackButton(onClick = onClick) {
        Box(
            Modifier.size(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            SolarIcon(
                name = icon,
                tint = when {
                    dimmed -> Color.White.copy(alpha = 0.28f)
                    active -> accent
                    else -> Color.White.copy(alpha = 0.92f)
                },
                size = 26.dp,
            )
            if (badge != null) {
                Text(
                    text = badge,
                    color = if (active) accent else Color.White.copy(alpha = 0.85f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp),
                )
            }
        }
    }
}

/**
 * Horizontal exposure slider in a dark bubble with a bottom tail.
 */
@Composable
private fun ExposureBubble(
    index: Int,
    min: Int,
    max: Int,
    accent: Color,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val range = (max - min).coerceAtLeast(1)
    var visualFrac by remember(min, max) {
        mutableFloatStateOf(((index - min).toFloat() / range).coerceIn(0f, 1f))
    }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(index, min, max) {
        if (!dragging) {
            visualFrac = ((index - min).toFloat() / range).coerceIn(0f, 1f)
        }
    }

    fun applyFrac(f: Float) {
        val clamped = f.coerceIn(0f, 1f)
        visualFrac = clamped
        onChange(min + kotlin.math.round(clamped * range).toInt())
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier
                .width(220.dp)
                .background(Color(0xEE1C1C1E), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SolarIcon(name = "sun-outline", tint = Color.White.copy(alpha = 0.55f), size = 16.dp)
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .height(28.dp)
                    .pointerInput(min, max) {
                        detectTapGestures { offset ->
                            applyFrac(offset.x / size.width.toFloat())
                        }
                    }
                    .pointerInput(min, max) {
                        detectDragGestures(
                            onDragStart = { dragging = true },
                            onDragEnd = { dragging = false },
                            onDragCancel = { dragging = false },
                            onDrag = { change, _ ->
                                change.consume()
                                applyFrac(change.position.x / size.width.toFloat())
                            },
                        )
                    },
            ) {
                val thumbR = 8.dp
                val travel = maxWidth - thumbR * 2
                // Track
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(999.dp)),
                )
                // Active fill
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth(visualFrac.coerceIn(0.02f, 1f))
                        .height(3.dp)
                        .background(accent, RoundedCornerShape(999.dp)),
                )
                // Thumb
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .size(thumbR * 2)
                        .graphicsLayer {
                            translationX = travel.toPx() * visualFrac
                        }
                        .background(Color.White, CircleShape),
                )
            }
            Text(
                text = if (index > 0) "+$index" else "$index",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(28.dp),
            )
        }
        // Tail pointing down toward the sun icon
        androidx.compose.foundation.Canvas(
            Modifier.size(width = 16.dp, height = 8.dp),
        ) {
            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(p, Color(0xEE1C1C1E))
        }
    }
}

/**
 * Thin wrapper around the top-level [AnimatedVisibility] so call sites nested
 * inside a [Column] (where [androidx.compose.foundation.layout.ColumnScope]
 * is an implicit receiver) don't accidentally resolve to the ColumnScope
 * extension overload — that overload can't be invoked from a Box child and
 * fails to compile ("cannot be called in this context with an implicit receiver").
 */
@Composable
private fun FadeScaleVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition,
    exit: ExitTransition,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit,
        content = content,
    )
}
