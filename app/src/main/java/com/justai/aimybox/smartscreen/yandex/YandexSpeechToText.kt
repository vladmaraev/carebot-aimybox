package com.justai.aimybox.smartscreen.yandex

import android.content.Context
import androidx.annotation.RequiresPermission
import com.justai.aimybox.core.SpeechToTextException
import com.justai.aimybox.speechtotext.SpeechToText
import com.justai.superhero.yandex.L
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import ru.yandex.speechkit.*
import java.util.*

class YandexSpeechToText(context: Context, apiKey: String
) : SpeechToText(), CoroutineScope {

    override val coroutineContext = Dispatchers.Main + Job()

    private var recognizer: Recognizer? = null

    init {
        SpeechKit.getInstance().apply {
            init(context, apiKey)
            uuid = UUID.randomUUID().toString()
        }
    }

    override suspend fun cancelRecognition() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    override fun destroy() {
        launch {
            cancelRecognition()
        }
    }

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    override fun startRecognition(): ReceiveChannel<Result> {
        val channel = Channel<Result>()
        launch {
            if (recognizer == null) {
                L.i("Initializing Yandex STT")

                recognizer = OnlineRecognizer.Builder(Language.RUSSIAN, OnlineModel.NOTES, createRecognitionListener(channel))
                    .setDisableAntimat(true)
                    .setEnablePunctuation(false)
                    .setWaitForConnection(false)
                    .build()

                L.i("Yandex STT is initialized")
            }

            recognizer?.startRecording()
        }

        return channel
    }

    override suspend fun stopRecognition() {
        recognizer?.stopRecording()
    }

    private fun createRecognitionListener(resultChannel: Channel<Result>) = object: RecognizerListener {

        private fun sendResult(result: Result) {
            if (resultChannel.isClosedForSend) {
                L.w("Channel $resultChannel is closed for send. Omitting $result")
            } else {
                resultChannel.offer(result).let { success ->
                    if (!success) L.w("Failed to send $result to $resultChannel")
                }
            }
        }

        override fun onRecordingDone(p0: Recognizer) {}

        override fun onPowerUpdated(p0: Recognizer, p1: Float) = onSoundVolumeRmsChanged(p1)

        override fun onPartialResults(recognizer: Recognizer, recognition: Recognition, end: Boolean) {
            if (end) {
                sendResult(Result.Final(recognition.bestResultText))
            } else {
                sendResult(Result.Partial(recognition.bestResultText))
            }
        }

        override fun onMusicResults(p0: Recognizer, p1: Track) {}

        override fun onRecordingBegin(p0: Recognizer) {}

        override fun onSpeechEnds(p0: Recognizer) = onSpeechEnd()

        override fun onRecognizerError(p0: Recognizer, e: Error) {
            sendResult(Result.Exception(SpeechToTextException(e.message)))
        }

        override fun onSpeechDetected(p0: Recognizer) = onSpeechStart()

        override fun onRecognitionDone(p0: Recognizer) {
            resultChannel.close()
            recognizer = null
        }

    }

}