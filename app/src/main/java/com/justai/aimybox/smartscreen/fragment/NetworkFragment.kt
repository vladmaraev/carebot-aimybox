package com.justai.aimybox.smartscreen.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.justai.aimybox.smartscreen.R

class NetworkFragment: Fragment(R.layout.network_layout) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.open_settings_btn).setOnClickListener {
            startActivity(Intent("android.settings.WIFI_SETTINGS"))
        }
    }
}