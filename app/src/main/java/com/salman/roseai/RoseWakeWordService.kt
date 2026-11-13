package com.salman.roseai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.util.Locale

/**
 * User-started foreground microphone service.
 * It listens for a spoken ROSE wake phrase and then handles allow-listed local commands.
 * This is software speech recognition, not a low-power DSP/hardware hotword detector.
 */
class RoseWakeWordService : Service(), TextToSpeech.OnInitListener {
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var languageTag = "en-US"
    private var awaitingCommand = false
    private var stopped = false

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopListening()
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }
        languageTag = intent?.getStringExtra(EXTRA_LANGUAGE_TAG) ?: languageTag
        stopped = false
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak("Speech recognition is not available on this phone.")
            main.postDelayed({ stopSelf() }, 1800)
            return START_NOT_STICKY
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(listener)
        }
        startListening()
        return START_NOT_STICKY
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: android.os.Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onPartialResults(partialResults: android.os.Bundle?) = Unit
        override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit

        override fun onResults(results: android.os.Bundle?) {
            val phrase = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            if (phrase.isNotBlank()) handlePhrase(phrase)
            scheduleRestart(if (awaitingCommand) 900L else 550L)
        }

        override fun onError(error: Int) {
            if (!stopped) scheduleRestart(1200L)
        }
    }

    private fun startListening() {
        if (stopped) return
        val r = recognizer ?: return
        try {
            val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            r.startListening(request)
        } catch (_: Exception) {
            scheduleRestart(1800L)
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        if (stopped) return
        main.removeCallbacks(restartTask)
        main.postDelayed(restartTask, delayMs)
    }

    private val restartTask = Runnable { startListening() }

    private fun handlePhrase(raw: String) {
        val normalized = raw.lowercase(Locale.ROOT)
            .replace(Regex("[،,.!?؟؛:]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (awaitingCommand) {
            awaitingCommand = false
            executeCommand(raw)
            return
        }

        val triggers = listOf(
            "hey rose", "hello rose", "okay rose", "ok rose",
            "اے روز", "ہیلو روز", "روز سنو",
            "اې روز", "سلام روز"
        )
        val trigger = triggers.firstOrNull { normalized.contains(it) } ?: return
        val remainder = normalized.substringAfter(trigger).trim()
        if (remainder.isNotBlank()) {
            executeCommand(remainder)
        } else {
            awaitingCommand = true
            speak("I'm listening.")
        }
    }

    private fun executeCommand(command: String) {
        val reply = try {
            LocalActions(this).handle(command)
        } catch (_: Exception) {
            null
        }
        if (reply != null) {
            speak(reply)
        } else {
            speak("I can do supported phone commands here. Open ROSE Assistant for a full AI conversation.")
        }
    }

    private fun speak(message: String) {
        val engine = tts ?: return
        val locale = Locale.forLanguageTag(languageTag)
        val result = engine.setLanguage(locale)
        if (result < TextToSpeech.LANG_AVAILABLE) engine.setLanguage(Locale.getDefault())
        engine.speak(message, TextToSpeech.QUEUE_FLUSH, null, "rose_wake_reply")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setLanguage(Locale.forLanguageTag(languageTag))
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "ROSE wake-word listening", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Visible notification while ROSE listens for its wake phrase"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): android.app.Notification {
        val openIntent = PendingIntent.getActivity(
            this, 10, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
        )
        val stopIntent = PendingIntent.getService(
            this, 11, Intent(this, RoseWakeWordService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("ROSE Assistant is listening")
            .setContentText("Say “Hey ROSE” • Tap Stop to turn off microphone listening")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_media_pause, "Stop listening", stopIntent)
            .build()
    }

    private fun immutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    private fun stopListening() {
        stopped = true
        main.removeCallbacks(restartTask)
        try { recognizer?.cancel() } catch (_: Exception) { }
        try { recognizer?.destroy() } catch (_: Exception) { }
        recognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE)
        else @Suppress("DEPRECATION") stopForeground(true)
    }

    override fun onDestroy() {
        stopListening()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_LANGUAGE_TAG = "rose_language_tag"
        const val ACTION_STOP = "com.salman.roseai.STOP_WAKE_LISTENING"
        private const val CHANNEL_ID = "rose_wake_listener"
        private const val NOTIFICATION_ID = 804
    }
}
