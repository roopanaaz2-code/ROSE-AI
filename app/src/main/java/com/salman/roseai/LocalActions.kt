package com.salman.roseai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Explicit, user-initiated Android actions. Anything consequential opens a system UI for review. */
class LocalActions(private val context: Context) {
    fun handle(raw: String): String? {
        val text = raw.trim()
        val lower = text.lowercase(Locale.getDefault())

        if (lower in listOf("what time is it", "time", "tell me the time", "current time")) {
            return "It's " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) + "."
        }
        if (lower in listOf("what is the date", "today's date", "date", "what day is it")) {
            return "Today is " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date()) + "."
        }
        if (lower.contains("open settings") || lower == "settings") {
            return launch(Intent(Settings.ACTION_SETTINGS), "Opening Android Settings.")
        }
        if (lower.contains("open wifi") || lower.contains("wi-fi settings")) {
            return launch(Intent(Settings.ACTION_WIFI_SETTINGS), "Opening Wi-Fi settings.")
        }

        val alarm = Regex("""(?:set|create) (?:an? )?alarm(?: for)? (\d{1,2})(?::(\d{2}))?\s*(am|pm)?""", RegexOption.IGNORE_CASE)
            .find(text)
        if (alarm != null) {
            var hour = alarm.groupValues[1].toIntOrNull() ?: return "I couldn't understand that alarm time."
            val minute = alarm.groupValues[2].toIntOrNull() ?: 0
            val suffix = alarm.groupValues[3].lowercase(Locale.ROOT)
            if (minute !in 0..59 || hour !in 1..12) return "Please say an alarm time like 7:30 AM."
            if (suffix == "pm" && hour < 12) hour += 12
            if (suffix == "am" && hour == 12) hour = 0
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, "ROSE reminder")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            }
            return launch(intent, "Opening your clock app to confirm the alarm.")
        }

        if (lower.startsWith("call ")) {
            val number = text.substringAfter("call ").trim()
            if (number.isBlank()) return "Tell me the phone number to dial."
            return launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))),
                "Opening the dialler. Review the number and tap call yourself.")
        }
        if (lower.startsWith("text ") || lower.startsWith("send sms to ")) {
            return "For safety, I won't send messages automatically. Open your messaging app and review the recipient and message before sending."
        }

        val math = Regex("""^(?:calculate\s+)?(-?\d+(?:\.\d+)?)\s*([+*/-])\s*(-?\d+(?:\.\d+)?)$""")
            .matchEntire(lower)
        if (math != null) {
            val a = math.groupValues[1].toDouble()
            val b = math.groupValues[3].toDouble()
            val answer = when (math.groupValues[2]) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> if (b == 0.0) return "You can't divide by zero." else a / b
                else -> return null
            }
            return "The answer is " + answer.toString()
        }

        if (lower.startsWith("open ")) {
            val requested = lower.removePrefix("open ").trim()
            val launchIntent = context.packageManager.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
            ).firstOrNull {
                it.loadLabel(context.packageManager).toString().lowercase(Locale.getDefault()).contains(requested)
            }?.activityInfo?.let {
                context.packageManager.getLaunchIntentForPackage(it.packageName)
            }
            if (launchIntent != null) return launch(launchIntent, "Opening $requested.")
            return "I couldn't find an installed app named $requested."
        }
        return null
    }

    private fun launch(intent: Intent, success: String): String {
        return try {
            if (intent.resolveActivity(context.packageManager) == null) "I couldn't find an app to handle that action."
            else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                success
            }
        } catch (_: Exception) {
            "Android couldn't open that action on this device."
        }
    }
}
