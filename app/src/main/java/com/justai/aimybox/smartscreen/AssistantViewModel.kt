package com.justai.aimybox.smartscreen


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.justai.aimybox.Aimybox
import com.justai.aimybox.api.DialogApi
import com.justai.aimybox.api.aimybox.AimyboxDialogApi
import com.justai.aimybox.components.widget.*
import com.justai.aimybox.model.Response
import com.justai.aimybox.model.reply.ButtonsReply
import com.justai.aimybox.model.reply.ImageReply
import com.justai.aimybox.smartscreen.AssistantApplication.Companion.unitId
import com.justai.aimybox.texttospeech.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.launch
import java.util.*

class AssistantViewModel(
    val aimybox: Aimybox
): ViewModel(), CoroutineScope by MainScope() {

    val aimyboxState = aimybox.stateChannel.toLiveData()

    private val isAssistantVisibleInternal = MutableLiveData<Boolean>()
    val isAssistantVisible = isAssistantVisibleInternal.immutable()

    private val urlIntentsInternal = Channel<String>()
    val urlIntents = urlIntentsInternal as ReceiveChannel<String>

    private val responseInternal = MutableLiveData<Response>()
    val response = responseInternal.immutable()

    init {
        launch {
            aimybox.dialogApiEvents.observe { onDialogApiEvent(it) }
            aimybox.textToSpeechEvents.observe{ onTextToSpeechEvent(it) }
        }
    }

    private fun onDialogApiEvent(event: DialogApi.Event) {
        when (event) {
            is DialogApi.Event.ResponseReceived -> processResponse(event.response)
        }
    }

    private fun onTextToSpeechEvent(event: TextToSpeech.Event) {
        when (event) {
            is TextToSpeech.Event.SpeechSequenceCompleted -> onSpeechSequenceCompleted()
        }
    }

    private fun onSpeechSequenceCompleted() {
        val response = responseInternal.value

        if (response?.question == false) {
            println("ZZZ_onSpeechSequenceCompleted_question=false")
            //val dialogApi = AimyboxDialogApi("cc96633d-59d4-4724-94bd-f5db2f02ad13", unitId, "https://bot-zaboty.herokuapp.com")
            //dialogApi.send("", aimybox)
            toggleAssistantView(false)
        }
    }

    private fun processResponse(response: Response) {
        responseInternal.value = response

        val buttons = response.replies
            .filterIsInstance(ButtonsReply::class.java)

        val image = response.replies
            .filterIsInstance(ImageReply::class.java)
            .firstOrNull()

        if (image != null || buttons.isNotEmpty()) {
            toggleAssistantView(true)
        } else if (response.replies.isEmpty() && response.question == false) {
            toggleAssistantView(false)
        }
    }

    fun onButtonClick(button: Button) {
        when (button) {
            is ResponseButton -> aimybox.sendRequest(button.text)
            is PayloadButton -> aimybox.sendRequest(button.payload)
            is LinkButton -> urlIntentsInternal.safeOffer(button.url)
        }
    }

    fun toggleAssistantView(show: Boolean) {
        if (isAssistantVisible.value != show) {
            isAssistantVisibleInternal.postValue(show)
        }
    }

    private fun <T> BroadcastChannel<T>.toLiveData(): LiveData<T> = MutableLiveData<T>().apply {
        observe { postValue(it) }
    }

    private fun <T> MutableLiveData<T>.immutable() = this as LiveData<T>

    private fun <T> BroadcastChannel<T>.observe(action: suspend (T) -> Unit) {
        val channel = openSubscription()
        launch {
            channel.consumeEach { action(it) }
        }.invokeOnCompletion { channel.cancel() }
    }

    private fun <T> SendChannel<T>.safeOffer(value: T) =
        takeUnless(SendChannel<T>::isClosedForSend)?.offer(value) ?: false

    class Factory private constructor(private val aimybox: Aimybox) : ViewModelProvider.Factory {

        companion object {
            private lateinit var instance: Factory

            fun getInstance(aimybox: Aimybox): Factory {
                if (!::instance.isInitialized) instance = Factory(aimybox)
                return instance
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            require(AssistantViewModel::class.java.isAssignableFrom(modelClass)) { "$modelClass is not a subclass of AssistantViewModel" }
            require(modelClass.constructors.size == 1) { "AssistantViewModel must have only one constructor" }
            val constructor = checkNotNull(modelClass.constructors[0])
            return constructor.newInstance(aimybox) as T
        }
    }
}