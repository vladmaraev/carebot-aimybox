package com.justai.aimybox.smartscreen

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.util.Log

class NetworkService: Service() {

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        registerReceiver(NetworkStateReceiver(), IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"))
        return START_STICKY
    }

    class NetworkStateReceiver: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("NetworkStateReceiver", "onReceive $intent")

            val manager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val isConnected = manager.activeNetworkInfo?.isConnected ?: false
            if (isConnected) {
                context.startActivity(Intent(context, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                context.unregisterReceiver(this)
            }
        }
    }
}