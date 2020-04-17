package com.justai.aimybox.smartscreen.skill

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import com.github.salomonbrys.kotson.array
import com.github.salomonbrys.kotson.obj
import com.github.salomonbrys.kotson.string
import com.justai.aimybox.Aimybox
import com.justai.aimybox.api.aimybox.AimyboxRequest
import com.justai.aimybox.api.aimybox.AimyboxResponse
import com.justai.aimybox.core.CustomSkill
import com.justai.aimybox.model.Response
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlin.coroutines.CoroutineContext

class MusicSkill: CustomSkill<AimyboxRequest, AimyboxResponse>, CoroutineScope {

    override val coroutineContext = Dispatchers.IO + Job()

    private val playList = mutableListOf<String>()

    private val player = MediaPlayer().apply {
        setOnCompletionListener {
            if (playList.isNotEmpty()) {
                play(playList.removeAt(0))
            }
        }

        setOnPreparedListener {
            it.start()
        }

        setOnErrorListener { mp, what, extra ->

            Log.d("MusicSkill", "Error $what $extra")
            true
        }
    }

    override fun canHandle(response: AimyboxResponse)
            = response.intent?.startsWith("/Music/") == true

    override suspend fun onResponse(
        response: AimyboxResponse,
        aimybox: Aimybox,
        defaultHandler: suspend (Response) -> Unit
    ) {
        when (response.action) {
            "musicOn" -> musicOn(response)
        }

        aimybox.standby()

        val subscription = aimybox.stateChannel.openSubscription()
        coroutineScope {
            launch {
                subscription.consumeEach { state ->
                    if (state == Aimybox.State.LISTENING) {
                        player.reset()
                        coroutineContext.cancelChildren()
                    }
                }
            }.invokeOnCompletion {
                subscription.cancel()
            }
        }
    }

    private fun musicOn(response: AimyboxResponse) {
        response.data?.let { data ->
            playList.clear()
            playList.addAll(data["musicList"].array.map { it.obj["stream"].string })
            play(data["stream"].string)
        }
    }

    private fun play(url: String) {
        launch(Dispatchers.Main) {
            player.reset()
            player.setDataSource(url)
            player.prepareAsync()
        }
    }
}