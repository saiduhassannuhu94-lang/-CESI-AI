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
            c.contains("kunna flashlight") || c.contains("kunna haske") ||
            c.contains("kunna torch") -> AssistantIntent.FlashlightOn

            c.contains("selfie") || c.contains("hoton kaina") ->
                AssistantIntent.Selfie

            c == "camera" || c.contains("open camera") ||
            c.contains("take photo") || c.contains("take a photo") ||
            c.contains("take a picture") || c.contains("kamara") ->
                AssistantIntent.Camera

            c.contains("where am i") || c.contains("my location") ||
            c.contains("show my location") || c.contains("ina nake") ||
            c.contains("ina nake yanzu") || c == "location" ->
                AssistantIntent.Location

            c.startsWith("call ") || c.startsWith("kira ") ->
                AssistantIntent.Call(extractAfterPrefix(c))

            c.startsWith("find contact ") || c.startsWith("search contact ") ||
            c.startsWith("find number ") || c.startsWith("search number ") ||
            c.startsWith("nemo contact ") || c.startsWith("nemo lambar ") ->
                AssistantIntent.ContactSearch(extractAfterPrefix(c))

            c.startsWith("open ") || c.startsWith("bude ") ||
            c.startsWith("buɗe ") || c.startsWith("launch ") ||
            c.startsWith("start ") || c.startsWith("run ") ->
                AssistantIntent.AppLaunch(extractAfterPrefix(c))

            c.contains("volume up") || c.contains("increase volume") ||
            c.contains("turn volume up") || c.contains("kara sauti") ->
                AssistantIntent.VolumeUp

            c.contains("volume down") || c.contains("decrease volume") ||
            c.contains("turn volume down") || c.contains("rage sauti") ->
                AssistantIntent.VolumeDown

            c == "mute" || c.contains("mute phone") || c.contains("yi shiru") ->
                AssistantIntent.Mute

            c.contains("battery") || c.contains("nawa battery") ||
            c.contains("battery percentage") ->
                AssistantIntent.BatteryStatus

            c.startsWith("search google for ") ||
            c.startsWith("search google ") ||
            c.startsWith("google search for ") ||
            c.startsWith("google search ") ||
            c.startsWith("search for ") ||
            c.startsWith("search ") ||
            c.startsWith("google ") ||
            c.startsWith("bincika a google ") ||
            c.startsWith("bincika ") ||
            c.startsWith("nemo a google ") ->
                AssistantIntent.WebSearch(extractSearchQuery(c))

            else -> AssistantIntent.Unknown(c)
        }
    }

    private fun extractAfterPrefix(text: String): String =
        text.replaceFirst(
            Regex("^(call|kira|find contact|search contact|find number|search number|nemo contact|nemo lambar|open|bude|buɗe|launch|start|run)\\s+"),
            ""
        ).trim()

    private fun extractSearchQuery(text: String): String =
        text.replaceFirst(
            Regex("^(search google for|search google|google search for|google search|search for|search|google|bincika a google|bincika|nemo a google)\\s+"),
            ""
        ).trim()
}
