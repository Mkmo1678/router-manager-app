package com.router.manager

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebChromeClient.FileChooserParams
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.io.File

class MainActivity : android.app.Activity() {

    companion object {
        private const val TAB_HOME = 0
        private const val TAB_MULTI = 1
        private const val TAB_WEB = 2
        private const val TAB_ABOUT = 3
        private const val REQUEST_FILE_CHOOSER = 2001
    }

    private lateinit var contentContainer: FrameLayout
    private lateinit var managerView: RouterManagerView
    private lateinit var multiTaskView: MultiTaskView
    private lateinit var bottomNav: LinearLayout

    /** WebView 池：路由器ID -> WebView，切换时不销毁，保持登录状态 */
    private val webViewPool = mutableMapOf<String, WebView>()

    /** 跟踪每个 WebView 加载时的 URL，用于检测访问模式切换 */
    private val webViewUrls = mutableMapOf<String, String>()

    /** 当前管理界面的加载进度条 */
    private var webProgressBar: ProgressBar? = null

    /** 已打开的路由器（多任务列表） */
    private val openedRouters = mutableListOf<RouterStore.Router>()

    /** 当前显示的路由器ID，null 表示在首页/多任务页 */
    private var currentRouterId: String? = null

    /** 当前标签页 */
    private var currentTab = TAB_HOME

    private var statusBarHeight: Int = 0
    private var navBarHeight: Int = 0
    private var defaultUserAgent: String? = null

    /** 文件上传回调 */
    private var uploadMessage: ValueCallback<Array<Uri>>? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取系统栏高度
        statusBarHeight = getStatusBarHeight()
        navBarHeight = getNavBarHeight()

        // 沉浸式：内容延伸到状态栏和导航栏后面，状态栏图标颜色随背景自动切换
        applyImmersiveFlags()
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // 根布局用 FrameLayout，让 bottomNav 浮在内容上
        val root = FrameLayout(this)

        // 内容区（填满整个屏幕，包括状态栏和导航栏后面）
        contentContainer = FrameLayout(this)
        root.addView(
            contentContainer,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // 底部导航（浮在底部）
        bottomNav = createBottomNav()
        val bottomNavLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
            marginStart = dp(16)
            marginEnd = dp(16)
            bottomMargin = navBarHeight + dp(10)
        }
        root.addView(bottomNav, bottomNavLp)

        setContentView(root)

        // 初始化页面
        managerView = RouterManagerView(this, statusBarHeight).apply {
            onRouterClick = { router -> openRouter(router) }
            onRouterEdit = { router -> editRouter(router) }
            onRouterDelete = { router -> confirmDeleteRouter(router) }
            onSettingsClick = { showSettings() }
        }

