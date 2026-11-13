package com.salman.roseai

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.Executors
import android.os.Handler
import android.os.Looper

/**
 * Camera gesture mode is opt-in. Frames are analyzed on-device and are never uploaded.
 * Wrist movement can request vertical scrolling; eye-open classification is shown as
 * an experimental blink signal, not as precise gaze tracking.
 */
@Composable
fun GestureCameraPanel(onAction: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraEnabled by remember { mutableStateOf(false) }
    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var status by remember { mutableStateOf("Camera is off. Start it to enable local gesture detection.") }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
        cameraEnabled = granted
        status = if (granted) "Camera active • processing stays on this phone" else "Camera permission denied. Gesture mode remains off."
    }
    val controller = remember(context) {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val analyzer = remember(onAction) {
        RoseCameraAnalyzer(
            onScroll = { direction ->
                mainHandler.post {
                    status = if (direction == "scroll_down") "Hand swipe detected • scrolling down" else "Hand swipe detected • scrolling up"
                    onAction(direction)
                }
            },
            onEye = { mainHandler.post { status = "Blink signal detected • eye direction is not calibrated" } }
        )
    }

    LaunchedEffect(cameraEnabled) {
        if (cameraEnabled && cameraGranted) {
            try {
                controller.setImageAnalysisAnalyzer(executor, analyzer)
                controller.bindToLifecycle(lifecycleOwner)
                status = "Front camera active • raise and move a wrist to scroll"
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
        title = { Text("ROSE Gesture Mode") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Optional front-camera mode. Wrist movement can scroll up or down. Blink detection is experimental; directional eye tracking is not available yet.")
                if (cameraEnabled) {
                    AndroidView(
                        factory = { ctx -> PreviewView(ctx).also { it.controller = controller } },
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        update = { it.controller = controller }
                    )
                }
                Text(status)
                Text("Camera frames are processed locally and are not sent to the AI service.")
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (cameraEnabled) {
                    OutlinedButton(onClick = { cameraEnabled = false; status = "Camera stopped." }) { Text("Stop camera") }
                } else {
                    Button(onClick = {
                        if (cameraGranted) {
                            cameraEnabled = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }) { Text("Start camera") }
                }
                Button(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

private class RoseCameraAnalyzer(
    private val onScroll: (String) -> Unit,
    private val onEye: () -> Unit
) : ImageAnalysis.Analyzer {
    private val poseDetector = PoseDetection.getClient(
        PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build()
    )
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )
    private var previousWristY: Float? = null
    private var previousEyeOpen = true
    private var lastGestureAt = 0L
    private var lastBlinkAt = 0L

    override fun analyze(proxy: androidx.camera.core.ImageProxy) {
        val mediaImage = proxy.image
        if (mediaImage == null) {
            proxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
        val poseTask = poseDetector.process(image)
        val faceTask = faceDetector.process(image)
        com.google.android.gms.tasks.Tasks.whenAllComplete(poseTask, faceTask).addOnCompleteListener {
            try {
                if (poseTask.isSuccessful) {
                    val pose = poseTask.result
                    val left = pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.LEFT_WRIST)
                    val right = pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_WRIST)
                    val wristY = listOfNotNull(left, right).map { it.position.y }.takeIf { it.isNotEmpty() }?.average()?.toFloat()
                    if (wristY != null) {
                        val old = previousWristY
                        val now = System.currentTimeMillis()
                        if (old != null && now - lastGestureAt > 1100L) {
                            val delta = wristY - old
                            if (delta > 125f) {
                                lastGestureAt = now
                                onScroll("scroll_up")
                            } else if (delta < -125f) {
                                lastGestureAt = now
                                onScroll("scroll_down")
                            }
                        }
                        previousWristY = wristY
                    }
                }
                if (faceTask.isSuccessful) {
                    val face = faceTask.result.firstOrNull()
                    val leftEye = face?.leftEyeOpenProbability
                    val rightEye = face?.rightEyeOpenProbability
                    if (leftEye != null && rightEye != null) {
                        val open = leftEye > 0.65f && rightEye > 0.65f
                        val closed = leftEye < 0.25f && rightEye < 0.25f
                        val now = System.currentTimeMillis()
                        if (previousEyeOpen && closed && now - lastBlinkAt > 1400L) {
                            lastBlinkAt = now
                            onEye()
                        }
                        if (open) previousEyeOpen = true
                        else if (closed) previousEyeOpen = false
                    }
                }
            } catch (_: Exception) {
                // Ignore an individual frame; camera analysis must continue safely.
            } finally {
                proxy.close()
            }
        }
    }
}
