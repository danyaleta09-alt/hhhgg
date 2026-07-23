package com.letify.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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

@Composable
fun CameraCaptureScreen(
    onBack: () -> Unit,
    onCaptured: () -> Unit = {},
    // Live bind gate. Host keeps this true while the sheet is visible;
    // drops it only AFTER a freeze-frame has been captured so the user
    // never sees the live surface die mid-slide.
    readyToBind: Boolean = true,
    // Host flips this true at the start of close. We snapshot
    // PreviewView.bitmap → frozen ImageBitmap; the sheet then slides
    // away with that static picture while the HAL is released.
    freezeFrame: Boolean = false,
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
    // Last live frame, frozen at close-start so the sheet can slide away
    // without a live Surface/TextureView (and without a black flash).
    var frozenBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isRecording by remember { mutableStateOf(false) }
    var captureBusy by remember { mutableStateOf(false) }
    val shutterFlash = remember { Animatable(0f) }
    var lastThumb by remember { mutableStateOf<String?>(null) }
    var sessionCaptureCount by remember { mutableIntStateOf(0) }
    var exposureBias by remember { mutableFloatStateOf(0f) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var recordSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordSeconds = 0
            while (true) {
                delay(1000)
                recordSeconds++
            }
        }
    }

    val imageCapture = CameraPrewarm.imageCapture
    val videoCapture = CameraPrewarm.videoCapture
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    val cameraExecutor = CameraPrewarm.executor
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var bindGeneration by remember { mutableIntStateOf(0) }

    // ── Freeze-frame on close ────────────────────────────────────────
    // Snapshot the last painted frame while the TextureView is still live.
    // Host waits ~2 frames after flipping freezeFrame, then drops
    // readyToBind (unbind) — the static Image stays and rides the slide-out.
    LaunchedEffect(freezeFrame) {
        if (freezeFrame) {
            // Stop any in-flight recording so finalize doesn't race unbind.
            runCatching { activeRecording?.stop() }
            activeRecording = null
            isRecording = false
            val view = previewView
            val src: Bitmap? = runCatching { view?.bitmap }.getOrNull()
            if (src != null && !src.isRecycled && src.width > 1 && src.height > 1) {
                val config = src.config ?: Bitmap.Config.ARGB_8888
                val copy = runCatching { src.copy(config, false) }.getOrNull()
                if (copy != null) {
                    frozenBitmap = copy.asImageBitmap()
                    // Fully opaque — freeze replaces the live fade pipeline.
                    previewAlpha.snapTo(1f)
                }
            }
        } else {
            frozenBitmap = null
        }
    }

    // Bind as soon as the PreviewView exists. readyToBind stays true for the
    // whole open+close slide so the live TextureView rides translationY —
    // picture never snaps to black. Fade-in waits for StreamState.STREAMING
    // (first real frame), not bindToLifecycle return — that was the "резко
    // появилась картинка" pop: alpha hit 1 before any frame was on screen.
    DisposableEffect(readyToBind, lensFacing, previewView, lifecycleOwner, bindGeneration, hasCamera) {
        val view = previewView
        if (!readyToBind || view == null || !hasCamera) {
            onDispose { }
        } else {
            val mainExecutor = ContextCompat.getMainExecutor(context)
            val future = CameraPrewarm.future(context)
            var provider: ProcessCameraProvider? = null
            var boundPreview: Preview? = null
            var cancelled = false
            // Reveal only when the first camera frame is actually painting.
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
                    val preview = Preview.Builder().build().also { p ->
                        p.setSurfaceProvider(view.surfaceProvider)
                    }
                    boundPreview = preview
                    val selector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()
                    provider?.unbindAll()
                    if (cancelled) return@addListener
                    val cam = provider?.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        imageCapture,
                        videoCapture,
                    )
                    if (cancelled) {
                        try { provider?.unbindAll() } catch (_: Exception) {}
                        return@addListener
                    }
                    boundCamera = cam
                    cam?.cameraControl?.setZoomRatio(
                        zoomRatio.coerceIn(
                            cam.cameraInfo.zoomState.value?.minZoomRatio ?: 1f,
                            cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f,
                        ),
                    )
                    val expState = cam?.cameraInfo?.exposureState
                    if (expState != null && expState.isExposureCompensationSupported) {
                        val range = expState.exposureCompensationRange
                        val idx = (exposureBias * range.upper.coerceAtLeast(1)).toInt()
                            .coerceIn(range.lower, range.upper)
                        cam.cameraControl.setExposureCompensationIndex(idx)
                    }
                    // Do NOT fade here — wait for streamObserver / STREAMING.
                } catch (e: Exception) {
                    if (!cancelled) Log.e(TAG, "bind failed", e)
                }
            }, mainExecutor)

            onDispose {
                cancelled = true
                view.previewStreamState.removeObserver(streamObserver)
                try { boundPreview?.setSurfaceProvider(null) } catch (_: Exception) {}
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

    fun takePhoto() {
        if (captureBusy || isRecording || boundCamera == null) return
        captureBusy = true
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
            // Display priority:
            //  1. Frozen last frame (close in progress) — static, zero HAL cost
            //  2. Live TextureView while readyToBind — rides open slide, fades
            //     in on StreamState.STREAMING
            val frozen = frozenBitmap
            if (frozen != null) {
                Image(
                    bitmap = frozen,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (hasCamera && readyToBind) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            // TextureView: correctly transformed by parent
                            // graphicsLayer during the open slide.
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }.also { previewView = it }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = previewAlpha.value },
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

            // Close — overlaid on the preview, top-left.
            // Status-bar inset is already applied on the parent Column.
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
        }

        // Control bar — lives in the black area below the preview, not on
        // top of the live feed.
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(top = 18.dp, bottom = 16.dp),
        ) {
            // Last shot thumbnail (bottom-left) — stays on camera after capture
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .size(48.dp),
            ) {
                FadeScaleVisibility(
                    visible = lastThumb != null,
                    enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.7f, animationSpec = tween(180)),
                    exit = fadeOut(),
                ) {
                    Box {
                        val path = lastThumb
                        if (path != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(Uri.fromFile(File(path)))
                                    .size(160)
                                    // Crossfade between successive thumbnails so a new
                                    // shot eases in over the old one instead of
                                    // popping in abruptly once decoded.
                                    .crossfade(220)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
                            )
                        }
                        // Shot counter — how many photos/videos taken this session.
                        if (sessionCaptureCount > 0) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-6).dp)
                                    .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                                    .padding(horizontal = 5.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (sessionCaptureCount > 99) "99+" else sessionCaptureCount.toString(),
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp,
                                )
                            }
                        }
                    }
                }
            }

            // Shutter — outer ring stays put, inner shape morphs circle → rounded
            // square while recording, plus a light press-in for tactile feedback.
            run {
                var pressed by remember { mutableStateOf(false) }
                val pressScale by animateFloatAsState(
                    if (pressed) 0.92f else 1f,
                    label = "shutterPress",
                )
                val innerSize by animateDpAsState(
                    if (isRecording) 30.dp else 58.dp,
                    label = "shutterInnerSize",
                )
                val innerColor by animateColorAsState(
                    if (isRecording) Color.Red else Color.White,
                    label = "shutterInnerColor",
                )
                val innerShape = if (isRecording) RoundedCornerShape(9.dp) else CircleShape

                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(76.dp)
                        .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
                        .border(3.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                        .pointerInput(boundCamera) {
                            detectTapGestures(
                                onTap = { takePhoto() },
                                onLongPress = { startVideo() },
                                onPress = {
                                    pressed = true
                                    tryAwaitRelease()
                                    pressed = false
                                    if (isRecording) stopVideo()
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(innerSize).background(innerColor, innerShape))
                }
            }

            // Flip camera — bottom-right
            NoFeedbackButton(
                onClick = { flipCamera() },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(48.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    SolarIcon(name = "restart-bold", tint = Color.White, size = 22.dp)
                }
            }
        }

        // Hint
        Text(
            if (isRecording) "Отпусти, чтобы остановить"
            else "Тап — фото · Удерживай — видео",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 10.dp),
        )
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
