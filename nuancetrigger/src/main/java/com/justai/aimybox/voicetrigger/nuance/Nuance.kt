package com.justai.aimybox.voicetrigger.nuance

import com.justai.aimybox.logging.Logger

internal val L = Logger("NuanceSpeechkit")

class Nuance private constructor(){
    enum class Language(internal val tag: String) {
        RU("rus-RUS"), EN("eng-USA")
    }
}
