package com.cesi.assistant.core.intent

class IntentEngine {

    fun understand(input: String): AssistantIntent {
        val command = input.trim().lowercase().replace(Regex("\\s+"), " ")
        if (command.isBlank()) return AssistantIntent.Unknown

        return when {
            command.contains("turn on flashlight") || command.contains("switch on flashlight") ||
            command.contains("turn on torch") || command.contains("switch on torch") ||
            command == "flashlight" || command == "torch" || command.contains("kunna haske") ||
            command.contains("kunna torch") -> AssistantIntent.FlashlightOn

            command.contains("turn off flashlight") || command.contains("switch off flashlight") ||
            command.contains("turn off torch") || command.contains("switch off torch") ||
            command.contains("kashe flashlight") || command.contains("kashe torch") ||
            command.contains("kashe haske") -> AssistantIntent.FlashlightOff

            command.contains("take a selfie") || command.contains("take selfie") ||
            command.contains("selfie") || command.contains("hoton kaina") -> AssistantIntent.Selfie

            command == "camera" || command.contains("open camera") || command.contains("bude camera") ||
            command.contains("buɗe camera") || command.contains("take a photo") ||
            command.contains("take a picture") || command.contains("kamara") -> AssistantIntent.Camera

            command.contains("where am i") || command.contains("my location") ||
            command.contains("show my location") || command.contains("ina nake") ||
            command.contains("ina nake yanzu") -> AssistantIntent.Location

            command == "what time is it" || command == "what's the time" || command == "time" ||
            command.contains("current time") || command.contains("lokaci nawa") ||
            command.contains("wani lokaci") -> AssistantIntent.Time

            command == "what is today's date" || command == "what is the date" ||
            command == "today's date" || command == "date" || command.contains("today date") ||
            command.contains("kwanan wata") || command.contains("ranar yau") -> AssistantIntent.Date

            command == "settings" || command == "open settings" || command == "bude settings" ||
            command == "buɗe settings" || command.contains("phone settings") -> AssistantIntent.OpenSettings

            command.contains("wifi settings") || command.contains("wi-fi settings") ||
            command.contains("open wifi") || command.contains("bude wifi") ||
            command.contains("buɗe wifi") || command.contains("saitin wifi") -> AssistantIntent.WifiSettings

            command.contains("bluetooth settings") || command.contains("open bluetooth") ||
            command.contains("bude bluetooth") || command.contains("buɗe bluetooth") ||
            command.contains("saitin bluetooth") -> AssistantIntent.BluetoothSettings

            command.contains("sound settings") || command.contains("audio settings") ||
            command.contains("open sound settings") || command.contains("bude sound settings") ||
            command.contains("saitin sauti") -> AssistantIntent.SoundSettings

            command.contains("display settings") || command.contains("screen settings") ||
            command.contains("open display settings") || command.contains("bude display settings") ||
            command.contains("saitin screen") -> AssistantIntent.DisplaySettings

            command.contains("notification settings") || command.contains("open notification settings") ||
            command.contains("bude notification settings") || command.contains("saitin notification") ->
                AssistantIntent.NotificationSettings

            isMessageCommand(command) -> parseMessage(command)

            command.matches(Regex("""(?:dial|buga)\s+[*#0-9+() -]{2,}""")) ->
                AssistantIntent.Dial(extractAfterPrefix(command, "dial ", "buga ").trim())

            command.matches(Regex("""(?:ussd|dial ussd|lambar ussd)\s+[*#0-9+() -]{2,}""")) ->
                AssistantIntent.Ussd(extractAfterPrefix(command, "ussd ", "dial ussd ", "lambar ussd ").trim())

            command.startsWith("call ") || command.startsWith("kira ") ->
                AssistantIntent.Call(extractAfterPrefix(command, "call ", "kira "))

            command.startsWith("find contact ") || command.startsWith("search contact ") ||
            command.startsWith("nemo contact ") || command.startsWith("nemo lambar ") ->
                AssistantIntent.ContactSearch(extractAfterPrefix(
                    command, "find contact ", "search contact ", "nemo contact ", "nemo lambar "
                ))

            command.startsWith("open ") || command.startsWith("bude ") || command.startsWith("buɗe ") ||
            command.startsWith("launch ") || command.startsWith("start ") || command.startsWith("run ") ->
                AssistantIntent.AppLaunch(extractAfterPrefix(
                    command, "open ", "bude ", "buɗe ", "launch ", "start ", "run "
                ))

            command.contains("volume up") || command.contains("increase volume") ||
            command.contains("kara sauti") || command.contains("ƙara sauti") -> AssistantIntent.VolumeUp

            command.contains("volume down") || command.contains("decrease volume") ||
            command.contains("rage sauti") -> AssistantIntent.VolumeDown

            command == "mute" || command.contains("mute phone") || command.contains("yi shiru") ->
                AssistantIntent.Mute

            isAlarmCommand(command) -> parseAlarm(command)

            command.contains("battery") || command.contains("battery status") ||
            command.contains("nawa battery") || command.contains("nawa batirin") ->
                AssistantIntent.BatteryStatus

            command.startsWith("search google for ") || command.startsWith("search google ") ||
            command.startsWith("google search for ") || command.startsWith("google search ") ||
            command.startsWith("search for ") || command.startsWith("search ") ||
            command.startsWith("google ") || command.startsWith("bincika a google ") ||
            command.startsWith("bincika ") || command.startsWith("nemo a google ") -> {
                val query = extractSearchQuery(command)
                if (query.isBlank()) AssistantIntent.Unknown else AssistantIntent.WebSearch(query)
            }

            else -> AssistantIntent.Unknown
        }
    }

