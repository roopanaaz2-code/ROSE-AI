package com.salman.roseai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mediapipe.framework.image.MediaImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer

/**
 * Opt-in front-camera interaction. MediaPipe models run on-device; frames are never uploaded.
 * Hand gestures trigger an allow-listed set of navigation actions. Eye gaze is an approximate
 * iris-landmark estimate and should not be treated as calibrated eye-tracking hardware.
 */
@Composable
fun GestureCameraPanel(onAction: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraEnabled by remember { mutableStateOf(false) }
    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var status by remember { mutableStateOf("Camera is off. Start it to enable on-device hand and eye tracking.") }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
        cameraEnabled = granted
        status = if (granted) "Camera active • MediaPipe processing stays on this phone" else "Camera permission denied. Gesture mode remains off."
    }
    val controller = remember(context) {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            setImageAnalysisOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        }
    }
    val executor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val currentOnAction by rememberUpdatedState(onAction)
    val analyzer = remember(context) {
        RoseMediaPipeAnalyzer(
            context = context,
            onGesture = { gesture, action ->
                mainHandler.post {
                    status = "$gesture detected • ${action.replace('_', ' ')}"
                    currentOnAction(action)
                }
            },
            onGaze = { gaze -> mainHandler.post { status = "Eye direction: $gaze" } }
        )
    }
    DisposableEffect(analyzer) {
        onDispose { analyzer.close() }
    }

    LaunchedEffect(cameraEnabled) {
        if (cameraEnabled && cameraGranted) {
            try {
                controller.setImageAnalysisAnalyzer(executor, analyzer)
                controller.bindToLifecycle(lifecycleOwner)
                status = "Front camera active • show a clear hand gesture and face"
            } catch (_: Exception) {
                cameraEnabled = false
                status = "Unable to start the front camera on this device."
            }
        } else {
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
            executor.shutdown()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ROSE Hand & Eye Mode") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Hand gestures: open palm = scroll down, pointing up = scroll up, closed fist = back, victory = home, thumbs up/down = swipe right/left, I-love-you = recent apps.")
                if (cameraEnabled) {
                    AndroidView(
                        factory = { ctx -> PreviewView(ctx).also { it.controller = controller } },
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        update = { it.controller = controller }
                    )
                }
                Text(status)
                Text("Eye direction is estimated from iris landmarks and may need good lighting, a clear face view, and calibration. It is not medical-grade tracking.")
                Text("Camera frames are processed locally and are not sent to ROSE's online AI. Enable Accessibility below to execute navigation gestures.")
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { currentOnAction("open accessibility settings") }) { Text("Accessibility") }
                if (cameraEnabled) {
                    OutlinedButton(onClick = { cameraEnabled = false; status = "Camera stopped." }) { Text("Stop camera") }
                } else {
                    Button(onClick = {
                        if (cameraGranted) cameraEnabled = true
                        else permissionLauncher.launch(Manifest.permission.CAMERA)
                    }) { Text("Start camera") }
                }
                Button(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

private class RoseMediaPipeAnalyzer(
    context: Context,
    private val onGesture: (String, String) -> Unit,
    private val onGaze: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private val gestureRecognizer = GestureRecognizer.createFromOptions(
        context,
        GestureRecognizer.GestureRecognizerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath("gesture_recognizer.task").build())
            .setRunningMode(RunningMode.VIDEO)
            .setNumHands(2)
            .build()
    )
    private val faceLandmarker = FaceLandmarker.createFromOptions(
        context,
        FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath("face_landmarker.task").build())
            .setRunningMode(RunningMode.VIDEO)
            .setNumFaces(1)
            .build()
    )
    private val gestureActions = mapOf(
        "Open_Palm" to "scroll_down",
        "Pointing_Up" to "scroll_up",
        "Closed_Fist" to "back",
        "Victory" to "home",
        "Thumb_Up" to "swipe_right",
        "Thumb_Down" to "swipe_left",
        "ILoveYou" to "recents"
    )
    private var previousGesture = "None"
    private var lastGestureAt = 0L
    private var previousGaze = ""

    fun close() {
        gestureRecognizer.close()
        faceLandmarker.close()
    }

    override fun analyze(proxy: androidx.camera.core.ImageProxy) {
        val mediaImage = proxy.image
        if (mediaImage == null) {
            proxy.close()
            return
        }
        try {
            val image = MediaImageBuilder(mediaImage).build()
            val processing = ImageProcessingOptions.builder()
                .setRotationDegrees(proxy.imageInfo.rotationDegrees)
                .build()
            val timestamp = SystemClock.uptimeMillis()

            val gestureResult = gestureRecognizer.recognizeForVideo(image, processing, timestamp)
            val top = gestureResult.gestures().firstOrNull()?.firstOrNull()
            val gestureName = top?.categoryName().orEmpty().ifBlank { "None" }
            val confidence = top?.score() ?: 0f
            val action = gestureActions[gestureName]
            val now = SystemClock.uptimeMillis()
            if (action != null && confidence >= 0.72f && gestureName != previousGesture && now - lastGestureAt > 900L) {
                previousGesture = gestureName
                lastGestureAt = now
                onGesture(gestureName.replace('_', ' '), action)
            } else if (gestureName == "None") {
                previousGesture = "None"
            }

            val faceResult = faceLandmarker.detectForVideo(image, processing, timestamp)
            val face = faceResult.faceLandmarks().firstOrNull()
            val gaze = estimateGaze(face) ?: "face not detected"
            if (gaze != previousGaze) {
                previousGaze = gaze
                onGaze(gaze)
            }
        } catch (_: Exception) {
            // Skip a bad frame without interrupting camera streaming.
        } finally {
            proxy.close()
        }
    }

    private fun estimateGaze(points: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>?): String? {
        if (points == null || points.size < 478) return null

        fun value(index: Int, axis: String): Float =
            if (axis == "x") points[index].x() else points[index].y()

        fun ratio(center: Float, edgeA: Float, edgeB: Float): Float? {
            val low = minOf(edgeA, edgeB)
            val high = maxOf(edgeA, edgeB)
            if (high - low < 0.001f) return null
            return ((center - low) / (high - low)).coerceIn(0f, 1f)
        }

        val rightX = ratio(value(468, "x"), value(33, "x"), value(133, "x"))
        val leftX = ratio(value(473, "x"), value(362, "x"), value(263, "x"))
        val rightY = ratio(value(468, "y"), value(159, "y"), value(145, "y"))
        val leftY = ratio(value(473, "y"), value(386, "y"), value(374, "y"))
        val x = listOfNotNull(rightX, leftX).takeIf { it.size == 2 }?.average()?.toFloat() ?: return null
        val y = listOfNotNull(rightY, leftY).takeIf { it.size == 2 }?.average()?.toFloat() ?: return null
        val horizontal = when {
            x < 0.34f -> "left"
            x > 0.66f -> "right"
            else -> "center"
        }
        val vertical = when {
            y < 0.34f -> "up"
            y > 0.66f -> "down"
            else -> "center"
        }
        return when {
            horizontal == "center" && vertical == "center" -> "center"
            vertical == "center" -> horizontal
            horizontal == "center" -> vertical
            else -> "$vertical and $horizontal"
        }
    }
}
