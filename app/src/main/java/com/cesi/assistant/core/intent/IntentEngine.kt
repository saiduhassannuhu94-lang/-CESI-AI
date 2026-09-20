package com.cesi.assistant.core.intent

class IntentEngine {
    fun understand(input: String): AssistantIntent {
        val command = input.trim().lowercase().replace(Regex("\\s+"), " ")
        if (command.isBlank()) return AssistantIntent.Unknown

        return when {
            command.contains("turn on flashlight") || command.contains("switch on flashlight") ||
            command.contains("turn on torch") || command.contains("switch on torch") ||
            command == "flashlight" || command == "torch" ||
            command.contains("kunna haske") || command.contains("kunna torch") -> AssistantIntent.FlashlightOn

            command.contains("turn off flashlight") || command.contains("switch off flashlight") ||
            command.contains("turn off torch") || command.contains("switch off torch") ||
            command.contains("kashe flashlight") || command.contains("kashe torch") ||
            command.contains("kashe haske") -> AssistantIntent.FlashlightOff

            command.contains("take a selfie") || command.contains("take selfie") ||
            command.contains("selfie") || command.contains("hoton kaina") ||
            command.contains("ɗauki hotona") || command.contains("dauki hotona") -> AssistantIntent.Selfie

            command == "camera" || command.contains("open camera") ||
            command.contains("bude camera") || command.contains("buɗe camera") ||
            command.contains("take a photo") || command.contains("take a picture") ||
            command.contains("kamara") || command.contains("buɗe kamara") -> AssistantIntent.Camera

            command.contains("where am i") || command.contains("my location") ||
            command.contains("show my location") || command.contains("ina nake") ||
            command.contains("ina nake yanzu") -> AssistantIntent.Location

            command.startsWith("call ") || command.startsWith("kira ") ||
            command.startsWith("kira wa ") -> AssistantIntent.Call(
                extractAfterPrefix(command,"call ","kira wa ","kira ")
            )

            command.startsWith("find contact ") || command.startsWith("search contact ") ||
            command.startsWith("nemo contact ") || command.startsWith("nemo lambar ") ->
                AssistantIntent.ContactSearch(extractAfterPrefix(command,"find contact ","search contact ","nemo contact ","nemo lambar "))

            command.startsWith("open ") || command.startsWith("bude ") || command.startsWith("buɗe ") ||
            command.startsWith("launch ") || command.startsWith("start ") || command.startsWith("run ") ||
            command.startsWith("buɗe min ") || command.startsWith("bude min ") ->
                AssistantIntent.AppLaunch(extractAfterPrefix(command,"open ","bude min ","buɗe min ","bude ","buɗe ","launch ","start ","run "))

            command.contains("volume up") || command.contains("increase volume") ||
            command.contains("kara sauti") || command.contains("ƙara sauti") -> AssistantIntent.VolumeUp

            command.contains("volume down") || command.contains("decrease volume") ||
            command.contains("rage sauti") -> AssistantIntent.VolumeDown

            command == "mute" || command.contains("mute phone") || command.contains("yi shiru") -> AssistantIntent.Mute

            command.contains("battery") || command.contains("battery status") ||
            command.contains("nawa battery") || command.contains("nawa batirin") ||
            command.contains("nawa ya rage") -> AssistantIntent.BatteryStatus

            command.startsWith("search google for ") || command.startsWith("search google ") ||
            command.startsWith("google search for ") || command.startsWith("google search ") ||
            command.startsWith("search for ") || command.startsWith("search ") ||
            command.startsWith("google ") || command.startsWith("bincika a google ") ||
            command.startsWith("bincika ") || command.startsWith("nemo a google ") ->
                extractSearchQuery(command).let { if(it.isBlank()) AssistantIntent.Unknown else AssistantIntent.WebSearch(it) }

            else -> AssistantIntent.Unknown
        }
    }

    private fun extractAfterPrefix(command:String,vararg prefixes:String):String =
        prefixes.firstOrNull { command.startsWith(it) }?.let { command.removePrefix(it).trim() } ?: ""

    private fun extractSearchQuery(command:String):String =
        extractAfterPrefix(command,"search google for ","search google ","google search for ","google search ","search for ","search ","google ","bincika a google ","bincika ","nemo a google ")
}