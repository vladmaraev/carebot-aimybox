package com.justai.superhero.yandex

import android.content.Context
import android.net.Uri
import com.justai.aimybox.model.AudioSpeech
import com.justai.aimybox.model.TextSpeech
import com.justai.aimybox.texttospeech.BaseTextToSpeech

class YandexTextToSpeech(context: Context,
                         private val apiKey: String,
                         private val speaker: String
) : BaseTextToSpeech(context) {

    companion object {
        private val BASE_URI = Uri.parse("https://tts.voicetech.yandex.net/generate")
    }

    override suspend fun speak(speech: TextSpeech) {
        val uri = BASE_URI.buildUpon()
            .appendQueryParameter("key", apiKey)
            .appendQueryParameter("format", "mp3")
            .appendQueryParameter("speaker", speaker)
            .appendQueryParameter("text", speech.text)
            .build().toString()

        audioSynthesizer.play(AudioSpeech.Uri(uri))
    }

}