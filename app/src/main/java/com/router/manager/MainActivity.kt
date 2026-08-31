package com.router.manager

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import android.app.AlertDialog
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.min

class MainActivity : android.app.Activity() {

    private lateinit var webView: WebView
    private lateinit var prefs: SharedPreferences
    private val routers: MutableList<Router> = mutableListOf()
    private var currentRouter: Router? = null

    // 编辑对话框临时状态
    private var editingId: String = ""
    private var editingIconColor: Int = iconColors[0]
    private var editingCustomIconPath: String? = null
    private var editIconPreview: ImageView? = null

    companion object {
        private const val PREFS_NAME = "router_manager_prefs"
        private const val KEY_ROUTERS = "routers_json"
        private const val KEY_CURRENT_ID = "current_router_id"
        private const val REQUEST_PICK_IMAGE = 1001

        private val iconColors = intArrayOf(
            0xFF1565C0.toInt(), // 蓝
            0xFF2E7D32.toInt(), // 绿
            0xFFE65100.toInt(), // 橙
            0xFFC62828.toInt(), // 红
            0xFF6A1B9A.toInt(), // 紫
            0xFF00838F.toInt(), // 青
            0xFFAD1457.toInt(), // 粉
            0xFF455A64.toInt(), // 灰蓝
        )
    }

    data class Router(
        val id: String,
        val name: String,
        val url: String,
        val iconColor: Int,
        val customIconPath: String?
    )

    // ─── 生命周期 ────────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadRouters()

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

        val currentId = prefs.getString(KEY_CURRENT_ID, null)
        currentRouter = routers.find { it.id == currentId }

        when {
            currentRouter != null -> loadRouter(currentRouter!!)
            routers.isNotEmpty() -> loadRouter(routers.first())
            else -> {
                actionBar?.title = "路由器管理"
                showEditDialog(null, cancelable = false)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK) {
            data?.data?.let { uri -> handleImageSelected(uri) }
        }
    }

    // ─── 数据持久化 ──────────────────────────────────────

