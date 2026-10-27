package ge.cleaninghouse.quest.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import ge.cleaninghouse.quest.data.AppState
import ge.cleaninghouse.quest.ui.Analysis
import ge.cleaninghouse.quest.ui.AppViewModel
import ge.cleaninghouse.quest.ui.LocalKa
import ge.cleaninghouse.quest.ui.Screen
import ge.cleaninghouse.quest.ui.components.GradientButton
import ge.cleaninghouse.quest.ui.theme.Coral
import ge.cleaninghouse.quest.ui.theme.Muted
import ge.cleaninghouse.quest.ui.theme.Purple
import ge.cleaninghouse.quest.ui.tr
import kotlinx.coroutines.delay
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private enum class Mode { VIDEO, PHOTO }

private const val MAX_VIDEO_SECONDS = 60

@Composable
fun CaptureScreen(state: AppState, vm: AppViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ka = LocalKa.current

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
        vm.captureDir.listFiles()?.forEach { it.delete() }
    }

    var mode by remember { mutableStateOf(Mode.VIDEO) }
    var video by remember { mutableStateOf<File?>(null) }
    val photos = remember { mutableStateListOf<File>() }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var seconds by remember { mutableIntStateOf(0) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var count by remember { mutableStateOf<Int?>(null) }
    var hint by remember { mutableStateOf("") }
    var flash by remember { mutableStateOf(false) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }
    val recorder = remember {
        Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.SD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
            .build()
    }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(hasPermission, mode) {
        if (!hasPermission) return@LaunchedEffect
        try {
            val p = provider ?: suspendCoroutine { cont ->
                val future = ProcessCameraProvider.getInstance(context)
                future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(context))
            }
            provider = p
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            p.unbindAll()
            p.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                if (mode == Mode.VIDEO) videoCapture else imageCapture,
            )
            cameraError = null
        } catch (e: Exception) {
            cameraError = e.message ?: e.toString()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            recording?.stop()
            provider?.unbindAll()
        }
    }
    LaunchedEffect(recording) {
        if (recording != null) {
            delay(MAX_VIDEO_SECONDS * 1000L)
            recording?.stop()
        }
    }
    LaunchedEffect(flash) {
        if (flash) {
            delay(120)
            flash = false
        }
    }

    fun startRecording() {
        val file = File(vm.captureDir, "video_${System.currentTimeMillis()}.mp4")
        seconds = 0
        recording = recorder
            .prepareRecording(context, FileOutputOptions.Builder(file).build())
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Status ->
                        seconds = (event.recordingStats.recordedDurationNanos / 1_000_000_000L).toInt()
                    is VideoRecordEvent.Finalize -> {
                        recording = null
                        if (file.exists() && file.length() > 0) {
                            video?.takeIf { it != file }?.delete()
                            video = file
                        } else {
                            cameraError = "Recording failed (${event.error})"
                        }
                    }
                    else -> Unit
                }
            }
    }

    fun takePhoto() {
        val file = File(vm.captureDir, "photo_${System.currentTimeMillis()}.jpg")
        flash = true
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    photos.add(file)
                }

                override fun onError(exception: ImageCaptureException) {
                    cameraError = exception.message
                }
            },
        )
    }

    val analysis = vm.analysis
    BackHandler {
        when {
            analysis is Analysis.Running -> vm.cancelAnalysis()
            recording != null -> recording?.stop()
            else -> vm.screen = Screen.Home
        }
    }

    val hasMedia = video != null || photos.isNotEmpty()
    val noKey = state.settings.apiKey.isBlank()

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasPermission) {
            AndroidView({ previewView }, Modifier.fillMaxSize())
        } else {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("📷", fontSize = 48.sp)
                Text(
                    tr("Camera permission is needed to scan the room.", "ოთახის დასასკანერებლად კამერის ნებართვაა საჭირო."),
                    color = Color.White, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text(tr("Allow camera", "კამერის დაშვება")) }
            }
        }
        if (flash) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.6f)))

        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            // Top bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIcon("✕") { if (recording == null) vm.screen = Screen.Home }
                Spacer(Modifier.weight(1f))
                if (recording == null) {
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(4.dp)
                    ) {
                        ModeTab(tr("🎥 Video", "🎥 ვიდეო"), mode == Mode.VIDEO) { mode = Mode.VIDEO }
                        ModeTab(tr("📷 Photo", "📷 ფოტო"), mode == Mode.PHOTO) { mode = Mode.PHOTO }
                    }
                } else {
                    val blink = rememberInfiniteTransition(label = "rec")
                    val a by blink.animateFloat(1f, 0.2f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "recA")
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(Color.Red.copy(alpha = a)))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "%d:%02d / %d:%02d".format(seconds / 60, seconds % 60, MAX_VIDEO_SECONDS / 60, MAX_VIDEO_SECONDS % 60),
                            color = Color.White, fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(44.dp))
            }

            if (recording != null) {
                Text(
                    tr("Walk slowly around the room 🚶", "ნელა შემოიარე ოთახი 🚶"),
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color.White,
                )
            }

            Spacer(Modifier.weight(1f))

            // Bottom panel
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(26.dp),
                color = Color.White.copy(alpha = 0.95f),
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                when {
                                    video != null && photos.isNotEmpty() -> tr("🎬 Video + ${photos.size} photos", "🎬 ვიდეო + ${photos.size} ფოტო")
                                    video != null -> tr("🎬 Video ready (${seconds}s)", "🎬 ვიდეო მზადაა (${seconds}წმ)")
                                    photos.isNotEmpty() -> tr("📸 ${photos.size} photos", "📸 ${photos.size} ფოტო")
                                    mode == Mode.VIDEO -> tr("Record the room", "ჩაწერე ოთახი")
                                    else -> tr("Snap a few photos", "გადაიღე რამდენიმე ფოტო")
                                },
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                if (mode == Mode.VIDEO) tr("Up to 60 s, in-app only", "მაქს. 60 წმ, პირდაპირ აპში")
                                else tr("Tap to capture each spot", "თითოეული კუთხე გადაიღე"),
                                color = Muted, fontSize = 12.sp,
                            )
                        }
                        ShutterButton(mode, recording != null) {
                            when {
                                mode == Mode.PHOTO -> takePhoto()
                                recording != null -> recording?.stop()
                                else -> startRecording()
                            }
                        }
                    }

                    if (hasMedia && recording == null) {
                        Spacer(Modifier.height(12.dp))
                        Text(tr("How many quests?", "რამდენი ქვესტი?"), fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf<Int?>(null, 5, 10, 20, 50).forEach { n ->
                                FilterChip(
                                    selected = count == n,
                                    onClick = { count = n },
                                    label = { Text(n?.toString() ?: tr("🤖 Auto", "🤖 ავტო")) },
                                )
                            }
                        }
                        OutlinedTextField(
                            hint, { hint = it },
                            Modifier.fillMaxWidth(),
                            label = { Text(tr("Note for AI (optional)", "შენიშვნა AI-სთვის (არასავალდებულო)")) },
                            placeholder = { Text(tr("e.g. bedroom, focus on laundry", "მაგ. საძინებელი, სარეცხზე გაამახვილე")) },
                            singleLine = true,
                        )
                        Spacer(Modifier.height(10.dp))
                        GradientButton(
                            if (noKey) tr("🎲 Generate demo quests", "🎲 დემო ქვესტების შექმნა")
                            else tr("✨ Analyze with AI", "✨ AI ანალიზი"),
                            Modifier.fillMaxWidth(),
                        ) { vm.analyze(video, photos.toList(), count, hint, demo = noKey) }
                        if (noKey) {
                            Text(
                                tr(
                                    "No Gemini API key yet — add it in Settings for real video analysis.",
                                    "Gemini API გასაღები არ არის — ვიდეოს რეალური ანალიზისთვის დაამატე პარამეტრებში.",
                                ),
                                color = Coral, fontSize = 12.sp, textAlign = TextAlign.Center,
                            )
                        }
                        TextButton(onClick = {
                            video?.delete(); video = null
                            photos.forEach { it.delete() }; photos.clear()
                        }) { Text(tr("Start over", "თავიდან")) }
                    }
                }
            }
        }

        if (analysis is Analysis.Running) AnalyzingOverlay(ka) { vm.cancelAnalysis() }
    }

    cameraError?.let { msg ->
        AlertDialog(
            onDismissRequest = { cameraError = null },
            title = { Text(tr("Camera problem", "კამერის პრობლემა")) },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { cameraError = null }) { Text("OK") } },
        )
    }
    if (analysis is Analysis.Failed) {
        AlertDialog(
            onDismissRequest = { vm.dismissAnalysisError() },
            title = { Text(tr("Analysis failed", "ანალიზი ვერ მოხერხდა")) },
            text = { Text(analysis.message) },
            confirmButton = { TextButton(onClick = { vm.dismissAnalysisError() }) { Text("OK") } },
            dismissButton = {
                TextButton(onClick = {
                    vm.dismissAnalysisError()
                    vm.analyze(video, photos.toList(), count, hint, demo = true)
                }) { Text(tr("Use demo quests", "დემო ქვესტები")) }
            },
        )
    }
}

