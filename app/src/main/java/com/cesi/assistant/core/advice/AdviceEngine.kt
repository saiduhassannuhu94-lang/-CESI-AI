package com.cesi.assistant.core.advice

class AdviceEngine {
    fun respond(situation: String): String {
        val text = situation.trim()
        if (text.isBlank()) {
            return "Ka gaya min abin da kake fuskanta ko zaɓin da kake tunani a kai. Zan taimaka maka mu rarraba shi zuwa zaɓuɓɓuka, ribobi, haɗari, da matakin da za ka iya gwadawa."
        }

        val lower = text.lowercase()
        return when {
            lower.contains("study") || lower.contains("exam") ||
                lower.contains("karatu") || lower.contains("jarrabawa") ->
                "Game da karatu, mu fara da abin da kake son samu da lokacin da ya rage. Ka gaya min course/topic ɗin, abin da kake iya yanzu, da abin da yake maka wahala; zan taimaka maka mu tsara abin da za ka yi farko."

            lower.contains("career") || lower.contains("job") ||
                lower.contains("aiki") || lower.contains("sana'a") || lower.contains("sana") ->
                "Game da career ko aiki, kada mu duba kuɗi kaɗai. Mu kwatanta skills ɗin da ake bukata, lokacin koyo, damar samun aiki, kudin farawa, da yadda zaɓin ya dace da burinka; ka kawo min zaɓuɓɓukan da kake tsakanin su."

            lower.contains("business") || lower.contains("kasuwanci") ||
                lower.contains("money") || lower.contains("kudi") || lower.contains("kuɗi") ->
                "Game da kasuwanci ko kuɗi, mu fara da risk da abin da za ka iya rasa ba tare da matsalar rayuwa ba. Ka gaya min jari, abin da kake son sayarwa, customers ɗinka, da zaɓuɓɓukan da kake tunani a kai; zan taimaka maka mu kwatanta su."

            lower.contains("relationship") || lower.contains("friend") ||
                lower.contains("soyayya") || lower.contains("aboki") ->
                "Game da relationship ko abota, mu raba abin da ka sani daga abin da kake zato. Ka gaya min abin da ya faru, abin da kake so ya kasance, da abin da kake tsoron zai faru; daga nan mu duba hanyoyin da za su rage rikici kuma su mutunta kowa."

            lower.contains("buy") || lower.contains("purchase") ||
                lower.contains("saya") || lower.contains("sayen") ->
                "Kafin ka saya, mu duba bukata, farashi, quality, alternatives, da ko sayen zai shafi sauran kuɗin da kake bukata. Ka gaya min abin da kake son saya da budget ɗinka."

            lower.contains("should i") || lower.contains("what should i do") ||
                lower.contains("me zan yi") || lower.contains("me ya kamata in yi") ||
                lower.contains("ya kamata in") ->
                "Kada mu yi gaggawar zaɓar maka. Ka gaya min zaɓuɓɓukan da kake da su, abin da ya fi muhimmanci gare ka, da abin da kake son kauce wa; zan taimaka maka mu duba ribar, hasara, risk, da mataki na farko."

            else ->
                "Zan taimaka maka ka duba wannan a hankali ba tare da yin gaggawar yanke maka hukunci ba. Ka gaya min: menene burinka, wane zaɓuɓɓuka kake da su, menene abin da ya fi muhimmanci gare ka, da menene babban abin da kake tsoro; daga nan mu tsara mataki mai ma'ana."
        }
    }
}
