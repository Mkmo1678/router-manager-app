package com.router.manager

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
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
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.widget.Toast
import android.app.AlertDialog
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class MainActivity : android.app.Activity() {

    private lateinit var webView: WebView
    private lateinit var prefs: SharedPreferences
    private val routers: MutableList<Router> = mutableListOf()
    private var currentRouter: Router? = null

    companion object {
        private const val PREFS_NAME = "router_manager_prefs"
        private const val KEY_ROUTERS = "routers_json"
        private const val KEY_CURRENT_ID = "current_router_id"
    }

    data class Router(val id: String, val name: String, val url: String)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadRouters()

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

        // 恢复上次使用的路由器
        val currentId = prefs.getString(KEY_CURRENT_ID, null)
        currentRouter = routers.find { it.id == currentId }

        when {
            currentRouter != null -> loadRouter(currentRouter!!)
            routers.isNotEmpty() -> loadRouter(routers.first())
            else -> showAddDialog(cancelable = false)
        }
    }

    // ─── 数据持久化 ───────────────────────────────────────

    private fun loadRouters() {
        routers.clear()
        val json = prefs.getString(KEY_ROUTERS, null) ?: return
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                routers.add(
                    Router(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        url = obj.getString("url")
                    )
                )
            }
        } catch (_: Exception) {
            // 解析失败则忽略旧数据
        }
    }

    private fun saveRouters() {
        val arr = JSONArray()
        for (r in routers) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("name", r.name)
                put("url", r.url)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_ROUTERS, arr.toString()).apply()
    }

    private fun loadRouter(router: Router) {
        currentRouter = router
        prefs.edit().putString(KEY_CURRENT_ID, router.id).apply()
        actionBar?.title = router.name
        webView.loadUrl(router.url)
    }

    // ─── 对话框 ──────────────────────────────────────────

    /**
     * 添加路由器：输入名称 + 地址
     */
    private fun showAddDialog(cancelable: Boolean) {
        val nameInput = EditText(this).apply {
            hint = "名称（如：家里、公司、机房A）"
            val pad = dp(12)
            setPadding(pad, pad, pad, pad)
        }
        val urlInput = EditText(this).apply {
            hint = "地址（如：http://192.168.1.1:3000）"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            val pad = dp(12)
            setPadding(pad, pad, pad, pad)
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(20)
            setPadding(pad, 0, pad, 0)
            addView(nameInput)
            addView(urlInput)
        }

        AlertDialog.Builder(this)
            .setTitle("添加路由器")
            .setView(layout)
            .setCancelable(cancelable)
            .setPositiveButton("保存") { _, _ ->
                val name = nameInput.text.toString().trim()
                var url = urlInput.text.toString().trim()

                if (name.isEmpty() || url.isEmpty()) {
                    Toast.makeText(this, "名称和地址都不能为空", Toast.LENGTH_SHORT).show()
                    if (!cancelable) showAddDialog(false)
                    return@setPositiveButton
                }
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "http://$url"
                }

                val router = Router(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    url = url
                )
                routers.add(router)
                saveRouters()
                loadRouter(router)
                Toast.makeText(this, "已添加「$name」", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 切换路由器：列表选择
     */
    private fun showSwitchDialog() {
        if (routers.isEmpty()) {
            showAddDialog(cancelable = true)
            return
        }
        val names = routers.map {
            if (it.id == currentRouter?.id) "✓ ${it.name}" else it.name
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("切换路由器")
            .setItems(names) { _, which ->
                val router = routers[which]
                if (router.id != currentRouter?.id) {
                    loadRouter(router)
                    Toast.makeText(this, "已切换到「${router.name}」", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 删除当前路由器
     */
    private fun showDeleteDialog() {
        val cur = currentRouter ?: return
        AlertDialog.Builder(this)
            .setTitle("删除路由器")
            .setMessage("确定要删除「${cur.name}」吗？")
            .setPositiveButton("删除") { _, _ ->
                routers.removeAll { it.id == cur.id }
                saveRouters()
                currentRouter = null
                prefs.edit().remove(KEY_CURRENT_ID).apply()

                if (routers.isNotEmpty()) {
                    loadRouter(routers.first())
                } else {
                    webView.loadUrl("about:blank")
                    showAddDialog(cancelable = false)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ─── 工具 ────────────────────────────────────────────

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    // ─── 菜单 ────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 1, 0, "切换")?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu?.add(0, 2, 1, "添加路由器")
        menu?.add(0, 3, 2, "删除当前路由器")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> showSwitchDialog()
            2 -> showAddDialog(cancelable = true)
            3 -> showDeleteDialog()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    // ─── 生命周期 ────────────────────────────────────────

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
