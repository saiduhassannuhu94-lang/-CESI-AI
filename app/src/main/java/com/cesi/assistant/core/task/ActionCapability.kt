package com.cesi.assistant.core.task

/**
 * Capabilities CESI can compose into a task.
 * This is intentionally capability-based instead of a fixed command list.
 */
enum class ActionCapability {
    COMMUNICATION,
    TELEPHONY,
    USSD,
    MESSAGING,
    APP_CONTROL,
    WEB_SEARCH,
    MEDIA,
    DEVICE_SETTINGS,
    ALARM,
    LOCATION,
    CONTACTS
}
