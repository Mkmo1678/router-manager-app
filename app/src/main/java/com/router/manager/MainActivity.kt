package com.router.manager

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.File

class MainActivity : android.app.Activity() {

    private lateinit var container: FrameLayout
    private lateinit var managerView: RouterManagerView

    /** WebView 池：路由器ID -> WebView，切换时不销毁，保持登录状态 */
    private val webViewPool = mutableMapOf<String, WebView>()

    /** 已打开的路由器（多任务列表） */
    private val openedRouters = mutableListOf<RouterStore.Router>()

    /** 当前显示的路由器ID，null 表示在管理页面 */
    private var currentRouterId: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        container = FrameLayout(this)
        setContentView(container)

        managerView = RouterManagerView(this)
        managerView.onRouterClick = { router -> openRouter(router) }
        managerView.onRouterEdit = { router -> editRouter(router) }
        managerView.onRouterDelete = { router -> confirmDeleteRouter(router) }

        showManagerPage()
    }

    // ─── 页面切换 ────────────────────────────────────────

    private fun showManagerPage() {
        currentRouterId = null
        container.removeAllViews()
        managerView.refresh()
        container.addView(managerView)
        actionBar?.title = "路由器管理"
        invalidateOptionsMenu()
    }

    private fun openRouter(router: RouterStore.Router) {
        currentRouterId = router.id
        container.removeAllViews()

        val webView = webViewPool.getOrPut(router.id) {
            createWebView(router)
        }
        if (openedRouters.none { it.id == router.id }) {
            openedRouters.add(router)
        }
        container.addView(webView)
        actionBar?.title = router.name
        invalidateOptionsMenu()
    }

    private fun createWebView(router: RouterStore.Router): WebView {
        return WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
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
            webViewClient = object : WebViewClient() {
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
            webChromeClient = WebChromeClient()
            loadUrl(router.url)
        }
    }

    /** 关闭一个已打开的 WebView（从池中移除并销毁） */
    private fun closeRouter(router: RouterStore.Router) {
        webViewPool.remove(router.id)?.destroy()
        openedRouters.removeAll { it.id == router.id }
        if (currentRouterId == router.id) {
            if (openedRouters.isNotEmpty()) {
                openRouter(openedRouters.first())
            } else {
                showManagerPage()
            }
        }
    }

    // ─── 多任务切换界面 ──────────────────────────────────

    private fun showMultiTaskDialog() {
        if (openedRouters.isEmpty()) {
            Toast.makeText(this, "暂无已打开的管理界面", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(8)
            setPadding(pad, pad, pad, pad)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("多任务切换（${openedRouters.size} 个已打开）")
            .setView(dialogView)
            .setNegativeButton("返回管理") { _, _ -> showManagerPage() }
            .create()

        for (router in openedRouters) {
            dialogView.addView(createMultiTaskItem(router, dialog))
        }

        dialog.show()
    }

    private fun createMultiTaskItem(
        router: RouterStore.Router,
        dialog: AlertDialog
    ): View {
        val isCurrent = router.id == currentRouterId

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            if (isCurrent) {
                setBackgroundColor(0xFFE3F2FD.toInt())
            }
        }

        val icon = ImageView(this).apply {
            val size = dp(36)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(12)
            }
            setImageDrawable(RouterStore.getIconDrawable(this@MainActivity, router))
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
        }

        val nameText = TextView(this).apply {
            text = if (isCurrent) "✓ ${router.name}" else router.name
            textSize = 15f
            setTextColor(0xFF222222.toInt())
            setTypeface(null, Typeface.BOLD)
        }

        val urlText = TextView(this).apply {
            text = router.url
            textSize = 11f
            setTextColor(0xFF888888.toInt())
        }

        textLayout.addView(nameText)
        textLayout.addView(urlText)

        val closeBtn = TextView(this).apply {
            text = "✕"
            textSize = 18f
            setTextColor(0xFF999999.toInt())
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener {
                closeRouter(router)
                dialog.dismiss()
            }
        }

        layout.setOnClickListener {
            openRouter(router)
            dialog.dismiss()
        }

        layout.addView(icon)
        layout.addView(textLayout)
        layout.addView(closeBtn)

        return layout
    }

    // ─── 路由器增删改 ────────────────────────────────────

    private fun addRouter() {
        RouterEditor.showEditDialog(this, null, true) { router ->
            val list = RouterStore.loadRouters(this)
            list.add(router)
            RouterStore.saveRouters(this, list)
            managerView.refresh()
            Toast.makeText(this, "已添加「${router.name}」", Toast.LENGTH_SHORT).show()
        }
    }

    private fun editRouter(router: RouterStore.Router) {
        RouterEditor.showEditDialog(this, router, true) { updated ->
            val list = RouterStore.loadRouters(this)
            val idx = list.indexOfFirst { it.id == router.id }
            if (idx >= 0) list[idx] = updated
            RouterStore.saveRouters(this, list)

            // 更新已打开列表中的信息
            val openedIdx = openedRouters.indexOfFirst { it.id == router.id }
            if (openedIdx >= 0) openedRouters[openedIdx] = updated

            // 如果是当前显示的，更新标题（不重新加载，保留登录状态）
            if (currentRouterId == router.id) {
                actionBar?.title = updated.name
            }

            managerView.refresh()
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteRouter(router: RouterStore.Router) {
        AlertDialog.Builder(this)
            .setTitle("删除路由器")
            .setMessage("确定要删除「${router.name}」吗？已打开的管理界面也会关闭。")
            .setPositiveButton("删除") { _, _ ->
                // 关闭 WebView
                if (webViewPool.containsKey(router.id)) {
                    webViewPool.remove(router.id)?.destroy()
                    openedRouters.removeAll { it.id == router.id }
                }
                router.customIconPath?.let { File(it).delete() }

                val list = RouterStore.loadRouters(this)
                list.removeAll { it.id == router.id }
                RouterStore.saveRouters(this, list)

                if (currentRouterId == router.id) {
                    if (openedRouters.isNotEmpty()) {
                        openRouter(openedRouters.first())
                    } else {
                        showManagerPage()
                    }
                }
                managerView.refresh()
                Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ─── 菜单 ────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (currentRouterId == null) {
            // 管理页面
            menu?.add(0, 1, 0, "添加")?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            if (openedRouters.isNotEmpty()) {
                menu?.add(0, 2, 1, "多任务")
            }
        } else {
            // WebView 管理界面
            menu?.add(0, 1, 0, "管理")?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu?.add(0, 2, 1, "窗口")?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu?.add(0, 3, 2, "添加路由器")
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> {
                if (currentRouterId == null) {
                    addRouter()
                } else {
                    showManagerPage()
                }
            }
            2 -> showMultiTaskDialog()
            3 -> addRouter()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    // ─── 返回键 / 生命周期 ───────────────────────────────

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (currentRouterId != null) {
            val webView = webViewPool[currentRouterId]
            if (webView?.canGoBack() == true) {
                webView.goBack()
            } else {
                showManagerPage()
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        RouterEditor.handleActivityResult(this, requestCode, resultCode, data)
    }

    override fun onDestroy() {
        webViewPool.values.forEach { it.destroy() }
        webViewPool.clear()
        super.onDestroy()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
