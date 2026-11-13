package com.salman.roseai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Explicit, user-initiated Android actions. Consequential actions open system UI for review. */
class LocalActions(private val context: Context) {
    fun handle(raw: String): String? {
        // Strip the spoken wake phrase even when the user taps the mic manually.
        val text = raw.trim()
            .replace(Regex("(?i)^\\s*(hey|hi)\\s+rose[,! ]*"), "")
            .trim()
        val lower = text.lowercase(Locale.getDefault())

        if (lower in listOf("what time is it", "time", "tell me the time", "current time")) {
            return "It's " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) + "."
        }
        if (lower in listOf("what is the date", "today's date", "date", "what day is it")) {
            return "Today is " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date()) + "."
        }
        if (lower.contains("open accessibility settings")) {
            return launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), "Opening Accessibility settings. Enable ROSE only if you want voice navigation commands.")
        }
        if (lower.contains("open settings") || lower == "settings") {
            return launch(Intent(Settings.ACTION_SETTINGS), "Opening Android Settings.")
        }
        if (lower.contains("open wifi") || lower.contains("wi-fi settings")) {
            return launch(Intent(Settings.ACTION_WIFI_SETTINGS), "Opening Wi-Fi settings.")
        }

        val navigation = when {
            lower in listOf("go back", "press back", "back") -> "back"
            lower in listOf("go home", "home screen", "show home screen") -> "home"
            lower in listOf("recent apps", "show recent apps", "open recent apps") -> "recents"
            lower in listOf("scroll down", "swipe up") -> "scroll_down"
            lower in listOf("scroll up", "swipe down") -> "scroll_up"
            else -> null
        }
        if (navigation != null) {
            if (RoseAccessibilityService.performUserAction(navigation)) {
                return when (navigation) {
                    "back" -> "Going back."
                    "home" -> "Going to the Home screen."
                    "recents" -> "Opening recent apps."
                    "scroll_down" -> "Scrolling down."
                    else -> "Scrolling up."
                }
            }
            return "To use phone navigation, first open Accessibility settings and enable ROSE AI. This permission is optional and can be turned off anytime."
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
            val requested = lower.removePrefix("open ")
                .substringBefore(" and ")
                .substringBefore(" then ")
                .removeSuffix(" please")
                .removeSuffix(" for me")
                .trim()
            val launchables = context.packageManager.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
            )
            val match = launchables.firstOrNull {
                it.loadLabel(context.packageManager).toString().equals(requested, ignoreCase = true)
            } ?: launchables.firstOrNull {
                it.loadLabel(context.packageManager).toString().contains(requested, ignoreCase = true)
            }
            val launchIntent = match?.activityInfo?.let {
                context.packageManager.getLaunchIntentForPackage(it.packageName)
            }
            if (launchIntent != null) return launch(launchIntent, "Opening $requested.")
            return "I couldn't find an installed app named $requested."
        }
        return null
    }

    private fun launch(intent: Intent, success: String): String {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            success
        } catch (_: Exception) {
            "Android couldn't open that action on this device."
        }
    }
}
