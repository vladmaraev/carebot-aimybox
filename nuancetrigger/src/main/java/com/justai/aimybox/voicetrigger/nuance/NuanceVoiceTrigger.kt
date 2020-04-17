package com.justai.aimybox.voicetrigger.nuance

import android.content.Context
import com.justai.aimybox.voicetrigger.VoiceTrigger
import com.nuance.dragon.toolkit.audio.AudioChunk
import com.nuance.dragon.toolkit.audio.AudioType
import com.nuance.dragon.toolkit.audio.pipes.BufferingDuplicatorPipe
import com.nuance.dragon.toolkit.audio.pipes.PrecisionClearBufferingPipe
import com.nuance.dragon.toolkit.audio.sources.MicrophoneRecorderSource
import com.nuance.dragon.toolkit.audio.sources.RecorderSource
import com.nuance.dragon.toolkit.vocon.*
import kotlinx.coroutines.*
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class NuanceVoiceTrigger(
    context: Context,
    language: Nuance.Language,
    private val phrases: Array<String>,
    private val threshold: Int = DEFAULT_THRESHOLD
): VoiceTrigger, CoroutineScope {

    companion object {
        private const val DEFAULT_THRESHOLD = -7000
        private const val PENALTY = 300
    }

    override val coroutineContext = newFixedThreadPoolContext(1, "Microphone")

    private val recognizerInitializationJob =
        createRecognizerAsync(context, language, true)

    private lateinit var recognizer: VoconRecognizer
    private lateinit var onTriggerListener: (String?) -> Unit
    private lateinit var onTriggerException: (e: Throwable) -> Unit

    private val wakeupListener = TriggerResultListener()

    private var audioSource: RecorderSource<AudioChunk>? = null
    private var duplicatorPipe: BufferingDuplicatorPipe<AudioChunk>? = null
    private var bufferAudioForSeamlessAudio: PrecisionClearBufferingPipe? = null

    private var startJob: Job? = null

    init {
        L.i("Initializing Nuance VoiceTrigger")
        require(phrases.any(String::isNotBlank)) { "Phrases must not be empty or contain blank strings.\n$phrases" }
    }

    override suspend fun startDetection(
        onTriggered: (phrase: String?) -> Unit,
        onException: (e: Throwable) -> Unit
    ) {
        startJob = launchDetection(onTriggered, onException)
        startJob?.join()
    }

    override suspend fun stopDetection() {
        L.i("Stopping Nuance VT")
        startJob?.cancel()
        if (::recognizer.isInitialized) recognizer.cancelRecognition()

        stopRecording()
        L.i("Nuance VT stopped")
    }

    override fun destroy() {
        recognizer.release()
        audioSource?.stopRecording()
    }

    private fun launchDetection(
        onTriggered: (phrase: String?) -> Unit,
        onException: (e: Throwable) -> Unit
    ) = launch(coroutineContext) {
        L.i("Starting Nuance VT")

        recognizerInitializationJob
            .takeIf { it.isActive }
            ?.join()

        onTriggerListener = onTriggered
        onTriggerException = onException

        startWakeup()
        L.i("Nuance VT started")
    }

    private suspend fun startWakeup() {
        var retries = 5
        while (retries > 0 && isActive) {
            try {
                audioSource = startRecording()
                break
            } catch (e: Throwable) {
                L.e("Failed to start recording. Tries left: (${--retries})...")
                delay(500)
                if (retries == 0) error("Failed to initialize NuanceVoiceTrigger after 5 retries")
            }
        }
        recognizer.startWakeupMode(
            audioSource,
            createRecognizerParams(),
            phrases.toList(),
            PENALTY,
            wakeupListener
        )
    }

    private fun startRecording(): MicrophoneRecorderSource = runBlocking(Dispatchers.Main) {
        val source = MicrophoneRecorderSource(AudioType.PCM_16k)
        suspendCoroutine<MicrophoneRecorderSource> { continuation ->
            source.startRecording(object : RecorderSource.Listener<AudioChunk> {
                override fun onStarted(p0: RecorderSource<AudioChunk>?) {
                    continuation.resume(source)
                }

                override fun onError(p0: RecorderSource<AudioChunk>?) {
                    continuation.resumeWithException(IOException("RecorderSourceException"))
                }

                override fun onStopped(p0: RecorderSource<AudioChunk>?) {}
            })
        }
    }

    private suspend fun stopRecording() {
        audioSource?.stopRecording()
        while (audioSource?.isActive == true) delay(50)
        audioSource = null
        duplicatorPipe?.disconnectAudioSource()
        duplicatorPipe = null
        bufferAudioForSeamlessAudio?.disconnectAudioSource()
        bufferAudioForSeamlessAudio = null
    }

    private fun createRecognizerParams() = hashMapOf(
        ParamSpecs.Fx.TSILENCE to 0,
        ParamSpecs.Fx.START_ENABLE to ParamSpecs.Boolean.TRUE,
        ParamSpecs.Fx.MINSPEECH to 150,
        ParamSpecs.Fx.TIMEOUT_LSILENCE to 0,
        ParamSpecs.Fx.TIMEOUT_SPEECH to 5000,
        ParamSpecs.Fx.EVENT_TIMER to 100,
        ParamSpecs.Fx.KNOWN_SPEAKER_CHANGES to ParamSpecs.Boolean.FALSE,
        ParamSpecs.Fx.FARTALK to ParamSpecs.Boolean.TRUE,
        ParamSpecs.Fx.SPEAKER_ADAPTATION to ParamSpecs.SpeakerAdaptMode.DISABLE,
        ParamSpecs.Fx.ABSOLUTE_THRESHOLD to threshold
    )

    private fun createRecognizerAsync(
        applicationContext: Context,
        language: Nuance.Language,
        isVerboseLoggingEnabled: Boolean
    ) = launch(Dispatchers.Main) {
        recognizer = VoconRecognizerFactory
            .create(applicationContext, VoconContext("grammar_smart_home.fcf"), language, isVerboseLoggingEnabled)
            .await()

        L.i("NuanceVoiceTrigger initialized")
    }

    private inner class TriggerResultListener: VoconRecognizer.ResultListener {
        override fun onResult(result: VoconResult) {
            L.i("VoconRecognizer OnResult")
            launch {
                stopDetection()
                onTriggerListener(result.recognizedWakeupPhrase)
            }
        }

        override fun onError(error: VoconError) {
            when {
                error.reasonCode == VoconError.Reason.CANCELED -> L.w("Nuance VT cancelled")
                error.reasonCode == VoconError.Reason.NO_RESULT -> L.w("Nuance VT no result")
                else -> launch {
                    L.e(NuanceVoiceTriggerException(error.reason, error.reasonCode))
                    delay(3000)
                    startDetection(onTriggerListener, onTriggerException)
                }
            }
        }
    }
}