package com.cesi.assistant.core.intent

class IntentEngine {
    fun understand(input: String): AssistantIntent {
        val command = input.trim().lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .replace(Regex("\\s+"), " ").trim()
        if (command.isBlank()) return AssistantIntent.Unknown
        return when {
            command.contains("turn on flashlight") || command.contains("switch on flashlight") || command.contains("turn on torch") || command.contains("switch on torch") || command=="flashlight" || command=="torch" || command.contains("kunna haske") || command.contains("kunna torch") -> AssistantIntent.FlashlightOn
            command.contains("turn off flashlight") || command.contains("switch off flashlight") || command.contains("turn off torch") || command.contains("switch off torch") || command.contains("kashe flashlight") || command.contains("kashe torch") || command.contains("kashe haske") -> AssistantIntent.FlashlightOff
            command.contains("take a selfie") || command.contains("take selfie") || command.contains("selfie") || command.contains("hoton kaina") -> AssistantIntent.Selfie
            command=="camera" || command.contains("open camera") || command.contains("bude camera") || command.contains("buɗe camera") || command.contains("take a photo") || command.contains("take a picture") || command.contains("kamara") -> AssistantIntent.Camera
            command.contains("where am i") || command.contains("my location") || command.contains("show my location") || command.contains("ina nake") -> AssistantIntent.Location
            command.contains("what time") || command.contains("current time") || command=="time" || command.contains("lokaci nawa") || command.contains("wani lokaci") -> AssistantIntent.Time
            command.contains("what is the date") || command=="date" || command.contains("today date") || command.contains("kwanan wata") || command.contains("ranar yau") -> AssistantIntent.Date
            command=="settings" || command=="open settings" || command.contains("phone settings") -> AssistantIntent.OpenSettings
            command.contains("wifi settings") || command.contains("wi fi settings") || command.contains("open wifi") || command.contains("open wi fi") || command.contains("wireless settings") || command.contains("bude wifi") || command.contains("bude wi fi") || command.contains("saitin wifi") -> AssistantIntent.WifiSettings
            command.contains("bluetooth settings") || command.contains("open bluetooth") || command.contains("bude bluetooth") || command.contains("saitin bluetooth") -> AssistantIntent.BluetoothSettings
            command.contains("sound settings") || command.contains("audio settings") || command.contains("open sound") || command.contains("bude sound") || command.contains("saitin sauti") -> AssistantIntent.SoundSettings
            command.contains("display settings") || command.contains("screen settings") || command.contains("open display") || command.contains("bude display") || command.contains("saitin screen") -> AssistantIntent.DisplaySettings
            command.contains("notification settings") || command.contains("open notification") || command.contains("bude notification") -> AssistantIntent.NotificationSettings
            command.startsWith("call ") || command.startsWith("kira ") -> AssistantIntent.Call(extract(command,"call ","kira "))
            command.startsWith("find contact ") || command.startsWith("search contact ") || command.startsWith("nemo contact ") || command.startsWith("nemo lambar ") -> AssistantIntent.ContactSearch(extract(command,"find contact ","search contact ","nemo contact ","nemo lambar "))
            command.startsWith("send whatsapp message to ") || command.startsWith("send whatsapp to ") || command.startsWith("whatsapp message to ") || command.startsWith("tura ma ") || command.startsWith("tura sako zuwa ") -> {
                val body=extract(command,"send whatsapp message to ","send whatsapp to ","whatsapp message to ","tura ma ","tura sako zuwa ")
                val marker=listOf(" saying "," message "," cewa ").firstOrNull{body.contains(it)}
                if(marker==null) AssistantIntent.Unknown else {
                    val target=body.substringBefore(marker).trim(); val msg=body.substringAfter(marker).trim()
                    if(target.isBlank()||msg.isBlank()) AssistantIntent.Unknown else AssistantIntent.Message(target,msg)
                }
            }
            command.startsWith("open youtube and search for ") || command.startsWith("youtube search for ") || command.startsWith("search youtube for ") || command.startsWith("bincika youtube ") || command.startsWith("nemo a youtube ") -> {
                val q=extract(command,"open youtube and search for ","youtube search for ","search youtube for ","bincika youtube ","nemo a youtube ")
                if(q.isBlank()) AssistantIntent.Unknown else AssistantIntent.YouTubeSearch(q)
            }
            command.startsWith("open ") || command.startsWith("bude ") || command.startsWith("buɗe ") || command.startsWith("launch ") || command.startsWith("start ") -> AssistantIntent.AppLaunch(extract(command,"open ","bude ","buɗe ","launch ","start "))
            command.contains("volume up") || command.contains("increase volume") || command.contains("kara sauti") || command.contains("ƙara sauti") -> AssistantIntent.VolumeUp
            command.contains("volume down") || command.contains("decrease volume") || command.contains("rage sauti") -> AssistantIntent.VolumeDown
            command=="mute" || command.contains("mute phone") || command.contains("yi shiru") -> AssistantIntent.Mute
            command.contains("battery") || command.contains("nawa battery") || command.contains("nawa batirin") -> AssistantIntent.BatteryStatus
            command.startsWith("search google") || command.startsWith("google search") || command.startsWith("search for ") || command.startsWith("search ") || command.startsWith("google ") || command.startsWith("bincika ") || command.startsWith("nemo a google ") -> {
                val q=extract(command,"search google for ","search google ","google search for ","google search ","search for ","search ","google ","bincika a google ","bincika ","nemo a google ")
                if(q.isBlank()) AssistantIntent.Unknown else AssistantIntent.WebSearch(q)
            }
            else -> AssistantIntent.Unknown
        }
    }
    private fun extract(command:String,vararg prefixes:String):String {
        for(p in prefixes) if(command.startsWith(p)) return command.removePrefix(p).trim()
        return ""
    }
}
