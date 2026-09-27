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

            isMessengerMessageCommand(command) -> parseMessengerMessage(command)

            isReplyCommand(command) -> AssistantIntent.Reply(
                extractAfterPrefix(command, "reply ", "reply to him ", "reply to her ", "reply him ", "amsa ", "amsa masa ", "amsa mata ")
            )

            isVisualCommand(command) -> {
                val query = extractVisualQuery(command)
                if (query.isBlank()) AssistantIntent.Unknown else AssistantIntent.VisualSearch(query)
            }

            isTopicFollowUp(command) -> AssistantIntent.TopicFollowUp(command)

            isMessageCommand(command) -> parseMessage(command)

            command.matches(Regex("""(?:dial|buga)\\s+[*#0-9+() -]{2,}""")) ->
                AssistantIntent.Dial(extractAfterPrefix(command, "dial ", "buga ").trim())

            command.matches(Regex("""(?:ussd|dial ussd|lambar ussd)\\s+[*#0-9+() -]{2,}""")) ->
                AssistantIntent.Ussd(extractAfterPrefix(command, "ussd ", "dial ussd ", "lambar ussd ").trim())

            command.startsWith("call ") || command.startsWith("kira ") ->
                AssistantIntent.Call(extractAfterPrefix(command, "call ", "kira "))

            command.startsWith("find contact ") || command.startsWith("search contact ") ||
            command.startsWith("nemo contact ") || command.startsWith("nemo lambar ") ->
                AssistantIntent.ContactSearch(extractAfterPrefix(
                    command, "find contact ", "search contact ", "nemo contact ", "nemo lambar "
                ))

            isYouTubeSearchCommand(command) -> {
                val query = extractAfterPrefix(
                    command,
                    "search youtube for ", "search youtube ", "youtube search for ",
                    "youtube search ", "search for ", "find on youtube ", "find in youtube ",
                    "bincika youtube ", "nemo a youtube "
                )
                if (query.isBlank()) AssistantIntent.Unknown else AssistantIntent.YouTubeSearch(query)
            }

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

    private fun isMessengerMessageCommand(command: String): Boolean =
        command.matches(Regex("""(?:send|send a|send me a)\\s+(?:messenger\\s+)?message\\s+to\\s+.+\\s+(?:saying|that says|with the message)\\s+.+""")) ||
        command.matches(Regex("""messenger\\s+.+\\s+(?:saying|that says)\\s+.+""")) ||
        command.matches(Regex("""tura\\s+(?:wa\\s+)?(?:messenger\\s+)?saƙo\\s+.+\\s+(?:cewa|yana cewa)\\s+.+"""))

    private fun parseMessengerMessage(command: String): AssistantIntent.MessengerMessage {
        val patterns = listOf(
            Regex("""(?:send|send a|send me a)\\s+(?:messenger\\s+)?message\\s+to\\s+(.+?)\\s+(?:saying|that says|with the message)\\s+(.+)"""),
            Regex("""messenger\\s+(.+?)\\s+(?:saying|that says)\\s+(.+)"""),
            Regex("""tura\\s+(?:wa\\s+)?(?:messenger\\s+)?saƙo\\s+(.+?)\\s+(?:cewa|yana cewa)\\s+(.+)""")
        )
        val match = patterns.firstNotNullOfOrNull { it.find(command) } ?: return AssistantIntent.MessengerMessage(null, "")
        return AssistantIntent.MessengerMessage(match.groupValues[1].trim(), match.groupValues[2].trim())
    }

    private fun isReplyCommand(command: String): Boolean =
        command.startsWith("reply ") || command.startsWith("reply to him ") ||
        command.startsWith("reply to her ") || command.startsWith("reply him ") ||
        command.startsWith("amsa ") || command.startsWith("amsa masa ") ||
        command.startsWith("amsa mata ")

    private fun isVisualCommand(command: String): Boolean =
        command.startsWith("show me a picture of ") ||
        command.startsWith("show me pictures of ") ||
        command.startsWith("show me an image of ") ||
        command.startsWith("show images of ") ||
        command.startsWith("show photos of ") ||
        command.startsWith("show me what ") ||
        command.startsWith("nuna min hoton ") ||
        command.startsWith("nuna min hotuna ") ||
        command.startsWith("nuna min hoto na ")

    private fun extractVisualQuery(command: String): String = extractAfterPrefix(
        command, "show me a picture of ", "show me pictures of ", "show me an image of ",
        "show images of ", "show photos of ", "show me what ",
        "nuna min hoton ", "nuna min hotuna ", "nuna min hoto na "
    )

    private fun isTopicFollowUp(command: String): Boolean =
        command in setOf(
            "explain more", "tell me more", "give me an example", "give me examples",
            "show me an example", "show me examples", "show me a diagram",
            "show me a picture", "show me pictures", "what does that mean",
            "ban fahimta ba", "ka kara bayani", "kara bayani", "misali", "nuna min misali",
            "nuna min hoto", "nuna min hotuna"
        ) || command.startsWith("explain that") || command.startsWith("what about that")

    private fun isYouTubeSearchCommand(command: String): Boolean =
        command.startsWith("search youtube for ") || command.startsWith("search youtube ") ||
        command.startsWith("youtube search for ") || command.startsWith("youtube search ") ||
        command.startsWith("find on youtube ") || command.startsWith("find in youtube ") ||
        command.startsWith("bincika youtube ") || command.startsWith("nemo a youtube ") ||
        Regex("""search for .+ on youtube$""").matches(command)

    private fun isMessageCommand(command: String): Boolean =
        command.matches(Regex("""(?:send|send a|send me a)\\s+(?:whatsapp\\s+)?message\\s+to\\s+.+\\s+(?:saying|that says|with the message)\\s+.+""")) ||
        command.matches(Regex("""(?:whatsapp|message)\\s+.+\\s+(?:saying|that says)\\s+.+""")) ||
        command.matches(Regex("""tura\\s+(?:wa\\s+)?saƙo\\s+.+\\s+(?:cewa|yana cewa)\\s+.+"""))

    private fun parseMessage(command: String): AssistantIntent.Message {
        val patterns = listOf(
            Regex("""(?:send|send a|send me a)\\s+(?:whatsapp\\s+)?message\\s+to\\s+(.+?)\\s+(?:saying|that says|with the message)\\s+(.+)"""),
            Regex("""(?:whatsapp|message)\\s+(.+?)\\s+(?:saying|that says)\\s+(.+)"""),
            Regex("""tura\\s+(?:wa\\s+)?saƙo\\s+(.+?)\\s+(?:cewa|yana cewa)\\s+(.+)""")
        )
        val match = patterns.firstNotNullOfOrNull { it.find(command) } ?: return AssistantIntent.Message("", "")
        return AssistantIntent.Message(match.groupValues[1].trim(), match.groupValues[2].trim())
    }

    private fun isAlarmCommand(command: String): Boolean =
        command.matches(Regex("""(?:set|create|make|add)\\s+(?:an\\s+)?alarm\\s+(?:for\\s+)?\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?""")) ||
        command.matches(Regex("""(?:set|create|make|add)\\s+(?:an\\s+)?(?:alarm|ƙararrawa|kararrawa)\\s+.*"""))

    private fun parseAlarm(command: String): AssistantIntent {
        val timeMatch = Regex("""(\\d{1,2})(?:[:.]([0-5]\\d))?\\s*(am|pm)?""", RegexOption.IGNORE_CASE)
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
        return Regex("""^search for (.+) on youtube$""").find(command)?.groupValues?.get(1)?.trim() ?: ""
    }

    private fun extractSearchQuery(command: String): String = extractAfterPrefix(
        command, "search google for ", "search google ", "google search for ", "google search ",
        "search for ", "search ", "google ", "bincika a google ", "bincika ", "nemo a google "
    )
}
