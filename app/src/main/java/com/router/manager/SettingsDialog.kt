package com.router.manager

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * App 设置对话框：自定义标题、主题色、背景图
 */
object SettingsDialog {

    private const val REQUEST_PICK_BACKGROUND = 1002
    private const val REQUEST_BACKUP = 1003
    private const val REQUEST_RESTORE = 1004

    private var tempTitle: String = ""
    private var tempThemeColor: Int = AppSettings.defaultTheme
    private var tempBackgroundPath: String? = null
    private var tempUaMode: Int = AppSettings.UA_MODE_DESKTOP
    private var tempShowIp: Boolean = true
    private var bgPreview: ImageView? = null
    private var colorContainer: LinearLayout? = null
    private var uaMobileBtn: Button? = null
    private var uaDesktopBtn: Button? = null
    private var ipShowBtn: Button? = null
    private var ipHideBtn: Button? = null
    private var density: Float = 2f
    private var onChangedCallback: (() -> Unit)? = null

    fun show(activity: Activity, onChanged: () -> Unit) {
        tempTitle = AppSettings.getTitle(activity)
        tempThemeColor = AppSettings.getThemeColor(activity)
        tempBackgroundPath = AppSettings.getBackgroundPath(activity)
        tempUaMode = AppSettings.getUaMode(activity)
        tempShowIp = AppSettings.getShowIp(activity)
        density = activity.resources.displayMetrics.density
        onChangedCallback = onChanged

        val titleInput = EditText(activity).apply {
            hint = "首页标题（如：我的路由器）"
            setText(tempTitle)
            setSelection(text.length)
            val pad = dp(activity, 12)
            setPadding(pad, pad, pad, pad)
        }

        val titleLabel = TextView(activity).apply {
            text = "首页标题"
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            setPadding(dp(activity, 4), 0, 0, dp(activity, 4))
        }

        // 主题色选择
        val themeLabel = TextView(activity).apply {
            text = "主题色"
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            setPadding(dp(activity, 4), dp(activity, 12), 0, dp(activity, 4))
        }

        val colorScroll = HorizontalScrollView(activity).apply {
            isHorizontalScrollBarEnabled = false
        }
        colorContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(activity, 4), 0, dp(activity, 4), 0)
        }
        for (color in AppSettings.themeColors) {
            val isSelected = color == tempThemeColor
            val dot = View(activity).apply {
                val size = dp(activity, 40)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = dp(activity, 10)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    if (isSelected) setStroke(dp(activity, 3), Color.parseColor("#333333"))
                }
                setOnClickListener {
                    tempThemeColor = color
                    refreshColorDots()
                }
            }
            colorContainer?.addView(dot)
        }
        colorScroll.addView(colorContainer)

        // 背景图
        val bgLabel = TextView(activity).apply {
            text = "管理界面背景图"
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            setPadding(dp(activity, 4), dp(activity, 12), 0, dp(activity, 4))
        }

        bgPreview = ImageView(activity).apply {
            val size = dp(activity, 80)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(activity, 12)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.parseColor("#EEEEEE"))
            updateBgPreview(activity)
        }

        val pickBgBtn = Button(activity).apply {
            text = "从相册选择"
            setOnClickListener {
                val intent = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
                activity.startActivityForResult(intent, REQUEST_PICK_BACKGROUND)
            }
        }

        val clearBgBtn = Button(activity).apply {
            text = "清除背景"
            setOnClickListener {
                tempBackgroundPath = null
                updateBgPreview(activity)
            }
        }

        val bgBtnLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
            addView(pickBgBtn)
            addView(clearBgBtn)
        }

        val bgRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(bgPreview)
            addView(bgBtnLayout)
        }

        // UA 模式选择
        val uaLabel = TextView(activity).apply {
            text = "浏览器标识（UA）"
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            setPadding(dp(activity, 4), dp(activity, 12), 0, dp(activity, 4))
        }

        uaMobileBtn = Button(activity).apply {
            text = "手机模式"
            setOnClickListener {
                tempUaMode = AppSettings.UA_MODE_MOBILE
                updateUaButtons(activity)
            }
        }

        uaDesktopBtn = Button(activity).apply {
            text = "电脑模式"
            setOnClickListener {
                tempUaMode = AppSettings.UA_MODE_DESKTOP
                updateUaButtons(activity)
            }
        }

        val uaBtnRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(uaMobileBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(activity, 8)
            })
            addView(uaDesktopBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        updateUaButtons(activity)

        // IP 地址显示开关
        val ipLabel = TextView(activity).apply {
            text = "IP地址显示"
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            setPadding(dp(activity, 4), dp(activity, 12), 0, dp(activity, 4))
        }

        ipShowBtn = Button(activity).apply {
            text = "显示地址"
            setOnClickListener {
                tempShowIp = true
                updateIpButtons(activity)
            }
        }

        ipHideBtn = Button(activity).apply {
            text = "隐藏地址"
            setOnClickListener {
                tempShowIp = false
                updateIpButtons(activity)
            }
        }

        val ipBtnRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(ipShowBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(activity, 8)
            })
            addView(ipHideBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        updateIpButtons(activity)

        // 数据备份与恢复
        val backupLabel = TextView(activity).apply {
            text = "数据备份与恢复"
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            setPadding(dp(activity, 4), dp(activity, 12), 0, dp(activity, 4))
        }

        val backupBtn = Button(activity).apply {
            text = "备份数据"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    putExtra(Intent.EXTRA_TITLE, "router_backup_$time.json")
                }
                activity.startActivityForResult(intent, REQUEST_BACKUP)
            }
        }

        val restoreBtn = Button(activity).apply {
            text = "恢复数据"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                }
                activity.startActivityForResult(intent, REQUEST_RESTORE)
            }
        }

        val backupBtnRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(backupBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(activity, 8)
            })
            addView(restoreBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(activity, 20), dp(activity, 16), dp(activity, 20), dp(activity, 8))
            addView(titleLabel)
            addView(titleInput)
            addView(themeLabel)
            addView(colorScroll)
            addView(bgLabel)
            addView(bgRow)
            addView(uaLabel)
            addView(uaBtnRow)
            addView(ipLabel)
            addView(ipBtnRow)
            addView(backupLabel)
            addView(backupBtnRow)
        }

        AlertDialog.Builder(activity)
            .setTitle("界面设置")
            .setView(content)
            .setPositiveButton("保存") { _, _ ->
                val title = titleInput.text.toString().trim()
                if (title.isNotEmpty()) {
                    AppSettings.setTitle(activity, title)
                }
                AppSettings.setThemeColor(activity, tempThemeColor)
                AppSettings.setBackgroundPath(activity, tempBackgroundPath)
                AppSettings.setUaMode(activity, tempUaMode)
                AppSettings.setShowIp(activity, tempShowIp)
                onChangedCallback?.invoke()
                cleanup()
                Toast.makeText(activity, "设置已保存", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消") { _, _ ->
                cleanup()
            }
            .show()
    }

    fun handleActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ): Boolean {
        if (requestCode == REQUEST_PICK_BACKGROUND && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                handleBackgroundSelected(activity, uri)
                return true
            }
        }
        if (requestCode == REQUEST_BACKUP && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                doBackup(activity, uri)
                return true
            }
        }
        if (requestCode == REQUEST_RESTORE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                doRestore(activity, uri)
                return true
            }
        }
        return false
    }

    // ─── 数据备份与恢复 ────────────────────────────────────────

    private fun doBackup(activity: Activity, uri: Uri) {
        try {
            val routers = RouterStore.loadRouters(activity)
            val jsonArray = JSONArray()
            for (r in routers) {
                val obj = JSONObject().apply {
                    put("id", r.id)
                    put("name", r.name)
                    put("url", r.url)
                    put("remoteUrl", r.remoteUrl)
                    put("iconColor", r.iconColor)
                    put("customIconPath", r.customIconPath ?: "")
                    put("username", r.username)
                    put("password", r.password)
                    put("showIp", r.showIp)
                    put("accessMode", r.accessMode)
                }
                jsonArray.put(obj)
            }
            val root = JSONObject().apply {
                put("version", 1)
                put("backupTime", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                put("routers", jsonArray)
            }
            activity.contentResolver.openOutputStream(uri)?.use { out ->
                OutputStreamWriter(out).use { writer ->
                    writer.write(root.toString(2))
                }
            }
            Toast.makeText(activity, "备份成功，共 ${routers.size} 个路由器", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(activity, "备份失败：${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun doRestore(activity: Activity, uri: Uri) {
        try {
            val content = activity.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).use { reader ->
                    reader.readText()
                }
            } ?: run {
                Toast.makeText(activity, "读取文件失败", Toast.LENGTH_SHORT).show()
                return
            }
            val root = JSONObject(content)
            val jsonArray = root.getJSONArray("routers")
            val restored = mutableListOf<RouterStore.Router>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val customIcon = obj.optString("customIconPath", "")
                val router = RouterStore.Router(
                    id = obj.optString("id", System.currentTimeMillis().toString()),
                    name = obj.optString("name", "未命名"),
                    url = obj.optString("url", ""),
                    remoteUrl = obj.optString("remoteUrl", ""),
                    iconColor = obj.optInt("iconColor", AppSettings.defaultTheme),
                    customIconPath = if (customIcon.isNotEmpty() && File(customIcon).exists()) customIcon else null,
                    username = obj.optString("username", ""),
                    password = obj.optString("password", ""),
                    showIp = obj.optBoolean("showIp", true),
                    accessMode = obj.optInt("accessMode", RouterStore.ACCESS_LOCAL)
                )
                restored.add(router)
            }
            RouterStore.saveRouters(activity, restored)
            onChangedCallback?.invoke()
            Toast.makeText(activity, "恢复成功，共 ${restored.size} 个路由器", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(activity, "恢复失败：${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ─── 内部方法 ────────────────────────────────────────

    private fun refreshColorDots() {
        colorContainer?.let { container ->
            for (i in 0 until container.childCount) {
                val dot = container.getChildAt(i)
                val color = AppSettings.themeColors.getOrNull(i) ?: continue
                (dot.background as? GradientDrawable)?.setStroke(
                    if (color == tempThemeColor) (3 * density).toInt() else 0,
                    Color.parseColor("#333333")
                )
            }
        }
    }

    private fun updateBgPreview(activity: Activity) {
        bgPreview?.let { iv ->
            val path = tempBackgroundPath
            if (path != null && File(path).exists()) {
                iv.setImageBitmap(BitmapFactory.decodeFile(path))
            } else {
                iv.setImageDrawable(null)
                iv.setBackgroundColor(Color.parseColor("#EEEEEE"))
            }
        }
    }

    private fun handleBackgroundSelected(activity: Activity, uri: Uri) {
        try {
            val inputStream = activity.contentResolver.openInputStream(uri) ?: return
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap != null) {
                // 压缩保存
                val scaled = if (bitmap.width > 1080) {
                    val ratio = 1080f / bitmap.width
                    Bitmap.createScaledBitmap(
                        bitmap,
                        1080,
                        (bitmap.height * ratio).toInt(),
                        true
                    )
                } else bitmap

                val file = File(activity.filesDir, "app_background.jpg")
                FileOutputStream(file).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                tempBackgroundPath = file.absolutePath
                updateBgPreview(activity)
                Toast.makeText(activity, "背景图已选择", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(activity, "图片加载失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUaButtons(activity: Activity) {
        val selected = AppSettings.lighten(AppSettings.getThemeColor(activity), 0.85f)
        uaMobileBtn?.setBackgroundColor(
            if (tempUaMode == AppSettings.UA_MODE_MOBILE) selected else Color.parseColor("#EEEEEE")
        )
        uaDesktopBtn?.setBackgroundColor(
            if (tempUaMode == AppSettings.UA_MODE_DESKTOP) selected else Color.parseColor("#EEEEEE")
        )
    }

    private fun updateIpButtons(activity: Activity) {
        val selected = AppSettings.lighten(AppSettings.getThemeColor(activity), 0.85f)
        ipShowBtn?.setBackgroundColor(
            if (tempShowIp) selected else Color.parseColor("#EEEEEE")
        )
        ipHideBtn?.setBackgroundColor(
            if (!tempShowIp) selected else Color.parseColor("#EEEEEE")
        )
    }

    private fun cleanup() {
        bgPreview = null
        colorContainer = null
        uaMobileBtn = null
        uaDesktopBtn = null
        ipShowBtn = null
        ipHideBtn = null
        onChangedCallback = null
    }

    private fun dp(activity: Activity, value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }
}
