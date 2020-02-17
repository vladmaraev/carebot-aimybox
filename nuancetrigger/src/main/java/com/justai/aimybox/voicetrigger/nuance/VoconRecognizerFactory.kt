package com.justai.aimybox.voicetrigger.nuance

import android.content.Context
import com.nuance.dragon.toolkit.file.FileManager
import com.nuance.dragon.toolkit.vocon.Grammar
import com.nuance.dragon.toolkit.vocon.VoconConfig
import com.nuance.dragon.toolkit.vocon.VoconContext
import com.nuance.dragon.toolkit.vocon.VoconError
import com.nuance.dragon.toolkit.vocon.VoconRecognizer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object VoconRecognizerFactory {

    private val ACMODES = hashMapOf(
        Nuance.Language.EN to "acmod5_4000_enu_gen_car_f16_v2_0_0.dat",
        Nuance.Language.RU to "acmod5_4000_rur_gen_car_f16_v2_0_0.dat"
    )

    private val CLC = hashMapOf(
        Nuance.Language.EN to "clc_enu_cfg3_v6_0_4.dat",
        Nuance.Language.RU to "clc_rur_cfg1_v6_0_3.dat.dat"
    )

    fun create(
        applicationContext: Context,
        voconContext: VoconContext,
        language: Nuance.Language,
        isVerboseLoggingEnabled: Boolean
    ): Deferred<VoconRecognizer> {
        val deferred = CompletableDeferred<VoconRecognizer>()
        VoconRecognizer.createVoconRecognizer(FileManager(applicationContext, ".jpg", "vocon", "vocon"))
            .apply { enableVerboseAndroidLogging(isVerboseLoggingEnabled) }
            .initialize(language, voconContext, deferred)
        return deferred
    }

    private fun VoconRecognizer.initialize(
        language: Nuance.Language,
        voconContext: VoconContext,
        deferred: CompletableDeferred<VoconRecognizer>
    ) = GlobalScope.launch {
        initialize(VoconConfig(ACMODES[language], CLC[language]), "default") { voconRecognizer, isSuccess ->
            if (isSuccess) {
                loadGrammar(checkNotNull(voconRecognizer), voconContext, deferred)
            } else {
                deferred.completeExceptionally(NuanceVoiceTriggerException("Failed to initialize VoconRecognizer"))
            }
        }
    }


    private fun loadGrammar(
        recognizer: VoconRecognizer,
        voconContext: VoconContext,
        deferred: CompletableDeferred<VoconRecognizer>
    ) {
        recognizer.loadGrammar(
            Grammar.createGrammar(listOf(voconContext), null),
            object : VoconRecognizer.RebuildListener {
                override fun onComplete(
                    grammar: Grammar?,
                    skippedWord: MutableList<VoconRecognizer.RebuildListener.SkippedWord>?
                ) {
                    recognizer.saveState()
                    deferred.complete(recognizer)
                }

                override fun onError(error: VoconError) {
                    recognizer.release()
                    deferred.completeExceptionally(NuanceVoiceTriggerException(error.reason, error.reasonCode))
                }
            }
        )
    }

}