    private fun loadRouters() {
        routers.clear()
        val json = prefs.getString(KEY_ROUTERS, null) ?: return
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val customPath = obj.optString("customIconPath", "")
                routers.add(
                    Router(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        url = obj.getString("url"),
                        iconColor = obj.optInt("iconColor", iconColors[0]),
                        customIconPath = customPath.ifEmpty { null }
                    )
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun saveRouters() {
        val arr = JSONArray()
        for (r in routers) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("name", r.name)
                put("url", r.url)
                put("iconColor", r.iconColor)
                put("customIconPath", r.customIconPath ?: "")
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

    // ─── 图标相关 ────────────────────────────────────────

    private fun getIconDrawable(router: Router): Drawable? {
        router.customIconPath?.let { path ->
            getCustomIconDrawable(path)?.let { return it }
        }
        return getBuiltinIconDrawable(router.iconColor)
    }

    private fun getBuiltinIconDrawable(color: Int): Drawable {
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
        val symbol = getDrawable(R.drawable.ic_launcher_foreground)?.mutate()?.apply {
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
        }
        val layers = if (symbol != null) arrayOf(bg, symbol) else arrayOf(bg)
        return LayerDrawable(layers).apply {
            if (symbol != null) {
                val inset = dp(10)
                setLayerInset(1, inset, inset, inset, inset)
            }
        }
    }

    private fun getCustomIconDrawable(path: String): Drawable? {
        val file = File(path)
        if (!file.exists()) return null
        val bitmap = BitmapFactory.decodeFile(path) ?: return null
        return BitmapDrawable(resources, getCircularBitmap(bitmap))
    }

    private fun getCircularBitmap(src: Bitmap): Bitmap {
        val size = min(src.width, src.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val srcRect = Rect(
            (src.width - size) / 2,
            (src.height - size) / 2,
            (src.width + size) / 2,
            (src.height + size) / 2
        )
        canvas.drawBitmap(src, srcRect, Rect(0, 0, size, size), paint)
        return output
    }

    private fun scaleToSquare(src: Bitmap, size: Int): Bitmap {
        val minDim = min(src.width, src.height)
        val x = (src.width - minDim) / 2
        val y = (src.height - minDim) / 2
        val cropped = Bitmap.createBitmap(src, x, y, minDim, minDim)
        return Bitmap.createScaledBitmap(cropped, size, size, true)
    }

    private fun saveCustomIcon(bitmap: Bitmap, routerId: String): String {
        val dir = File(filesDir, "router_icons")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "$routerId.png")
        val scaled = scaleToSquare(bitmap, 128)
        FileOutputStream(file).use { out ->
            getCircularBitmap(scaled).compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.absolutePath
    }

    private fun handleImageSelected(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap != null) {
                val path = saveCustomIcon(bitmap, editingId)
                editingCustomIconPath = path
                updateEditIconPreview()
                Toast.makeText(this, "图标已更换", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateEditIconPreview() {
        editIconPreview?.let {
            val preview = Router("preview", "", "", editingIconColor, editingCustomIconPath)
            it.setImageDrawable(getIconDrawable(preview))
        }
    }

    // ─── 对话框：路由器列表（切换） ──────────────────────

    private fun showRouterListDialog() {
        if (routers.isEmpty()) {
            showEditDialog(null, cancelable = true)
            return
        }

        val listView = ListView(this)
        val adapter = RouterListAdapter()
        listView.adapter = adapter

        val addBtn = Button(this).apply {
            text = "＋ 添加路由器"
            setOnClickListener {
                showEditDialog(null, cancelable = true)
            }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                listView,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )
            addView(addBtn)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("切换路由器")
            .setView(content)
            .setNegativeButton("关闭", null)
            .create()

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val router = routers[position]
            if (router.id != currentRouter?.id) {
                loadRouter(router)
                Toast.makeText(this, "已切换到「${router.name}」", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    // ─── 对话框：添加 / 编辑路由器 ───────────────────────

    private fun showEditDialog(router: Router?, cancelable: Boolean) {
        editingId = router?.id ?: UUID.randomUUID().toString()
        editingIconColor = router?.iconColor ?: iconColors[0]
        editingCustomIconPath = router?.customIconPath

        val nameInput = EditText(this).apply {
            hint = "名称（如：家里、公司、机房A）"
            setText(router?.name ?: "")
            setSelection(text.length)
            val pad = dp(12)
            setPadding(pad, pad, pad, pad)
        }
        val urlInput = EditText(this).apply {
            hint = "地址（如：http://192.168.1.1:3000）"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            setText(router?.url ?: "")
            setSelection(text.length)
            val pad = dp(12)
            setPadding(pad, pad, pad, pad)
        }

        val iconPreview = ImageView(this).apply {
            val size = dp(56)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginEnd = dp(16)
            }
            val previewRouter = Router("preview", "", "", editingIconColor, editingCustomIconPath)
            setImageDrawable(getIconDrawable(previewRouter))
            setOnClickListener { showIconPickerDialog() }
        }
        editIconPreview = iconPreview

        val inputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
            addView(nameInput)
            addView(urlInput)
        }

        val topLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(20), dp(16), dp(20), dp(8))
            addView(iconPreview)
            addView(inputLayout)
        }

        val hintText = TextView(this).apply {
            text = "点击左侧图标可更换图标（颜色或相册图片）"
            textSize = 12f
            setTextColor(0xFF888888.toInt())
            setPadding(dp(20), 0, dp(20), dp(12))
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(topLayout)
            addView(hintText)
        }

        AlertDialog.Builder(this)
            .setTitle(if (router == null) "添加路由器" else "编辑路由器")
            .setView(content)
            .setCancelable(cancelable)
            .setPositiveButton("保存") { _, _ ->
                val name = nameInput.text.toString().trim()
                var url = urlInput.text.toString().trim()

                if (name.isEmpty() || url.isEmpty()) {
                    Toast.makeText(this, "名称和地址都不能为空", Toast.LENGTH_SHORT).show()
                    if (!cancelable) showEditDialog(null, false)
                    editIconPreview = null
                    return@setPositiveButton
                }
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "http://$url"
                }

                val finalRouter = Router(
                    id = editingId,
                    name = name,
                    url = url,
                    iconColor = editingIconColor,
                    customIconPath = editingCustomIconPath
                )

                if (router == null) {
                    routers.add(finalRouter)
                } else {
                    val index = routers.indexOfFirst { it.id == router.id }
                    if (index >= 0) routers[index] = finalRouter
                }
                saveRouters()
                loadRouter(finalRouter)
                editIconPreview = null
                Toast.makeText(this, "已保存「$name」", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消") { _, _ ->
                editIconPreview = null
            }
            .show()
    }

    // ─── 对话框：图标选择 ────────────────────────────────

    private fun showIconPickerDialog() {
        val colorGrid = GridLayout(this).apply {
            columnCount = 4
            setPadding(dp(20), dp(16), dp(20), dp(8))
        }

        for (color in iconColors) {
            val isSelected = (color == editingIconColor && editingCustomIconPath == null)
            val colorView = View(this).apply {
                val size = dp(44)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(dp(6), dp(6), dp(6), dp(6))
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    if (isSelected) setStroke(dp(3), 0xFF333333.toInt())
                }
                setOnClickListener {
                    editingIconColor = color
                    editingCustomIconPath = null
                    updateEditIconPreview()
                    for (i in 0 until colorGrid.childCount) {
                        (colorGrid.getChildAt(i).background as? GradientDrawable)?.setStroke(0, 0)
                    }
                    (it.background as? GradientDrawable)?.setStroke(dp(3), 0xFF333333.toInt())
                }
            }
            colorGrid.addView(colorView)
        }

        val pickFromAlbumBtn = Button(this).apply {
            text = "从相册选择图片"
            setOnClickListener {
                val intent = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
                startActivityForResult(intent, REQUEST_PICK_IMAGE)
            }
        }

        val resetBtn = Button(this).apply {
            text = "恢复默认图标"
            setOnClickListener {
                editingCustomIconPath = null
                editingIconColor = iconColors[0]
                updateEditIconPreview()
                for (i in 0 until colorGrid.childCount) {
                    (colorGrid.getChildAt(i).background as? GradientDrawable)?.setStroke(0, 0)
                }
                (colorGrid.getChildAt(0).background as? GradientDrawable)?.setStroke(
                    dp(3),
                    0xFF333333.toInt()
                )
            }
        }

        val btnLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), 0, dp(20), dp(16))
            addView(pickFromAlbumBtn)
            addView(resetBtn)
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(colorGrid)
            addView(btnLayout)
        }

        AlertDialog.Builder(this)
            .setTitle("选择图标")
            .setView(content)
            .setPositiveButton("完成", null)
            .show()
    }

    // ─── 对话框：删除 ────────────────────────────────────

    private fun showDeleteDialog() {
        val cur = currentRouter ?: return
        AlertDialog.Builder(this)
            .setTitle("删除路由器")
            .setMessage("确定要删除「${cur.name}」吗？")
            .setPositiveButton("删除") { _, _ ->
                cur.customIconPath?.let { File(it).delete() }
                routers.removeAll { it.id == cur.id }
                saveRouters()
                currentRouter = null
                prefs.edit().remove(KEY_CURRENT_ID).apply()

                if (routers.isNotEmpty()) {
                    loadRouter(routers.first())
                } else {
                    webView.loadUrl("about:blank")
                    actionBar?.title = "路由器管理"
                    showEditDialog(null, cancelable = false)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ─── 工具 ────────────────────────────────────────────

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    // ─── 列表适配器 ──────────────────────────────────────

    inner class RouterListAdapter : BaseAdapter() {
        override fun getCount() = routers.size
        override fun getItem(position: Int) = routers[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val router = routers[position]

            val layout = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(12))
            }

            val icon = ImageView(this@MainActivity).apply {
                val size = dp(44)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = dp(14)
                }
                setImageDrawable(getIconDrawable(router))
            }

            val textLayout = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val nameText = TextView(this@MainActivity).apply {
                text = if (router.id == currentRouter?.id) "✓ ${router.name}" else router.name
                textSize = 16f
                setTextColor(0xFF222222.toInt())
                setTypeface(null, Typeface.BOLD)
            }

            val urlText = TextView(this@MainActivity).apply {
                text = router.url
                textSize = 12f
                setTextColor(0xFF888888.toInt())
            }

            textLayout.addView(nameText)
            textLayout.addView(urlText)

            val editBtn = ImageView(this@MainActivity).apply {
                val size = dp(36)
                layoutParams = LinearLayout.LayoutParams(size, size)
                setImageResource(R.drawable.ic_edit)
                setColorFilter(0xFF666666.toInt(), PorterDuff.Mode.SRC_ATOP)
                setPadding(dp(6), dp(6), dp(6), dp(6))
                setOnClickListener {
                    showEditDialog(router, cancelable = true)
                }
            }

            layout.addView(icon)
            layout.addView(textLayout)
            layout.addView(editBtn)

            return layout
        }
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
            1 -> showRouterListDialog()
            2 -> showEditDialog(null, cancelable = true)
            3 -> showDeleteDialog()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    // ─── 返回键 / 销毁 ───────────────────────────────────

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
