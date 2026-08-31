package com.router.manager

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
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
import android.widget.Toast
import java.io.File

class MainActivity : android.app.Activity() {

    companion object {
        private const val TAB_HOME = 0
        private const val TAB_MULTI = 1
        private const val TAB_WEB = 2
    }

    private lateinit var contentContainer: FrameLayout
    private lateinit var managerView: RouterManagerView
    private lateinit var multiTaskView: MultiTaskView
    private lateinit var bottomNav: LinearLayout

    /** WebView 池：路由器ID -> WebView，切换时不销毁，保持登录状态 */
    private val webViewPool = mutableMapOf<String, WebView>()

    /** 已打开的路由器（多任务列表） */
    private val openedRouters = mutableListOf<RouterStore.Router>()

    /** 当前显示的路由器ID，null 表示在首页/多任务页 */
    private var currentRouterId: String? = null

    /** 当前标签页 */
    private var currentTab = TAB_HOME

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F0F2F5"))
        }

        // 内容区
        contentContainer = FrameLayout(this)
        root.addView(
            contentContainer,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )

        // 底部导航
        bottomNav = createBottomNav()
        root.addView(bottomNav)

        setContentView(root)

        // 初始化页面
        managerView = RouterManagerView(this).apply {
            onRouterClick = { router -> openRouter(router) }
            onRouterEdit = { router -> editRouter(router) }
            onRouterDelete = { router -> confirmDeleteRouter(router) }
        }

        multiTaskView = MultiTaskView(this).apply {
            onRouterClick = { router -> openRouter(router) }
            onRouterClose = { router -> closeRouter(router) }
        }

        switchTab(TAB_HOME)
    }

    // ─── 底部导航 ────────────────────────────────────────

    private fun createBottomNav(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(28).toFloat()
                setColor(Color.WHITE)
            }
            elevation = dp(8).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            ).apply {
                marginStart = dp(16)
                marginEnd = dp(16)
                bottomMargin = dp(14)
            }

            addView(createNavButton(R.drawable.ic_nav_home, TAB_HOME))
            addView(createNavButton(R.drawable.ic_nav_windows, TAB_MULTI))
            addView(createNavButton(R.drawable.ic_nav_add, -1)) // -1 表示操作按钮，不选中
        }
    }

    private fun createNavButton(iconRes: Int, tab: Int): View {
        val isSelected = (currentTab == tab)

        val container = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f
            )
            gravity = Gravity.CENTER
        }

        // 选中背景（圆形）
        val bg = View(this).apply {
            val size = dp(44)
            layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
            background = if (isSelected) {
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#BBDEFB"))
                }
            } else null
        }

        val icon = ImageView(this).apply {
            val size = dp(24)
            layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
            setImageResource(iconRes)
            setColorFilter(
                if (isSelected) Color.parseColor("#1565C0")
                else Color.parseColor("#9E9E9E"),
                PorterDuff.Mode.SRC_ATOP
            )
        }

        container.addView(bg)
        container.addView(icon)

        container.setOnClickListener {
            if (tab == -1) {
                addRouter()
            } else {
                switchTab(tab)
            }
        }

        return container
    }

    private fun updateBottomNav() {
        bottomNav.removeAllViews()
        bottomNav.addView(createNavButton(R.drawable.ic_nav_home, TAB_HOME))
        bottomNav.addView(createNavButton(R.drawable.ic_nav_windows, TAB_MULTI))
        bottomNav.addView(createNavButton(R.drawable.ic_nav_add, -1))
    }

    // ─── 页面切换 ────────────────────────────────────────

    private fun switchTab(tab: Int) {
        currentTab = tab
        currentRouterId = null
        contentContainer.removeAllViews()

        when (tab) {
            TAB_HOME -> {
                managerView.refresh()
                contentContainer.addView(managerView)
            }
            TAB_MULTI -> {
                multiTaskView.update(openedRouters, null)
                contentContainer.addView(multiTaskView)
            }
        }
        updateBottomNav()
    }

    private fun openRouter(router: RouterStore.Router) {
        currentRouterId = router.id
        currentTab = TAB_WEB

        val webView = webViewPool.getOrPut(router.id) {
            createWebView(router)
        }
        if (openedRouters.none { it.id == router.id }) {
            openedRouters.add(router)
        }

        contentContainer.removeAllViews()
        contentContainer.addView(webView)
        updateBottomNav()
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

    /** 关闭一个已打开的 WebView */
    private fun closeRouter(router: RouterStore.Router) {
        webViewPool.remove(router.id)?.destroy()
        openedRouters.removeAll { it.id == router.id }
        if (currentRouterId == router.id) {
            if (openedRouters.isNotEmpty()) {
                openRouter(openedRouters.first())
            } else {
                switchTab(TAB_HOME)
            }
        } else if (currentTab == TAB_MULTI) {
            multiTaskView.update(openedRouters, null)
        }
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

            val openedIdx = openedRouters.indexOfFirst { it.id == router.id }
            if (openedIdx >= 0) openedRouters[openedIdx] = updated

            managerView.refresh()
            if (currentTab == TAB_MULTI) {
                multiTaskView.update(openedRouters, currentRouterId)
            }
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteRouter(router: RouterStore.Router) {
        AlertDialog.Builder(this)
            .setTitle("删除路由器")
            .setMessage("确定要删除「${router.name}」吗？已打开的管理界面也会关闭。")
            .setPositiveButton("删除") { _, _ ->
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
                        switchTab(TAB_HOME)
                    }
                }
                managerView.refresh()
                Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ─── 返回键 / 生命周期 ───────────────────────────────

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (currentTab == TAB_WEB && currentRouterId != null) {
            val webView = webViewPool[currentRouterId]
            if (webView?.canGoBack() == true) {
                webView.goBack()
            } else {
                switchTab(TAB_HOME)
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
