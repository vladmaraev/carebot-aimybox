package com.justai.aimybox.smartscreen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager
import com.justai.aimybox.Aimybox
import com.justai.aimybox.api.aimybox.AimyboxDialogApi
import com.justai.aimybox.smartscreen.extensions.onRippleClick
import com.justai.aimybox.smartscreen.fragment.AimyboxFragment
import com.justai.aimybox.smartscreen.fragment.ScreensaverFragment
import com.justai.aimybox.smartscreen.fragment.AssistantFragment
import com.justai.aimybox.smartscreen.fragment.NetworkFragment
import com.justai.aimybox.smartscreen.ui.AimyboxButton
import com.justai.aimybox.smartscreen.ui.WaveView
import kotlinx.coroutines.*

class HomeActivity: AppCompatActivity(), CoroutineScope {

    override val coroutineContext = Dispatchers.Main

    private lateinit var button: AimyboxButton
    private lateinit var waveView: WaveView
    private lateinit var viewModel: AssistantViewModel
    private lateinit var audioManager: AudioManager
    private lateinit var settingsJob: Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_layout)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val application = application as AssistantApplication

        if (!::viewModel.isInitialized) {
            viewModel =
                ViewModelProviders.of(this, application.getViewModelFactory())
                    .get(AssistantViewModel::class.java)

            viewModel.isAssistantVisible
                .observe(this, Observer { toggleAssistantFragment() })

            viewModel.aimyboxState
                .observe(this, Observer(::onAimyboxStateChanged))
        }

        waveView = findViewById(R.id.wave)
        waveView.addDefaultWaves(2, 1)

        button = findViewById(R.id.aimybox_button)
        button.observeEvents(viewModel.aimybox)
        button.onRippleClick {
            toggleRecognition()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.aimybox.mute()
    }

    override fun onResume() {
        super.onResume()
        viewModel.aimybox.unmute()
        showScreenSaver()

        if (checkPermissions()) {
            launch {
                delay(3000)

                if (checkInternetConnection()) {
                    checkAimyboxConnection()
                } else {
                    startService(Intent(this@HomeActivity, NetworkService::class.java))
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == 132) {
            settingsJob = launch {
                delay(2000)
                showAimyboxView()
            }
            return true
        }
        return false
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            131 -> toggleRecognition()
            132 -> settingsJob.cancel()
            24 -> audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            25 -> audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
        }
        return true
    }

    override fun onBackPressed() {
        viewModel.aimybox.standby()
        val isVisible = viewModel.isAssistantVisible.value ?: false

        if (isVisible) {
            toggleAssistant(false)
            resetSession()
        } else {
            super.onBackPressed()
        }
    }

    private fun toggleMute() {
        if (viewModel.aimybox.isMuted) {
            viewModel.aimybox.unmute()
        } else {
            viewModel.aimybox.mute()
        }
    }

    private fun toggleRecognition() = viewModel.aimybox.toggleRecognition()

    private fun resetSession() {
        val api = viewModel.aimybox.config.dialogApi as? AimyboxDialogApi
        launch {
            api?.resetSession()
        }
    }

    private fun setFragment(fragment: Fragment) {
        val current = supportFragmentManager.findFragmentById(R.id.main_container)
        if (current == null || current::class != fragment::class) {
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.main_container, fragment)
                commitAllowingStateLoss()
            }
        }
    }

    private fun toggleAssistantFragment() {
        when (viewModel.isAssistantVisible.value) {
            true -> showAssistant()
            else -> showScreenSaver()
        }
    }

    private fun toggleAssistant(show: Boolean) = viewModel.toggleAssistantView(show)

    private fun showScreenSaver() = setFragment(ScreensaverFragment())

    private fun showAssistant() = setFragment(AssistantFragment())

    private fun showNetworkView() = setFragment(NetworkFragment())

    private fun showAimyboxView() = setFragment(AimyboxFragment())

    private fun checkPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (!permissions.map { checkSelfPermission(it) }.all { it == PackageManager.PERMISSION_GRANTED }) {
            requestPermissions(permissions, 1)
            return false
        }

        return true
    }

    private fun checkInternetConnection(): Boolean {
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val isConnected = manager.activeNetworkInfo?.isConnected ?: false
        if (!isConnected) {
            showNetworkView()
        }
        return isConnected
    }

    private fun checkAimyboxConnection(): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (preferences.getString(Preferences.AIMYBOX_KEY, null).isNullOrEmpty()) {
            showAimyboxView()
            return false
        }

        return true
    }

    private fun onAimyboxStateChanged(state: Aimybox.State) {
        toggleButton(state)
        toggleWave(state)
    }

    private fun toggleWave(state: Aimybox.State) {
        when (state) {
            Aimybox.State.PROCESSING -> waveView.startAnimation()
            else -> waveView.stopAnimation()
        }
    }

    private fun toggleButton(state: Aimybox.State) {
        when (state) {
            Aimybox.State.LISTENING -> button.visibility = View.VISIBLE
            else -> button.visibility = View.GONE
        }
    }
}