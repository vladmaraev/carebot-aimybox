package com.justai.aimybox.smartscreen

import android.app.Application
import android.content.Context
import com.justai.aimybox.Aimybox
import com.justai.aimybox.core.Config
import com.justai.aimybox.smartscreen.api.SessionAwareDialogApi
import com.justai.aimybox.smartscreen.skill.DateTimeSkill
import com.justai.aimybox.speechkit.google.platform.GooglePlatformSpeechToText
import com.justai.aimybox.speechkit.yandex.cloud.IAmTokenGenerator
import com.justai.aimybox.speechkit.yandex.cloud.Language
import com.justai.aimybox.speechkit.yandex.cloud.Voice
import com.justai.aimybox.speechkit.yandex.cloud.YandexTextToSpeech
import com.justai.aimybox.voicetrigger.nuance.Nuance
import com.justai.aimybox.voicetrigger.nuance.NuanceVoiceTrigger
import java.util.*

class AssistantApplication: Application() {

    companion object {
        private const val AIMYBOX_API_KEY = "vVEU88RRaOIqdityVhTREY3jobKNnd1f"
        private const val YANDEX_TOKEN = "AgAEA7qh4Sk-AATuweUovqabw0TWu2zVVuIS3hU"
        private const val YANDEX_FOLDER = "b1gh6rgpahg9909290r8"
    }

    val aimybox: Aimybox by lazy { createAimybox(this) }

    fun getViewModelFactory() = AssistantViewModel.Factory.getInstance(aimybox)

    private fun createAimybox(context: Context): Aimybox {
        val unitId = UUID.randomUUID().toString()

        val speechToText = GooglePlatformSpeechToText(context)

        val textToSpeech = YandexTextToSpeech(
            context,
            IAmTokenGenerator(YANDEX_TOKEN),
            YANDEX_FOLDER, Language.RU,
            YandexTextToSpeech.Config(
                voice = Voice.FILIPP
            )
        )

        val trigger = NuanceVoiceTrigger(
            context,
            Nuance.Language.RU,
            arrayOf(getString(R.string.wakeup_phrase))
        )

        val dialogApi = SessionAwareDialogApi(AIMYBOX_API_KEY, unitId,
            customSkills = linkedSetOf(
                DateTimeSkill()
            ))

        return Aimybox(Config.create(speechToText, textToSpeech, dialogApi) {
            voiceTrigger = trigger
            recognitionBehavior = Config.RecognitionBehavior.ALLOW_OVERRIDE
        })
    }
}