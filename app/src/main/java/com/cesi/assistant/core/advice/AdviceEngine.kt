package com.cesi.assistant.core.advice

class AdviceEngine {
    fun respond(situation: String): String {
        val text = situation.trim()
        if (text.isBlank()) {
            return "Ka gaya min abin da kake fuskanta. Zan taimaka maka mu duba zaɓuɓɓukan da kake da su."
        }

        val lower = text.lowercase()
        return when {
            lower.contains("study") || lower.contains("exam") || lower.contains("karatu") || lower.contains("jarrabawa") ->
                "Mu fara da abin da kake son cimmawa, lokacin da ya rage, da abin da ya fi baka wahala. Daga nan zan taimaka maka mu tsara matakai masu yiwuwa."

            lower.contains("career") || lower.contains("job") || lower.contains("aiki") || lower.contains("career") ->
                "Zan iya taimaka maka ka kwatanta zaɓuɓɓuka bisa ga skills, lokaci, kudin farawa, da abin da kake son cimmawa. Ka ba ni zaɓuɓɓukan da kake tunani a kai."

            lower.contains("business") || lower.contains("kasuwanci") || lower.contains("money") || lower.contains("kudi") ->
                "Kafin mu yanke shawara, mu duba jari, risk, customers, da abin da za ka iya gwadawa da ƙaramin asara."

            else ->
                "Zan taimaka maka ka duba wannan a hankali: menene burinka, wane zaɓuɓɓuka kake da su, menene ribar da hasarar kowane zaɓi, kuma wane mataki ne za ka iya gwadawa yanzu."
        }
    }
}
