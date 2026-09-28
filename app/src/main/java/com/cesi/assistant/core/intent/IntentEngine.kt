package com.cesi.assistant.core.intent

class IntentEngine {
    fun understand(input: String): AssistantIntent {
        var command = input.trim().lowercase().replace(Regex("\\s+"), " ")
        command = command
            .replace("’", "'")
            .replace("—", "-")
            .removePrefix("hey cesi,")
            .removePrefix("hey cesi")
            .removePrefix("cesi,")
            .removePrefix("cesi")
            .trim()
            .removePrefix("please ")
            .removePrefix("can you ")
            .removePrefix("could you ")
            .removePrefix("would you ")
            .removePrefix("would you please ")
            .removePrefix("i want you to ")
            .removePrefix("i need you to ")
            .removePrefix("i'd like you to ")
            .removePrefix("i would like you to ")
            .trim()
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""\bturn on the flashlight\b"""), "turn on flashlight")
            .replace(Regex("""\bturn off the flashlight\b"""), "turn off flashlight")
            .replace(Regex("""\bturn on the torch\b"""), "turn on torch")
            .replace(Regex("""\bturn off the torch\b"""), "turn off torch")
            .replace(Regex("""\bopen up\b"""), "open")
            .trim()
            .removeSuffix("?")
            .removeSuffix("!")
            .removeSuffix(".")
            .trim()
        if (command.isBlank()) return AssistantIntent.Unknown

        return when {
            command.contains("tell me the time") || command.contains("what time") ||
            command.contains("what's the time") -> AssistantIntent.Time

            command.contains("tell me today's date") || command.contains("what day is it") ||
            command.contains("what day today") -> AssistantIntent.Date

            command.contains("how much battery") || command.contains("how much charge") ||
            command.contains("battery left") -> AssistantIntent.BatteryStatus

            command.contains("where am i right now") || command.contains("where am i currently") ||
            command.contains("tell me my location") || command.contains("what is my location") ||
            command.contains("what's my location") -> AssistantIntent.Location

            command.contains("turn the flashlight on") || command.contains("turn the torch on") ||
            command.contains("switch the torch on") -> AssistantIntent.FlashlightOn

            command.contains("turn the flashlight off") || command.contains("turn the torch off") ||
            command.contains("switch the torch off") -> AssistantIntent.FlashlightOff

            command.contains("open the camera") || command.contains("open my camera") ||
            command.contains("take a photo for me") -> AssistantIntent.Camera

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

            isAdviceCommand(command) -> AssistantIntent.Advice(extractAfterPrefix(command,
                "give me advice on ", "give me advice about ", "i need advice on ",
                "i need advice about ", "advise me on ", "advise me about ",
                "help me decide ", "help me choose ", "what should i do about ", "should i ",
                "ka bani shawara akan ", "ka bani shawara game da ",
                "ina bukatar shawara akan ", "ina bukatar shawara game da ",
                "me zan yi ", "me ya kamata in yi ", "ya kamata in "
            ))

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
                AssistantIntent.Call(
                    extractAfterPrefix(command, "call ", "kira ")
                        .removePrefix("my ")
                        .removePrefix("a ")
                        .trim()
                )

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

            else -> naturalFallback(command)
        }
    }

    /**
     * Local semantic fallback for common natural speech. This does not replace
     * a real LLM brain, but it makes CESI much less dependent on one exact
     * sentence pattern while remaining deterministic and safe.
     */
    private fun naturalFallback(command: String): AssistantIntent {
        val c = command.lowercase().trim()

        fun hasAny(vararg phrases: String): Boolean =
            phrases.any { c.contains(it) }

        val locationScore =
            (if (hasAny("where am i", "where exactly am i", "where am i now", "my current location", "my exact location", "tell me where i am", "show me where i am", "ina nake", "ina nake yanzu", "ina nake a yanzu", "wurin da nake")) 3 else 0) +
            (if (hasAny("location", "gps", "where")) 1 else 0)

        val timeScore =
            (if (hasAny("what time", "current time", "time right now", "tell me the time", "lokaci nawa", "wane lokaci")) 3 else 0)

        val batteryScore =
            (if (hasAny("battery", "battery percentage", "battery level", "charge left", "how much charge", "nawa battery", "batirin")) 3 else 0)

        val flashlightOnScore =
            (if (hasAny("turn on flashlight", "switch on flashlight", "enable flashlight", "turn flashlight on", "flashlight on", "torch on", "kunna haske", "kunna torch")) 3 else 0)

        val flashlightOffScore =
            (if (hasAny("turn off flashlight", "switch off flashlight", "disable flashlight", "turn flashlight off", "flashlight off", "torch off", "kashe haske", "kashe torch")) 3 else 0)

        val cameraScore =
            (if (hasAny("open camera", "show me the camera", "bring up the camera", "take a picture", "take a photo", "camera app", "kamara", "bude camera", "buɗe camera")) 3 else 0)

        val selfieScore =
            (if (hasAny("take a selfie", "take my selfie", "selfie", "hoton kaina")) 4 else 0)

        val volumeUpScore =
            (if (hasAny("make it louder", "turn the volume up", "increase the volume", "raise the volume", "louder please", "kara sauti", "ƙara sauti")) 3 else 0)

        val volumeDownScore =
            (if (hasAny("make it quieter", "turn the volume down", "decrease the volume", "lower the volume", "quieter please", "rage sauti")) 3 else 0)

        val muteScore =
            (if (hasAny("mute the phone", "silence the phone", "put the phone on silent", "make the phone silent", "mute", "yi shiru")) 3 else 0)

        val searchMarkers = listOf(
            "search for ", "look up ", "google this ", "find out about ",
            "search online for ", "bincika ", "nemo a google "
        )
        if (searchMarkers.any { c.startsWith(it) }) {
            val query = c.substringAfter("search for ", "")
                .ifBlank { c.substringAfter("look up ", "") }
                .ifBlank { c.substringAfter("google this ", "") }
                .ifBlank { c.substringAfter("find out about ", "") }
                .ifBlank { c.substringAfter("search online for ", "") }
                .ifBlank { c.substringAfter("bincika ", "") }
                .ifBlank { c.substringAfter("nemo a google ", "") }
                .trim()
            if (query.isNotBlank()) return AssistantIntent.WebSearch(query)
        }

        if (locationScore >= 3) return AssistantIntent.Location
        if (timeScore >= 3) return AssistantIntent.Time
        if (batteryScore >= 3) return AssistantIntent.BatteryStatus
        if (flashlightOnScore >= 3) return AssistantIntent.FlashlightOn
        if (flashlightOffScore >= 3) return AssistantIntent.FlashlightOff
        if (selfieScore >= 4) return AssistantIntent.Selfie
        if (cameraScore >= 3) return AssistantIntent.Camera
        if (volumeUpScore >= 3) return AssistantIntent.VolumeUp
        if (volumeDownScore >= 3) return AssistantIntent.VolumeDown
        if (muteScore >= 3) return AssistantIntent.Mute

        val appNames = listOf(
            "youtube", "whatsapp", "chrome", "messenger", "facebook", "instagram",
            "settings", "camera", "gmail", "calculator", "clock", "gallery"
        )
        appNames.firstOrNull { app ->
            c.contains("open $app") || c.contains("launch $app") ||
                c.contains("start $app") || c.contains("bude $app") ||
                c.contains("buɗe $app")
        }?.let { return AssistantIntent.AppLaunch(it) }

        if (c.startsWith("call my ") || c.startsWith("call ")) {
            return AssistantIntent.Call(
                c.removePrefix("call my ").removePrefix("call ")
                    .removePrefix("my ")
                    .trim()
            )
        }
        if (c.startsWith("kira ")) {
            return AssistantIntent.Call(c.removePrefix("kira ").trim())
        }

        // Until CESI has a secured native LLM backend, ordinary factual/open
        // questions fall back to a useful web lookup instead of "unknown".
        val questionMarkers = listOf(
            "what is ", "what are ", "who is ", "who are ", "when is ", "when did ",
            "where is ", "where are ", "why is ", "why are ", "how do ", "how does ",
            "how can ", "how to ", "tell me about ", "explain ", "define ",
            "menene ", "waye ", "yaushe ", "me yasa ", "yaya "
        )
        val casual = setOf("how are you", "how are you doing", "are you okay", "what's up", "hello", "hi", "hey")
        if (c !in casual && questionMarkers.any { c.startsWith(it) }) {
            return AssistantIntent.WebSearch(c)
        }

        return AssistantIntent.Unknown
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

    private fun isAdviceCommand(command: String): Boolean =
        command.startsWith("give me advice on ") ||
        command.startsWith("give me advice about ") ||
        command.startsWith("i need advice on ") ||
        command.startsWith("i need advice about ") ||
        command.startsWith("advise me on ") ||
        command.startsWith("advise me about ") ||
        command.startsWith("help me decide ") ||
        command.startsWith("help me choose ") ||
        command.startsWith("what should i do about ") ||
        command.startsWith("should i ") ||
        command.startsWith("ka bani shawara akan ") ||
        command.startsWith("ka bani shawara game da ") ||
        command.startsWith("ina bukatar shawara akan ") ||
        command.startsWith("ina bukatar shawara game da ") ||
        command.startsWith("me zan yi ") ||
        command.startsWith("me ya kamata in yi ") ||
        command.startsWith("ya kamata in ")

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
