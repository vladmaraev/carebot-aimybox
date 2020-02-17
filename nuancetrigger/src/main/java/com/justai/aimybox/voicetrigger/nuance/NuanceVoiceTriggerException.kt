package com.justai.aimybox.voicetrigger.nuance

internal class NuanceVoiceTriggerException(message: String, code: Int? = null) : RuntimeException(
    if (code != null) "[$code] $message" else message
)