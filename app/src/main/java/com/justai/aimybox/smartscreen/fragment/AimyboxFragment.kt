package com.justai.aimybox.smartscreen.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.justai.aimybox.smartscreen.Preferences
import com.justai.aimybox.smartscreen.R

class AimyboxFragment: Fragment(R.layout.aimybox_layout) {

    private val webClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            if (url?.contains("/projects/") == true) {
                view?.evaluateJavascript("(function(){return document.getElementsByName('project-key')[0].getAttribute('content')})()") {
                    complete(it.substring(1, it.length - 1))
                }
            }
        }
    }

    private fun complete(key: String) {
        Log.d("AimyboxFragment", key)
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putString(Preferences.AIMYBOX_KEY, key)
            .apply()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val webView = view.findViewById<WebView>(R.id.web)
        webView.webViewClient = webClient
        webView.settings.javaScriptEnabled = true
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()

        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()

        view.findViewById<Button>(R.id.select_project_btn).setOnClickListener {
            webView.visibility = View.VISIBLE
            webView.loadUrl("https://app.aimybox.com/projects")
        }
    }
}