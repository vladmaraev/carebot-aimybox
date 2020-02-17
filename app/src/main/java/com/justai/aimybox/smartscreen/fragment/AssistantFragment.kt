package com.justai.aimybox.smartscreen.fragment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.justai.aimybox.Aimybox
import com.justai.aimybox.components.extensions.startActivityIfExist
import com.justai.aimybox.components.widget.*
import com.justai.aimybox.components.widget.Button
import com.justai.aimybox.model.Response
import com.justai.aimybox.model.reply.ButtonsReply
import com.justai.aimybox.model.reply.ImageReply
import com.justai.aimybox.model.reply.Reply
import com.justai.aimybox.model.reply.TextReply
import com.justai.aimybox.smartscreen.AssistantApplication
import com.justai.aimybox.smartscreen.AssistantViewModel
import com.justai.aimybox.smartscreen.R
import com.justai.aimybox.smartscreen.extensions.asWidget
import com.justai.aimybox.smartscreen.extensions.isVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.CoroutineContext

class AssistantFragment: Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + Job()

    private val replies = LinkedList<Reply>()

    private lateinit var viewModel: AssistantViewModel
    private lateinit var videoView: PlayerView
    private lateinit var videoPlayer: ExoPlayer
    private lateinit var imageView: ImageView
    private lateinit var buttonsLayout: LinearLayout
    private lateinit var responseLayout: LinearLayout

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val application = requireActivity().application as AssistantApplication

        if (!::viewModel.isInitialized) {
            viewModel =
                ViewModelProviders.of(requireActivity(), application.getViewModelFactory())
                    .get(AssistantViewModel::class.java)
        }
    }

    override fun onDetach() {
        super.onDetach()
        videoPlayer.release()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) =
        inflater.inflate(R.layout.assistant_layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.apply {
            videoView = findViewById(R.id.video)
            imageView = findViewById(R.id.image)
            buttonsLayout = findViewById(R.id.buttons)
            responseLayout = findViewById(R.id.response)
        }

        viewModel.response.observe(viewLifecycleOwner, Observer(::onResponse))
        viewModel.aimyboxState.observe(viewLifecycleOwner, Observer(::onAimyboxState))

        videoPlayer = SimpleExoPlayer.Builder(context!!).build()
        videoPlayer.addListener(videoListener)
        videoView.player = videoPlayer

        launch {
            viewModel.urlIntents.consumeEach {
                context?.startActivityIfExist(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
            }
        }
    }

    private fun onAimyboxState(state: Aimybox.State) {
        when(state) {
            Aimybox.State.LISTENING -> toggleVideo(false)
            Aimybox.State.STANDBY -> toggleVideo(true)
        }
    }

    private fun toggleVideo(play: Boolean) {
        videoPlayer.playWhenReady = play
    }

    private fun onResponse(response: Response) {
        videoPlayer.stop()
        clearResponse()

        replies.run {
            clear()
            addAll(response.replies)
        }

        showNextReply()
    }

    private fun clearResponse() {
        responseLayout.removeAllViews()
        buttonsLayout.removeAllViews()
    }

    private fun onResponseCompleted() {
        val response = viewModel.response.value
        if (response?.question == true) {
            viewModel.aimybox.startRecognition()
        } else {
            viewModel.toggleAssistantView(false)
        }
    }

    private fun showNextReply() {
        val reply = replies.poll()?.also { showReply(it) }

        if (reply == null && viewModel.aimyboxState.value == Aimybox.State.STANDBY) {
            onResponseCompleted()
        }
    }

    private fun showReply(reply: Reply) {
        when(reply) {
            is TextReply -> showText(reply.asWidget)
            is ButtonsReply -> showButtons(reply.asWidget)
            is ImageReply -> showImage(reply.asWidget)
            else -> showNextReply()
        }
    }

    private fun showImage(widget: ImageWidget) {
        if (widget.isVideo) {
            playVideo(widget.url)
        } else {
            showImage(widget.url)
            showNextReply()
        }
    }

    private fun playVideo(url: String) {
        imageView.setImageDrawable(null)
        val factory = DefaultDataSourceFactory(context, Util.getUserAgent(context!!, "${context?.packageName}"))
        val source = ProgressiveMediaSource.Factory(factory)
            .createMediaSource(Uri.parse(url))
        videoPlayer.prepare(source)
        toggleVideo(true)
    }

    private fun onVideoEnded() = showNextReply()

    private fun showImage(url: String) {
        Glide.with(this)
            .load(url)
            .into(imageView)
    }

    private fun showText(widget: ResponseWidget) {
        layoutInflater
            .inflate(R.layout.item_response, null)
            .apply {
                check(this is TextView)
                text = widget.text
                responseLayout.addView(this)
            }

        showNextReply()
    }

    private fun showButtons(widget: ButtonsWidget) {
        widget.buttons.forEach { addButton(it) }
        showNextReply()
    }

    private fun addButton(button: Button) {
        layoutInflater
            .inflate(com.justai.aimybox.components.R.layout.item_button, null)
            .apply {
                check(this is TextView)
                text = button.text

                setOnClickListener {
                    if (button !is LinkButton) {
                        clearResponse()
                    }
                    viewModel.onButtonClick(button)
                }

                buttonsLayout.addView(this)
            }
    }

    private val videoListener = object : Player.EventListener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            when(videoPlayer.playbackState) {
                Player.STATE_ENDED -> onVideoEnded()
            }
        }
    }
}