        multiTaskView = MultiTaskView(this, statusBarHeight).apply {
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
            setPadding(dp(8), dp(8), dp(8), dp(8))
            // 毛玻璃效果：半透明白色 + 大圆角 + 柔和阴影
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(32).toFloat()
                setColor(Color.parseColor("#EBFFFFFF")) // 92% 不透明白色
            }
            elevation = dp(16).toFloat()
        }
    }

    private fun createNavButton(iconRes: Int, tab: Int): View {
        val isSelected = (currentTab == tab)
        val themeColor = AppSettings.getThemeColor(this)

        val container = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, dp(52), 1f
            )
        }

        // 选中背景（主题色圆形）
        val bg = View(this).apply {
            val size = dp(42)
            layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
            background = if (isSelected) {
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(themeColor)
                }
            } else null
        }

        val icon = ImageView(this).apply {
            val size = if (isSelected) dp(24) else dp(22)
            layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
            setImageResource(iconRes)
            setColorFilter(
                if (isSelected) Color.WHITE
                else Color.parseColor("#888888"),
                PorterDuff.Mode.SRC_ATOP
            )
        }

        container.addView(bg)
        container.addView(icon)

        container.setOnClickListener {
            when (tab) {
                TAB_HOME -> switchTab(TAB_HOME)
                TAB_MULTI -> switchTab(TAB_MULTI)
                TAB_ABOUT -> showAbout()
                else -> {
                    // 添加按钮
                    addRouter()
                }
            }
        }

        return container
    }

    private fun updateBottomNav() {
        bottomNav.removeAllViews()
        bottomNav.addView(createNavButton(R.drawable.ic_nav_home, TAB_HOME))
        bottomNav.addView(createNavButton(R.drawable.ic_nav_windows, TAB_MULTI))
        bottomNav.addView(createNavButton(R.drawable.ic_nav_add, -1)) // -1 = 添加
        bottomNav.addView(createNavButton(R.drawable.ic_about, TAB_ABOUT))
    }

    // ─── 页面切换 ────────────────────────────────────────

    private fun switchTab(tab: Int) {
        currentTab = tab
        currentRouterId = null
        webProgressBar = null
        contentContainer.removeAllViews()
        // 首页/多任务页显示底部导航
        bottomNav.visibility = View.VISIBLE

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

        // 如果访问地址变化（切换了本地/远程模式），销毁旧 WebView 重新创建
        val savedUrl = webViewUrls[router.id]
        if (savedUrl != null && savedUrl != router.currentUrl) {
            webViewPool.remove(router.id)?.destroy()
            webViewUrls.remove(router.id)
        }

        val webView = webViewPool.getOrPut(router.id) {
            createWebView(router)
        }
        webViewUrls[router.id] = router.currentUrl
        if (openedRouters.none { it.id == router.id }) {
            openedRouters.add(router)
        }

        contentContainer.removeAllViews()
        // 管理界面隐藏底部导航
        bottomNav.visibility = View.GONE

        // 管理界面容器：顶部半透明状态栏背景 + WebView（避免网页与状态栏重叠）
        val webContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // 状态栏背景条（半透明黑色，让状态栏图标始终清晰）
        val statusBarBg = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                statusBarHeight
            )
            setBackgroundColor(Color.parseColor("#99000000"))
        }

        // 顶部工具栏：刷新按钮 + 路由器名称 + 加载进度条
        val toolBar = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
            )
            setBackgroundColor(Color.parseColor("#99000000"))
        }

        // 刷新按钮
        val refreshBtn = ImageView(this).apply {
            val size = dp(36)
            layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER_VERTICAL).apply {
                marginStart = dp(8)
            }
            setImageResource(R.drawable.ic_refresh)
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
            setPadding(dp(6), dp(6), dp(6), dp(6))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#33FFFFFF"))
            }
            setOnClickListener {
                webView.reload()
                Toast.makeText(this@MainActivity, "正在刷新...", Toast.LENGTH_SHORT).show()
            }
        }

        // 路由器名称
        val titleText = TextView(this).apply {
            text = router.name
            textSize = 15f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }

        // 加载进度条
        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(4),
                Gravity.BOTTOM
            )
            max = 100
            progress = 0
            visibility = View.GONE
            val themeColor = AppSettings.getThemeColor(this@MainActivity)
            progressTintList = android.content.res.ColorStateList.valueOf(themeColor)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#33FFFFFF"))
        }
        webProgressBar = progressBar

        toolBar.addView(refreshBtn)
        toolBar.addView(titleText)
        toolBar.addView(progressBar)

        // 复用 WebView 前先从旧父容器移除
        (webView.parent as? ViewGroup)?.removeView(webView)

        webContainer.addView(statusBarBg)
        webContainer.addView(toolBar)
        webContainer.addView(webView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        contentContainer.addView(webContainer)
        updateBottomNav()
    }

    private fun createWebView(router: RouterStore.Router): WebView {
        return WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                // 保存默认手机 UA（仅第一次）
                if (defaultUserAgent == null) {
                    defaultUserAgent = userAgentString
                }
                userAgentString = AppSettings.getUserAgent(
                    this@MainActivity,
                    defaultUserAgent ?: userAgentString
                )
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

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // 自动填充已保存的用户名和密码
                    if (router.username.isNotEmpty() || router.password.isNotEmpty()) {
                        val user = escapeJs(router.username)
                        val pwd = escapeJs(router.password)
                        val js = """
                            (function() {
                                try {
                                    var user = '$user';
                                    var pwd = '$pwd';
                                    var pwdInputs = document.querySelectorAll('input[type=password]');
                                    if (pwdInputs.length > 0) {
                                        var p = pwdInputs[0];
                                        p.value = pwd;
                                        p.dispatchEvent(new Event('input', {bubbles:true}));
                                        p.dispatchEvent(new Event('change', {bubbles:true}));
                                        // 找密码框前面的文本输入框
                                        var prev = p.previousElementSibling;
                                        var found = false;
                                        while (prev) {
                                            if (prev.tagName === 'INPUT' && (prev.type === 'text' || prev.type === 'email' || prev.type === '' || !prev.type)) {
                                                prev.value = user;
                                                prev.dispatchEvent(new Event('input', {bubbles:true}));
                                                prev.dispatchEvent(new Event('change', {bubbles:true}));
                                                found = true;
                                                break;
                                            }
                                            prev = prev.previousElementSibling;
                                        }
                                        // 按 name/id 查找用户名框
                                        if (!found) {
                                            var all = document.querySelectorAll('input[type=text], input[type=email], input:not([type])');
                                            for (var i = 0; i < all.length; i++) {
                                                var inp = all[i];
                                                var key = (inp.name + inp.id).toLowerCase();
                                                if (key.indexOf('user')>=0 || key.indexOf('name')>=0 || key.indexOf('login')>=0 || key.indexOf('account')>=0 || key.indexOf('username')>=0) {
                                                    inp.value = user;
                                                    inp.dispatchEvent(new Event('input', {bubbles:true}));
                                                    inp.dispatchEvent(new Event('change', {bubbles:true}));
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                } catch(e) {}
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(js, null)
                    }
                }
            }
            webChromeClient = object : WebChromeClient() {
                // 加载进度更新
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    webProgressBar?.progress = newProgress
                    webProgressBar?.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
                }

                // 文件上传支持
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    uploadMessage?.onReceiveValue(null)
                    uploadMessage = filePathCallback
                    val intent = fileChooserParams?.createIntent()
                    if (intent != null) {
                        try {
                            startActivityForResult(intent, REQUEST_FILE_CHOOSER)
                        } catch (e: Exception) {
                            uploadMessage = null
                            Toast.makeText(this@MainActivity, "无法打开文件选择器", Toast.LENGTH_SHORT).show()
                            return false
                        }
                    }
                    return true
                }
            }

            // 文件下载支持
            setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                try {
                    val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                    val request = DownloadManager.Request(Uri.parse(url)).apply {
                        setMimeType(mimeType)
                        addRequestHeader("User-Agent", userAgent)
                        setTitle(fileName)
                        setDescription("路由器管理下载")
                        allowScanningByMediaScanner()
                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    }
                    val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                    dm.enqueue(request)
                    Toast.makeText(this@MainActivity, "开始下载：$fileName", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "下载失败：${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            loadUrl(router.currentUrl)
        }
    }

    /** 关闭一个已打开的 WebView */
    private fun closeRouter(router: RouterStore.Router) {
        webViewPool.remove(router.id)?.destroy()
        webViewUrls.remove(router.id)
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

    // ─── 设置 ────────────────────────────────────────────

    private fun showSettings() {
        SettingsDialog.show(this) {
            applySettings()
        }
    }

    private fun applySettings() {
        applyImmersiveFlags()
        managerView.applyTheme()
        managerView.applyBackground()
        managerView.refresh()
        multiTaskView.applyTheme()
        multiTaskView.applyBackground()
        updateBottomNav()
        // 更新所有已打开 WebView 的 UA 并刷新
        val ua = AppSettings.getUserAgent(
            this,
            defaultUserAgent ?: System.getProperty("http.agent") ?: ""
        )
        webViewPool.values.forEach { webView ->
            webView.settings.userAgentString = ua
            webView.reload()
        }
    }

    /** 根据背景明暗自动切换状态栏图标颜色（暗背景白字，亮背景黑字） */
    private fun applyImmersiveFlags() {
        val baseFlags = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        val dark = AppSettings.isDarkBackground(this)
        window.decorView.systemUiVisibility = if (dark) {
            baseFlags // 暗色背景，状态栏图标保持白色
        } else {
            baseFlags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR // 亮色背景，状态栏图标变深
        }
    }

    // ─── 关于 ────────────────────────────────────────────

    private fun showAbout() {
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: Exception) {
            "1.0"
        }

        val message = """
            使用说明：
            1. 底部「+」添加路由器，可填名称、地址、用户名、密码
            2. 点击路由器卡片进入管理界面，登录状态自动保存
            3. 卡片右侧眼睛按钮可单独隐藏/显示该路由器的地址和密码
            4. 底部「多任务」查看已打开的管理界面，可切换或关闭
            5. 首页右上角齿轮可自定义标题、主题色、背景图、UA模式
            6. 管理界面按返回键直接回多任务页，登录状态不丢失
            7. 长按路由器卡片可删除

            更新日志：
            【最新】
            • 支持文件上传和下载，路由器固件升级/配置备份可用
            • 管理界面按返回键直接回多任务页，无需逐步退网页
            • 管理界面增加半透明状态栏背景条，避免网页与状态栏重叠
            • 重新绘制高大上矢量App图标（渐变蓝底+路由器+信号波纹）
            • 玻璃拟态卡片UI优化，文字对比度提升清晰可读
            • 每卡片独立IP/密码显示隐藏（眼睛矢量图标按钮）
            • 保存路由器用户名密码，打开后台自动填充登录
            • UA模式切换（手机/电脑），默认电脑模式适配UAC登录
            • 状态栏图标颜色随背景明暗自动适配
            • 多路由器后台多任务切换，登录状态保持
            • 自定义标题、多颜色主题、自定义背景壁纸
            • 底部导航栏透明带阴影，壁纸延伸到状态栏和导航栏
            • 版本号自动迭代递增

            作者：burry默默
            版本：$versionName
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("关于")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
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

    // ─── 系统栏高度 ──────────────────────────────────────

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else dp(24)
    }

    private fun getNavBarHeight(): Int {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else dp(48)
    }

    // ─── 返回键 / 生命周期 ───────────────────────────────

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (currentTab == TAB_WEB && currentRouterId != null) {
            // 按返回键直接回到多任务页面，不逐步退网页历史，WebView保持后台登录状态
            if (openedRouters.isNotEmpty()) {
                switchTab(TAB_MULTI)
            } else {
                switchTab(TAB_HOME)
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 文件上传选择结果
        if (requestCode == REQUEST_FILE_CHOOSER) {
            val results = if (resultCode == RESULT_OK && data != null) {
                FileChooserParams.parseResult(resultCode, data)
            } else null
            uploadMessage?.onReceiveValue(results)
            uploadMessage = null
            return
        }
        if (RouterEditor.handleActivityResult(this, requestCode, resultCode, data)) return
        SettingsDialog.handleActivityResult(this, requestCode, resultCode, data)
    }

    override fun onDestroy() {
        webViewPool.values.forEach { it.destroy() }
        webViewPool.clear()
        webViewUrls.clear()
        super.onDestroy()
    }

    /** 转义 JS 字符串中的特殊字符，防止注入 */
    private fun escapeJs(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
