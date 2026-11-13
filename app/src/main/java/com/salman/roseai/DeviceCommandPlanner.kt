package com.salman.roseai

/**
 * Small deterministic command planner for offline device control.
 * Only allow-listed actions are emitted; arbitrary AI text is never executed as code.
 */
object DeviceCommandPlanner {
    data class Step(val command: String)

    fun plan(utterance: String): List<Step> {
        val parts = utterance.trim()
            .split(Regex("\\s+(?:and then|then)\\s+", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (parts.isEmpty()) return emptyList()
        return parts.take(5).map { Step(it) }
    }
}
