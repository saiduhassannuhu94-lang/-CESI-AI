package com.cesi.assistant.core.task

/**
 * Platform-neutral plan for one user request.
 *
 * The action order is preserved exactly as produced by the planner. Android
 * execution remains outside this model.
 */
data class TaskPlan(
    val actions: List<PlannedAction>,
    val overallRisk: TaskRisk,
    val requiresConfirmation: Boolean
) {
    val isEmpty: Boolean
        get() = actions.isEmpty()
}

object TaskPlanBuilder {
    fun from(actions: List<PlannedAction>): TaskPlan {
        val overallRisk = actions.maxRisk()
        return TaskPlan(
            actions = actions,
            overallRisk = overallRisk,
            requiresConfirmation = actions.any { it.requiresConfirmation }
        )
    }

    private fun List<PlannedAction>.maxRisk(): TaskRisk =
        maxByOrNull { it.risk.priority }?.risk ?: TaskRisk.NONE
}

private val TaskRisk.priority: Int
    get() = when (this) {
        TaskRisk.NONE -> 0
        TaskRisk.LOW -> 1
        TaskRisk.EXTERNAL_SIDE_EFFECT -> 2
        TaskRisk.HIGH_IMPACT -> 3
    }
