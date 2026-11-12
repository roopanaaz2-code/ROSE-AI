package com.salman.roseai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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

    private val micPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startSpeechRecognition()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        }
        setContent {
            RoseApp(
                onListen = { result, error ->
                    onRecognized = result
                    onSpeechError = error
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {
                        startSpeechRecognition()
                    } else {
                        micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onSpeak = { text -> tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "rose_reply") }
            )
        }
    }

    private fun startSpeechRecognition() {
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
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty()
                if (text.isNotBlank()) onRecognized?.invoke(text) else onSpeechError?.invoke()
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        recognizer.startListening(intent)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault()
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun RoseApp(onListen: ((String) -> Unit, () -> Unit) -> Unit, onSpeak: (String) -> Unit) {
    val personas = listOf("Cheerful", "Caring", "Playful", "Calm")
    var persona by remember { mutableStateOf(personas.last()) }
    var input by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Ready when you are") }
    val messages = remember { mutableStateListOf("Hello! I'm ROSE. Tap the microphone or type a message to begin.") }

    fun handleMessage(raw: String) {
        val clean = raw.trim()
        if (clean.isEmpty()) return
        messages.add("You: $clean")
        input = ""
        val reply = when {
            clean.contains("hello", true) || clean.contains("hi", true) ->
                "Hey there! I'm ROSE. It's lovely to hear from you."
            clean.contains("time", true) ->
                "I can help with time-related tasks once the device action module is connected."
            else ->
                "I heard you say: $clean. My local voice and conversation foundation is ready. Connect an AI provider later for richer answers."
        }
        messages.add("ROSE: $reply")
        onSpeak(reply)
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Rose, secondary = Violet, background = Night, surface = SurfaceRose
    )) {
        Surface(modifier = Modifier.fillMaxSize(), color = Night) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("ROSE", color = Rose, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        Text("YOUR PERSONAL AI COMPANION", color = Color.LightGray, fontSize = 9.sp, letterSpacing = 2.sp)
                    }
                    Text("v0.1", color = Color.Gray)
                }
                Spacer(Modifier.height(22.dp))
                Box(
                    modifier = Modifier.size(176.dp)
                        .background(Brush.radialGradient(listOf(Rose.copy(alpha = .34f), Violet.copy(alpha = .12f), Night)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(112.dp).background(
                        Brush.linearGradient(listOf(Rose, Violet)), CircleShape
                    ), contentAlignment = Alignment.Center) {
                        Text("🌹", fontSize = 48.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(status, color = Color(0xFFE9C8D8), fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                Text("VOICE PERSONALITY", color = Color.LightGray, fontSize = 10.sp, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    personas.forEach { name ->
                        FilterChip(
                            selected = persona == name,
                            onClick = { persona = name },
                            label = { Text(name, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("CONVERSATION", modifier = Modifier.align(Alignment.Start),
                    color = Color.LightGray, fontSize = 10.sp, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .background(SurfaceRose, RoundedCornerShape(18.dp)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { msg ->
                        Text(msg, color = if (msg.startsWith("ROSE:") || msg.startsWith("Hello")) Rose else Color.White,
                            fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input, onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Talk to ROSE…") },
                        shape = RoundedCornerShape(18.dp),
                        singleLine = true
                    )
                    IconButton(onClick = { handleMessage(input) }) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Rose)
                    }
                    IconButton(onClick = {
                        listening = true
                        status = "Listening…"
                        onListen({ spoken ->
                            listening = false
                            status = "Ready when you are"
                            handleMessage(spoken)
                        }, {
                            listening = false
                            status = "Couldn't hear that. Try again."
                        })
                    }) {
                        Icon(Icons.Default.Mic, contentDescription = "Speak", tint = if (listening) Violet else Rose)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Microphone is used only when you tap the mic.", color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}
