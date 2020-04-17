package com.justai.aimybox.smartscreen.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.VideoView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.justai.aimybox.Aimybox
import com.justai.aimybox.smartscreen.AssistantApplication
import com.justai.aimybox.smartscreen.AssistantViewModel
import com.justai.aimybox.smartscreen.Preferences
import com.justai.aimybox.smartscreen.R
import java.text.SimpleDateFormat
import java.util.*

class ScreensaverFragment: Fragment(R.layout.screensaver_layout) {

    companion object {
        private val timeFormat = SimpleDateFormat("HH:mm", Preferences.defaultLocale)
        private val dateFormat = SimpleDateFormat("EEE, dd MMM", Preferences.defaultLocale)
    }

    private val tickReceiver = TickReceiver()

    private lateinit var viewModel: AssistantViewModel
    private lateinit var clockView: TextView
    private lateinit var dateView: TextView
    private lateinit var promptView: TextView
    private lateinit var videoView: VideoView

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val aimyboxProvider = requireActivity().application as AssistantApplication

        if (!::viewModel.isInitialized) {
            viewModel =
                ViewModelProviders.of(requireActivity(), aimyboxProvider.getViewModelFactory())
                    .get(AssistantViewModel::class.java)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.apply {
            clockView = findViewById(R.id.time)
            dateView = findViewById(R.id.date)
            promptView = findViewById(R.id.prompt)
            videoView = findViewById(R.id.video)

            refreshDateTime()
            startVideo()
        }

        clockView.text = timeFormat.format(Date())
        context?.registerReceiver(tickReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
        viewModel.aimyboxState.observe(viewLifecycleOwner, Observer(::onAimyboxStateChanged))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        context?.unregisterReceiver(tickReceiver)
    }

    private fun refreshDateTime() {
        val now = Date()
        clockView.text = timeFormat.format(now)
        dateView.text = dateFormat.format(now)
    }

    private fun startVideo() {
        videoView.setVideoPath("android.resource://${context?.packageName}/${R.raw.background}")
        videoView.setOnCompletionListener { it.start() }
        videoView.start()
    }

    private fun onAimyboxStateChanged(state: Aimybox.State) {
        togglePrompt(state)
    }

    private fun togglePrompt(state: Aimybox.State) {
        when (state) {
            Aimybox.State.STANDBY -> promptView.animate().alpha(1f)
            else -> promptView.animate().alpha(0f)
        }
    }

    inner class TickReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshDateTime()
        }
    }
}