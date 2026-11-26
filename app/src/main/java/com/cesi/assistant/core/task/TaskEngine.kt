package com.cesi.assistant.core.task

import android.content.Context

/**
 * Compatibility facade for older callers.
 * All requests are delegated to ContextTaskEngine so confirmation, cancellation,
 * plan disclosure, and result verification cannot be bypassed through this legacy
 * entry point.
 */
class TaskEngine(context: Context) {
    private val delegate = ContextTaskEngine(context)

    fun execute(input: String): String = delegate.execute(input)
}
