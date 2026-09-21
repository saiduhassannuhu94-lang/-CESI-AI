package com.cesi.assistant.core.intent

class IntentEngine {

    fun understand(input: String): AssistantIntent {
        val command = input.trim().lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (command.isBlank()) return AssistantIntent.Unknown

        return when {
            command.contains("turn on flashlight") ||
            command.contains("switch on flashlight") ||
            command.contains("turn on torch") ||
            command.contains("switch on torch") ||
            command == "flashlight" ||
            command == "torch" ||
            command.contains("kunna haske") ||
            command.contains("kunna torch") ->
                AssistantIntent.FlashlightOn

            command.contains("turn off flashlight") ||
            command.contains("switch off flashlight") ||
            command.contains("turn off torch") ||
            command.contains("switch off torch") ||
            command.contains("kashe flashlight") ||
            command.contains("kashe torch") ||
            command.contains("kashe haske") ->
                AssistantIntent.FlashlightOff

            command.contains("take a selfie") ||
            command.contains("take selfie") ||
            command.contains("selfie") ||
            command.contains("hoton kaina") ->
                AssistantIntent.Selfie

            command == "camera" ||
            command.contains("open camera") ||
            command.contains("bude camera") ||
            command.contains("buɗe camera") ||
            command.contains("take a photo") ||
            command.contains("take a picture") ||
            command.contains("kamara") ->
                AssistantIntent.Camera

            command.contains("where am i") ||
            command.contains("my location") ||
            command.contains("show my location") ||
            command.contains("ina nake") ||
            command.contains("ina nake yanzu") ->
                AssistantIntent.Location

            command == "what time is it" ||
            command == "what is the time" ||
            command == "whats the time" ||
            command == "time" ||
            command == "tell me the time" ||
            command == "current time" ||
            command.contains("what time") ||
            command.contains("current time") ||
            command.contains("lokaci nawa") ||
            command.contains("wani lokaci") ->
                AssistantIntent.Time

            command == "what is todays date" ||
            command == "what is today date" ||
            command == "what is the date" ||
            command == "today's date" ||
            command == "date" ||
            command.contains("today date") ||
            command.contains("kwanan wata") ||
            command.contains("ranar yau") ->
                AssistantIntent.Date

            command == "settings" ||
            command == "open settings" ||
            command == "bude settings" ||
            command == "buɗe settings" ||
            command.contains("phone settings") ->
                AssistantIntent.OpenSettings

            command.contains("wifi settings") ||
            command.contains("open wifi") ||
            command.contains("wifi setting") ||
            command.contains("wireless settings") ||
            command.contains("bude wifi") ||
            command.contains("buɗe wifi") ||
            command.contains("saitin wifi") ->
                AssistantIntent.WifiSettings

            command.contains("bluetooth settings") ||
            command.contains("open bluetooth") ||
            command.contains("bude bluetooth") ||
            command.contains("buɗe bluetooth") ||
            command.contains("saitin bluetooth") ->
                AssistantIntent.BluetoothSettings

            command.contains("sound settings") ||
            command.contains("sound setting") ||
            command.contains("audio settings") ||
            command.contains("audio setting") ||
            command.contains("open sound settings") ||
            command.contains("bude sound settings") ||
            command.contains("saitin sauti") ->
                AssistantIntent.SoundSettings

            command.contains("display settings") ||
            command.contains("display setting") ||
            command.contains("screen settings") ||
            command.contains("screen setting") ||
            command.contains("open display settings") ||
            command.contains("bude display settings") ||
            command.contains("saitin screen") ->
                AssistantIntent.DisplaySettings

            command.contains("notification settings") ||
            command.contains("open notification settings") ||
            command.contains("bude notification settings") ||
            command.contains("saitin notification") ->
                AssistantIntent.NotificationSettings

            command.startsWith("call ") ||
            command.startsWith("kira ") ->
                AssistantIntent.Call(
                    extractAfterPrefix(command, "call ", "kira ")
                )

            command.startsWith("find contact ") ||
            command.startsWith("search contact ") ||
            command.startsWith("nemo contact ") ||
            command.startsWith("nemo lambar ") ->
                AssistantIntent.ContactSearch(
                    extractAfterPrefix(
                        command,
                        "find contact ",
                        "search contact ",
                        "nemo contact ",
                        "nemo lambar "
                    )
                )

            command.startsWith("open ") ||
            command.startsWith("bude ") ||
            command.startsWith("buɗe ") ||
            command.startsWith("launch ") ||
            command.startsWith("start ") ||
            command.startsWith("run ") ->
                AssistantIntent.AppLaunch(
                    extractAfterPrefix(
                        command,
                        "open ",
                        "bude ",
                        "buɗe ",
                        "launch ",
                        "start ",
                        "run "
                    )
                )

            command.contains("volume up") ||
            command.contains("increase volume") ||
            command.contains("kara sauti") ||
            command.contains("ƙara sauti") ->
                AssistantIntent.VolumeUp

            command.contains("volume down") ||
            command.contains("decrease volume") ||
            command.contains("rage sauti") ->
                AssistantIntent.VolumeDown

            command == "mute" ||
            command.contains("mute phone") ||
            command.contains("yi shiru") ->
                AssistantIntent.Mute

            command.contains("battery") ||
            command.contains("battery status") ||
            command.contains("nawa battery") ||
            command.contains("nawa batirin") ->
                AssistantIntent.BatteryStatus

            command.startsWith("search google for ") ||
            command.startsWith("search google ") ||
            command.startsWith("google search for ") ||
            command.startsWith("google search ") ||
            command.startsWith("search for ") ||
            command.startsWith("search ") ||
            command.startsWith("google ") ||
            command.startsWith("bincika a google ") ||
            command.startsWith("bincika ") ||
            command.startsWith("nemo a google ") -> {
                val query = extractSearchQuery(command)
                if (query.isBlank()) AssistantIntent.Unknown
                else AssistantIntent.WebSearch(query)
            }

            else -> AssistantIntent.Unknown
        }
    }

    private fun extractAfterPrefix(
        command: String,
        vararg prefixes: String
    ): String {
        for (prefix in prefixes) {
            if (command.startsWith(prefix)) {
                return command.removePrefix(prefix).trim()
            }
        }
        return ""
    }

    private fun extractSearchQuery(command: String): String {
        return extractAfterPrefix(
            command,
            "search google for ",
            "search google ",
            "google search for ",
            "google search ",
            "search for ",
            "search ",
            "google ",
            "bincika a google ",
            "bincika ",
            "nemo a google "
        )
    }
}
