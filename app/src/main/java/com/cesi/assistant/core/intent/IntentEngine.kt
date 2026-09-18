package com.cesi.assistant.core.intent

class IntentEngine {

    fun understand(text: String): AssistantIntent {
        val command = text.lowercase().trim()

        return when {
            // FLASHLIGHT OFF — check FIRST
            command.contains("turn off flashlight") ||
            command.contains("turn off torch") ||
            command.contains("switch off flashlight") ||
            command.contains("switch off torch") ||
            command.contains("kashe flashlight") ||
            command.contains("kashe torch") ||
            command.contains("kashe haske") -> {
                AssistantIntent.FlashlightOff
            }

            // FLASHLIGHT ON
            command.contains("turn on flashlight") ||
            command.contains("turn on torch") ||
            command.contains("switch on flashlight") ||
            command.contains("switch on torch") ||
            command.contains("flashlight") ||
            command.contains("torch") ||
            (command.contains("haske") && !command.contains("kashe")) -> {
                AssistantIntent.FlashlightOn
            }

            // SELFIE
            command.contains("selfie") ||
            command.contains("hoton kaina") -> {
                AssistantIntent.Selfie
            }

            // CAMERA
            command.contains("camera") ||
            command.contains("take photo") ||
            command.contains("take a picture") ||
            command.contains("kamara") -> {
                AssistantIntent.Camera
            }

            // LOCATION
            command.contains("location") ||
            command.contains("where am i") ||
            command.contains("ina nake") -> {
                AssistantIntent.Location
            }

            // CALL
            command.startsWith("call ") ||
            command.startsWith("kira ") -> {
                val target = command
                    .removePrefix("call ")
                    .removePrefix("kira ")
                    .trim()

                AssistantIntent.Call(target)
            }

            // UNKNOWN
            else -> {
                AssistantIntent.Unknown(command)
            }
        }
    }
}
