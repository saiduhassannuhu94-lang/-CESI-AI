package com.cesi.assistant.core.intent

class IntentEngine {

    fun understand(text: String): AssistantIntent {

        val command = text
            .lowercase()
            .trim()

        return when {

            command.contains("flashlight") ||
            command.contains("torch") ||
            command.contains("haske") &&
            !command.contains("kashe") -> {

                AssistantIntent.FlashlightOn
            }

            command.contains("turn off flashlight") ||
            command.contains("turn off torch") ||
            command.contains("kashe flashlight") ||
            command.contains("kashe torch") ||
            command.contains("kashe haske") -> {

                AssistantIntent.FlashlightOff
            }

            command.contains("selfie") ||
            command.contains("hoton kaina") -> {

                AssistantIntent.Selfie
            }

            command.contains("camera") ||
            command.contains("take photo") ||
            command.contains("take a picture") ||
            command.contains("kamara") -> {

                AssistantIntent.Camera
            }

            command.contains("location") ||
            command.contains("where am i") ||
            command.contains("ina nake") -> {

                AssistantIntent.Location
            }

            command.startsWith("call ") ||
            command.startsWith("kira ") -> {

                val target = command
                    .removePrefix("call ")
                    .removePrefix("kira ")
                    .trim()

                AssistantIntent.Call(target)
            }

            else -> {

                AssistantIntent.Unknown(command)
            }
        }
    }
}