@Composable
private fun RoundIcon(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Color.White, fontSize = 18.sp) }
}

@Composable
private fun ModeTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        Modifier
            .clip(CircleShape)
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        color = if (selected) Purple else Color.White,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun ShutterButton(mode: Mode, recording: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(72.dp)
            .clip(CircleShape)
            .border(4.dp, if (mode == Mode.VIDEO) Color(0xFFFF3B30) else Purple, CircleShape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(if (recording) 26.dp else 52.dp)
                .clip(if (recording) RoundedCornerShape(6.dp) else CircleShape)
                .background(if (mode == Mode.VIDEO) Color(0xFFFF3B30) else Purple)
        )
    }
}

@Composable
private fun AnalyzingOverlay(ka: Boolean, onCancel: () -> Unit) {
    val t = rememberInfiniteTransition(label = "analyze")
    val angle by t.animateFloat(-20f, 20f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "broom")
    val tips = if (ka) listOf(
        "AI ათვალიერებს ოთახს… 🔍",
        "ვეძებთ ჭუჭყიან ჭურჭელს… 🍽️",
        "ვითვლით გასარეცხ ტანსაცმელს… 👕",
        "ვამზადებთ ქვესტებს… ⚔️",
    ) else listOf(
        "The AI is looking around… 🔍",
        "Spotting dirty dishes… 🍽️",
        "Counting laundry… 👕",
        "Forging your quests… ⚔️",
    )
    var tip by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1800)
            tip = (tip + 1) % tips.size
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xE6140F2E))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🧹", fontSize = 72.sp, modifier = Modifier.rotate(angle))
            Spacer(Modifier.height(16.dp))
            Text(tips[tip], color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onCancel) { Text(if (ka) "გაუქმება" else "Cancel", color = Color.White) }
        }
    }
}
