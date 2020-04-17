package com.justai.aimybox.smartscreen

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.justai.aimybox.Aimybox
import com.justai.aimybox.api.aimybox.AimyboxDialogApi
import com.justai.aimybox.core.Config
import com.justai.aimybox.smartscreen.skill.DateTimeSkill
import com.justai.aimybox.smartscreen.skill.MusicSkill
import com.justai.aimybox.smartscreen.yandex.YandexSpeechToText
import com.justai.aimybox.voicetrigger.nuance.Nuance
import com.justai.aimybox.voicetrigger.nuance.NuanceVoiceTrigger
import com.justai.superhero.yandex.YandexTextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class AssistantApplication: Application(), CoroutineScope, SharedPreferences.OnSharedPreferenceChangeListener {

    override val coroutineContext = Dispatchers.Main

    companion object {
        private const val YANDEX_API_KEY = "cc96633d-59d4-4724-94bd-f5db2f02ad13"
    }

    val aimybox: Aimybox by lazy { Aimybox(createAimyboxConfig(this)) }

    fun getViewModelFactory() = AssistantViewModel.Factory.getInstance(aimybox)

    override fun onCreate() {
        super.onCreate()
        PreferenceManager
            .getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)
    }

    private fun createAimyboxConfig(context: Context): Config {
        val apiKey = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(Preferences.AIMYBOX_KEY, null) ?: ""

        val unitId = UUID.randomUUID().toString()

        val speechToText = YandexSpeechToText(context, YANDEX_API_KEY)

        val textToSpeech = YandexTextToSpeech(context, YANDEX_API_KEY, "nick")

        val trigger = NuanceVoiceTrigger(
            context,
            Nuance.Language.RU,
            arrayOf(getString(R.string.wakeup_phrase))
        )

        val dialogApi = AimyboxDialogApi(apiKey, unitId,
            customSkills = linkedSetOf(
                DateTimeSkill(),
                MusicSkill()
            ))

        return Config.create(speechToText, textToSpeech, dialogApi) {
            setEarconRes(applicationContext, R.raw.earcon)
            voiceTrigger = trigger
            recognitionBehavior = Config.RecognitionBehavior.ALLOW_OVERRIDE
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (Preferences.AIMYBOX_KEY == key) {
            launch {
                aimybox.updateConfiguration(createAimyboxConfig(applicationContext))
                startActivity(Intent(applicationContext, HomeActivity::class.java))
            }
        }
    }
}