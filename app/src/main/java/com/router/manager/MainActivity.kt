package com.router.manager

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import java.io.File

class MainActivity : android.app.Activity() {

    private lateinit var webView: WebView
    private var currentRouter: RouterStore.Router? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        loadCurrentRouter()
    }

    override fun onResume() {
        super.onResume()
        // 从管理页面返回时，可能切换了路由器
        val currentId = RouterStore.getCurrentRouterId(this)
        if (currentRouter?.id != currentId) {
            loadCurrentRouter()
        }
    }

    private fun loadCurrentRouter() {
        val routers = RouterStore.loadRouters(this)
        val currentId = RouterStore.getCurrentRouterId(this)
        currentRouter = routers.find { it.id == currentId } ?: routers.firstOrNull()

        if (currentRouter != null) {
            RouterStore.setCurrentRouterId(this, currentRouter!!.id)
            actionBar?.title = currentRouter!!.name
            webView.loadUrl(currentRouter!!.url)
        } else {
            actionBar?.title = "路由器管理"
            webView.loadUrl("about:blank")
            RouterEditor.showEditDialog(this, null, false) { router ->
                val list = RouterStore.loadRouters(this)
                list.add(router)
                RouterStore.saveRouters(this, list)
                RouterStore.setCurrentRouterId(this, router.id)
                loadCurrentRouter()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        RouterEditor.handleActivityResult(this, requestCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 1, 0, "管理")?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu?.add(0, 2, 1, "添加路由器")
        menu?.add(0, 3, 2, "删除当前路由器")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> {
                startActivity(Intent(this, RouterListActivity::class.java))
            }
            2 -> {
                RouterEditor.showEditDialog(this, null, true) { router ->
                    val list = RouterStore.loadRouters(this)
                    list.add(router)
                    RouterStore.saveRouters(this, list)
                    RouterStore.setCurrentRouterId(this, router.id)
                    loadCurrentRouter()
                    Toast.makeText(this, "已添加「${router.name}」", Toast.LENGTH_SHORT).show()
                }
            }
            3 -> {
                val cur = currentRouter ?: return true
                android.app.AlertDialog.Builder(this)
                    .setTitle("删除路由器")
                    .setMessage("确定要删除「${cur.name}」吗？")
                    .setPositiveButton("删除") { _, _ ->
                        cur.customIconPath?.let { File(it).delete() }
                        val list = RouterStore.loadRouters(this)
                        list.removeAll { it.id == cur.id }
                        RouterStore.saveRouters(this, list)
                        RouterStore.setCurrentRouterId(this, list.firstOrNull()?.id)
                        loadCurrentRouter()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
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
