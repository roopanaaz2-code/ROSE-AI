package com.salman.roseai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.json.JSONArray
import java.util.Locale

private val Night = Color(0xFF090711)
private val Rose = Color(0xFFF06A9B)
private val Violet = Color(0xFF9B7BFF)
private val SurfaceRose = Color(0xFF1A1422)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var onRecognized: ((String) -> Unit)? = null
    private var onSpeechError: (() -> Unit)? = null
    private var pendingSpeechLanguage = "en-US"
    private var pendingWakeLanguage = "en-US"

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startSpeechRecognition(pendingSpeechLanguage) else onSpeechError?.invoke()
    }

    private val wakeMicPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startWakeWordService(pendingWakeLanguage)
    }

    private fun startWakeWordService(languageTag: String) {
        val intent = Intent(this, RoseWakeWordService::class.java).putExtra(RoseWakeWordService.EXTRA_LANGUAGE_TAG, languageTag)
        androidx.core.content.ContextCompat.startForegroundService(this, intent)
    }

    private fun stopWakeWordService() {
        stopService(Intent(this, RoseWakeWordService::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        }
        setContent {
            RoseApp(
                onListen = { languageTag, result, error ->
                    onRecognized = result
                    onSpeechError = error
                    pendingSpeechLanguage = languageTag
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        startSpeechRecognition(languageTag)
                    } else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                },
                onWakeToggle = { enabled, languageTag ->
                    pendingWakeLanguage = languageTag
                    if (enabled) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            startWakeWordService(languageTag)
                        } else wakeMicPermission.launch(Manifest.permission.RECORD_AUDIO)
                    } else stopWakeWordService()
                },
                onSpeak = { text, style, languageTag ->
                    val locale = Locale.forLanguageTag(languageTag)
                    val languageStatus = tts?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
                    if (languageStatus < TextToSpeech.LANG_AVAILABLE) tts?.setLanguage(Locale.getDefault())
                    val (pitch, rate) = when (style) {
                        "Cheerful" -> 1.15f to 1.08f
                        "Caring" -> 0.94f to 0.92f
                        "Playful" -> 1.22f to 1.12f
                        else -> 0.90f to 0.88f
                    }
                    tts?.setPitch(pitch)
                    tts?.setSpeechRate(rate)
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "rose_reply")
                }
            )
        }
    }

    private fun startSpeechRecognition(languageTag: String) {
        val recognizer = speechRecognizer ?: run {
            onSpeechError?.invoke()
            return
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { onSpeechError?.invoke() }
            override fun onResults(results: Bundle?) {
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (spoken.isNotBlank()) onRecognized?.invoke(spoken) else onSpeechError?.invoke()
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        recognizer.startListening(intent)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            // Prefer an installed female-labelled voice when the Android TTS engine exposes one.
            val femaleVoice = tts?.voices?.firstOrNull {
                it.locale.language == Locale.getDefault().language && it.name.contains("female", ignoreCase = true)
            }
            if (femaleVoice != null) tts?.voice = femaleVoice
        }
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun RoseApp(
    onListen: (String, (String) -> Unit, () -> Unit) -> Unit,
    onWakeToggle: (Boolean, String) -> Unit,
    onSpeak: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rose_settings", android.content.Context.MODE_PRIVATE) }
    val secureStore = remember { SecureKeyStore(context) }
    val localActions = remember { LocalActions(context) }
    val personas = listOf("Cheerful", "Caring", "Playful", "Calm")
    var persona by remember { mutableStateOf(prefs.getString("persona", "Calm") ?: "Calm") }
    var speechLanguage by remember { mutableStateOf(prefs.getString("speech_language", "en-US") ?: "en-US") }
    var wakeEnabled by remember { mutableStateOf(prefs.getBoolean("wake_enabled", false)) }
    var input by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Offline-ready • Tap to speak") }
    var showSettings by remember { mutableStateOf(false) }
    var showKey by remember { mutableStateOf(false) }
    var apiKeyDraft by remember { mutableStateOf(secureStore.read().orEmpty()) }
    val messages = remember {
        mutableStateListOf<String>().apply {
            val saved = try { JSONArray(prefs.getString("history", "[]")) } catch (_: Exception) { JSONArray() }
            for (i in 0 until saved.length()) add(saved.optString(i))
            if (isEmpty()) add("ROSE: Hello! I'm here. Try asking for the time, setting an alarm, opening an app, or chatting with me.")
        }
    }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    fun persist() {
        val array = JSONArray()
        messages.takeLast(100).forEach { array.put(it) }
        prefs.edit().putString("history", array.toString()).apply()
    }

    fun speakAndAdd(reply: String) {
        messages.add("ROSE: $reply")
        persist()
        onSpeak(reply, persona, speechLanguage)
    }

    fun handleMessage(raw: String) {
        val clean = raw.trim()
        if (clean.isEmpty() || busy) return
        messages.add("You: $clean")
        persist()
        input = ""
        val localReply = localActions.handle(clean)
        if (localReply != null) {
            speakAndAdd(localReply)
            status = "Offline action"
            return
        }
        val key = secureStore.read()
        if (!key.isNullOrBlank()) {
            // Snapshot Compose state on the UI thread before starting network work.
            val conversation = messages.takeLast(20).mapNotNull { line ->
                when {
                    line.startsWith("You: ") -> "user" to line.removePrefix("You: ")
                    line.startsWith("ROSE: ") -> "assistant" to line.removePrefix("ROSE: ")
                    else -> null
                }
            }
            busy = true
            status = "ROSE is thinking online…"
            Thread {
                val answer = try { OnlineAssistant.reply(key, conversation) }
                catch (_: Exception) { "I couldn't reach the online service. Check your internet connection and API settings." }
                mainHandler.post {
                    busy = false
                    status = "Online reply received • Your key stays on this device"
                    speakAndAdd(answer)
                }
            }.start()
        } else {
            val fallback = when {
                clean.contains("hello", true) || clean.contains("hi", true) ->
                    "Hello! I'm ROSE. My offline phone commands are ready. Add your own API key in Settings to enable online AI conversations."
                else ->
                    "I'm in offline mode. I can tell the time and date, calculate simple sums, open installed apps and Android settings, and prepare an alarm or dialler screen. Add an online API key in Settings for open-ended AI conversations."
            }
            status = "Offline mode • No API key configured"
            speakAndAdd(fallback)
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Rose, secondary = Violet, background = Night, surface = SurfaceRose)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Night) {
            Column(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("ROSE", color = Rose, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        Text("YOUR PERSONAL AI COMPANION", color = Color.LightGray, fontSize = 9.sp, letterSpacing = 2.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("v0.2", color = Color.Gray, fontSize = 11.sp)
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Rose)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.size(144.dp).background(
                        Brush.radialGradient(listOf(Rose.copy(alpha = .34f), Violet.copy(alpha = .12f), Night)), CircleShape
                    ), contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(98.dp).background(Brush.linearGradient(listOf(Rose, Violet)), CircleShape), contentAlignment = Alignment.Center) {
                        Text("🌹", fontSize = 44.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(status, color = Color(0xFFE9C8D8), fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Text("SPEECH LANGUAGE", color = Color.LightGray, fontSize = 10.sp, letterSpacing = 1.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("English" to "en-US", "اردو" to "ur-PK", "پښتو" to "ps-AF").forEach { (label, tag) ->
                        FilterChip(selected = speechLanguage == tag, onClick = {
                            speechLanguage = tag
                            prefs.edit().putString("speech_language", tag).apply()
                        }, label = { Text(label, fontSize = 11.sp) })
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Wake word: Hey ROSE", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Keeps listening with a visible foreground notification", color = Color.Gray, fontSize = 9.sp)
                    }
                    Switch(checked = wakeEnabled, onCheckedChange = { enabled ->
                        if (enabled && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            // Ask in-app for microphone permission; start the service only after approval.
                            wakeEnabled = false
                            prefs.edit().putBoolean("wake_enabled", false).apply()
                            onWakeToggle(false, speechLanguage)
                            // The normal microphone button also requests permission; the dedicated switch is retried after grant.
                            status = "Tap the microphone once to grant permission, then enable wake word mode."
                        } else {
                            wakeEnabled = enabled
                            prefs.edit().putBoolean("wake_enabled", enabled).apply()
                            onWakeToggle(enabled, speechLanguage)
                            status = if (enabled) "Wake word service requested • Say Hey ROSE" else "Wake word listening stopped"
                        }
                    })
                }
                Spacer(Modifier.height(8.dp))
                Spacer(Modifier.height(12.dp))
                Text("VOICE PERSONALITY", color = Color.LightGray, fontSize = 10.sp, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    personas.forEach { name ->
                        FilterChip(
                            selected = persona == name,
                            onClick = { persona = name; prefs.edit().putString("persona", name).apply() },
                            label = { Text(name, fontSize = 10.sp) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("CONVERSATION", Modifier.align(Alignment.Start), color = Color.LightGray, fontSize = 10.sp, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(6.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().background(SurfaceRose, RoundedCornerShape(18.dp)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { msg ->
                        Text(msg, color = if (msg.startsWith("ROSE:")) Rose else Color.White, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f),
                        placeholder = { Text("Talk to ROSE…") }, shape = RoundedCornerShape(18.dp), singleLine = true,
                        enabled = !busy
                    )
                    IconButton(onClick = { handleMessage(input) }, enabled = !busy) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Rose)
                    }
                    IconButton(onClick = {
                        listening = true
                        status = "Listening…"
                        onListen(speechLanguage, { spoken ->
                            listening = false
                            status = "Voice captured"
                            handleMessage(spoken)
                        }, {
                            listening = false
                            status = "Couldn't hear that. Check microphone permission and try again."
                        })
                    }, enabled = !busy) {
                        Icon(Icons.Default.Mic, contentDescription = "Speak", tint = if (listening) Violet else Rose)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Push-to-talk + optional wake-word listening • Chat history stays on this phone", color = Color.Gray, fontSize = 9.sp)
            }
        }

        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text("ROSE settings") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Mode: Offline commands work without an AI key. Online chat uses your own OpenRouter API key and free model routing.")
                        Text(if (secureStore.read().isNullOrBlank()) "Online AI: Not configured" else "Online AI: API key saved securely")
                        Text("Speech recognition and spoken voice are provided by Android. English, Urdu and Pashto availability depends on your device speech and TTS language packs.")
                        Text("Wake word mode uses a foreground microphone service with a persistent notification. Start it while ROSE is open, and stop it with the switch. It is not a hardware low-power hotword engine.")
                        Text("Camera hand/eye tracking is not yet enabled. ROSE supports explicit voice navigation and ordered command chains; camera controls will be added only with a tested on-device vision model.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { apiKeyDraft = secureStore.read().orEmpty(); showKey = true; showSettings = false }) {
                        Text("Configure online AI")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { messages.clear(); messages.add("ROSE: Chat cleared."); persist(); showSettings = false }) {
                        Text("Clear chat")
                    }
                }
            )
        }

        if (showKey) {
            AlertDialog(
                onDismissRequest = { showKey = false },
                title = { Text("Online AI connection") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Paste your OpenRouter API key (starts with sk-or-). It is encrypted with Android Keystore and is not included in the APK.")
                        OutlinedTextField(
                            value = apiKeyDraft, onValueChange = { apiKeyDraft = it },
                            label = { Text("API key") }, singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                        Text("Online requests use OpenRouter free-model routing. Free models have daily and per-minute limits; model availability can vary.", color = Color.Gray, fontSize = 11.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        secureStore.save(apiKeyDraft.trim())
                        showKey = false
                        status = if (apiKeyDraft.isBlank()) "Offline mode • API key removed" else "Online AI key saved securely"
                    }) { Text("Save key") }
                },
                dismissButton = {
                    TextButton(onClick = { secureStore.save(""); apiKeyDraft = ""; showKey = false; status = "Online AI key removed" }) {
                        Text("Remove key")
                    }
                }
            )
        }
    }
}