    private fun isMessageCommand(command: String): Boolean =
        command.matches(Regex("""(?:send|send a|send me a)\s+(?:whatsapp\s+)?message\s+to\s+.+\s+(?:saying|that says|with the message)\s+.+""")) ||
        command.matches(Regex("""(?:whatsapp|message)\s+.+\s+(?:saying|that says)\s+.+"""))

    private fun parseMessage(command: String): AssistantIntent {
        val match = Regex(
            """(?:send|send a|send me a)\s+(?:whatsapp\s+)?message\s+to\s+(.+?)\s+(?:saying|that says|with the message)\s+(.+)"""
        ).find(command) ?: Regex(
            """(?:whatsapp|message)\s+(.+?)\s+(?:saying|that says)\s+(.+)"""
        ).find(command) ?: return AssistantIntent.Unknown
        return AssistantIntent.Message(match.groupValues[1].trim(), match.groupValues[2].trim())
    }

    private fun isAlarmCommand(command: String): Boolean =
        command.matches(Regex("""(?:set|create|make|add)\s+(?:an\s+)?alarm\s+(?:for\s+)?\d{1,2}(?::\d{2})?\s*(?:am|pm)?""")) ||
        command.matches(Regex("""(?:set|create|make|add)\s+(?:an\s+)?(?:alarm|ƙararrawa|kararrawa)\s+.*"""))

    private fun parseAlarm(command: String): AssistantIntent {
        val timeMatch = Regex("""(\d{1,2})(?:[:.]([0-5]\d))?\s*(am|pm)?""", RegexOption.IGNORE_CASE)
            .find(command) ?: return AssistantIntent.Unknown
        var hour = timeMatch.groupValues[1].toIntOrNull() ?: return AssistantIntent.Unknown
        val minute = timeMatch.groupValues[2].ifBlank { "0" }.toIntOrNull() ?: 0
        val meridiem = timeMatch.groupValues[3].lowercase()
        if (meridiem == "pm" && hour in 1..11) hour += 12
        if (meridiem == "am" && hour == 12) hour = 0
        if (hour !in 0..23 || minute !in 0..59) return AssistantIntent.Unknown
        return AssistantIntent.SetAlarm(hour, minute, null)
    }

    private fun extractAfterPrefix(command: String, vararg prefixes: String): String {
        for (prefix in prefixes) if (command.startsWith(prefix)) return command.removePrefix(prefix).trim()
        return ""
    }

    private fun extractSearchQuery(command: String): String = extractAfterPrefix(
        command, "search google for ", "search google ", "google search for ", "google search ",
        "search for ", "search ", "google ", "bincika a google ", "bincika ", "nemo a google "
    )
}
