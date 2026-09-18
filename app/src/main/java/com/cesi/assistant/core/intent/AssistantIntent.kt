package com.cesi.assistant.core.intent

sealed class AssistantIntent {

    data object FlashlightOn : AssistantIntent()
    data object FlashlightOff : AssistantIntent()

    data object Selfie : AssistantIntent()
    data object Camera : AssistantIntent()

    data object Location : AssistantIntent()

    data class Call(val target: String) : AssistantIntent()

    data class Unknown(val text: String) : AssistantIntent()
}
