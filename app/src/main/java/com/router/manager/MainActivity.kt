package com.router.manager

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import android.app.AlertDialog

class MainActivity : android.app.Activity() {

    private lateinit var webView: WebView
    private lateinit var prefs: SharedPreferences

    companion object {
        private const val PREFS_NAME = "router_manager_prefs"
        private const val KEY_SERVER_URL = "server_url"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 根布局
        val root = FrameLayout(this)
        webView = WebView(this)
        root.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)

        // WebView 配置
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "无法连接服务器，请检查地址或网络",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
        webView.webChromeClient = WebChromeClient()

        // 读取已保存的地址
        val savedUrl = prefs.getString(KEY_SERVER_URL, null)
        if (savedUrl.isNullOrEmpty()) {
            showServerDialog(cancelable = false)
        } else {
            webView.loadUrl(savedUrl)
        }
    }

    /**
     * 弹出服务器地址输入框
     */
    private fun showServerDialog(cancelable: Boolean) {
        val input = EditText(this).apply {
            hint = "例如：http://192.168.1.1:3000"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            setText(prefs.getString(KEY_SERVER_URL, ""))
            setSelection(text.length)
            val pad = dp(16)
            setPadding(pad, pad, pad, pad)
        }

        val container = FrameLayout(this).apply {
            val pad = dp(20)
            setPadding(pad, 0, pad, 0)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("设置服务器地址")
            .setMessage("请输入路由器管理平台的访问地址（含 http:// 或 https://）")
            .setView(container)
            .setCancelable(cancelable)
            .setPositiveButton("确定") { _, _ ->
                var url = input.text.toString().trim()
                if (url.isNotEmpty()) {
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "http://$url"
                    }
                    prefs.edit().putString(KEY_SERVER_URL, url).apply()
                    webView.loadUrl(url)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 1, 0, "设置服务器地址")?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            showServerDialog(cancelable = true)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
