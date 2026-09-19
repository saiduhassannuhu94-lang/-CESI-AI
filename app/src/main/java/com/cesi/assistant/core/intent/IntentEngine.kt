package com.cesi.assistant.core.intent

class IntentEngine {
    fun understand(text: String): AssistantIntent {
        val c = text.lowercase().trim()
        return when {
            c.contains("turn off flashlight") || c.contains("turn off torch") ||
            c.contains("switch off flashlight") || c.contains("switch off torch") ||
            c.contains("kashe flashlight") || c.contains("kashe torch") ||
            c.contains("kashe haske") -> AssistantIntent.FlashlightOff

            c.contains("turn on flashlight") || c.contains("turn on torch") ||
            c.contains("switch on flashlight") || c.contains("switch on torch") ||
            c == "flashlight" || c == "torch" ||
            c.contains("kunna haske") || c.contains("kunna torch") -> AssistantIntent.FlashlightOn

            c.contains("selfie") || c.contains("hoton kaina") -> AssistantIntent.Selfie
            c == "camera" || c.contains("open camera") ||
            c.contains("take photo") || c.contains("take a picture") ||
            c.contains("kamara") -> AssistantIntent.Camera

            c.contains("where am i") || c.contains("my location") ||
            c.contains("show my location") || c.contains("ina nake") ||
            c == "location" -> AssistantIntent.Location

            c.startsWith("call ") || c.startsWith("kira ") ->
                AssistantIntent.Call(c.removePrefix("call ").removePrefix("kira ").trim())

            c.startsWith("find contact ") || c.startsWith("search contact ") ||
            c.startsWith("nemo contact ") || c.startsWith("nemo lambar ") ->
                AssistantIntent.ContactSearch(c.substringAfter(" ").substringAfter(" ").trim())

            c.startsWith("open ") || c.startsWith("bude ") || c.startsWith("launch ") ->
                AssistantIntent.AppLaunch(c.substringAfter(" ").trim())

            c.contains("volume up") || c.contains("increase volume") ||
            c.contains("kara sauti") -> AssistantIntent.VolumeUp

            c.contains("volume down") || c.contains("decrease volume") ||
            c.contains("rage sauti") -> AssistantIntent.VolumeDown

            c == "mute" || c.contains("yi shiru") -> AssistantIntent.Mute

            c.contains("battery") || c.contains("nawa battery") ->
                AssistantIntent.BatteryStatus

            c.startsWith("search google for ") || c.startsWith("google ") ||
            c.startsWith("search ") || c.startsWith("bincika ") ->
                AssistantIntent.WebSearch(
                    c.replaceFirst(
                        Regex("^(search google for|google|search|bincika)\\s+"),
                        ""
                    )
                )

            else -> AssistantIntent.Unknown(c)
        }
    }
}
