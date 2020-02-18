package com.justai.aimybox.smartscreen

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.justai.aimybox.Aimybox
import com.justai.aimybox.api.aimybox.AimyboxDialogApi
import com.justai.aimybox.smartscreen.extensions.onRippleClick
import com.justai.aimybox.smartscreen.fragment.ScreensaverFragment
import com.justai.aimybox.smartscreen.fragment.AssistantFragment
import com.justai.aimybox.smartscreen.ui.AimyboxButton
import com.justai.aimybox.smartscreen.ui.WaveView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeActivity: AppCompatActivity(), CoroutineScope {

    override val coroutineContext = Dispatchers.Main

    private lateinit var button: AimyboxButton
    private lateinit var waveView: WaveView
    private lateinit var viewModel: AssistantViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_layout)

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
            viewModel.aimybox.toggleRecognition()
        }

        requestPermissions()
        toggleAssistant(false)
    }

    override fun onPause() {
        super.onPause()
        viewModel.aimybox.mute()
    }

    override fun onResume() {
        super.onResume()
        viewModel.aimybox.unmute()
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

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE),
            1
        )
    }

    private fun onAimyboxStateChanged(state: Aimybox.State) {
        toggleWave(state)
    }

    private fun toggleWave(state: Aimybox.State) {
        when (state) {
            Aimybox.State.PROCESSING -> waveView.startAnimation()
            else -> waveView.stopAnimation()
        }
    }
}