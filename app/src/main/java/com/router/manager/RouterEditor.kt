package com.router.manager

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.UUID

/**
 * 路由器添加/编辑对话框（全局共享，含图标选择与相册选图）
 */
object RouterEditor {

    private const val REQUEST_PICK_IMAGE = 1001

    private var editingId: String = ""
    private var editingIconColor: Int = RouterStore.iconColors[0]
    private var editingCustomIconPath: String? = null
    private var editingRemoteUrl: String = ""
    private var editingAccessMode: Int = RouterStore.ACCESS_LOCAL
    private var editIconPreview: ImageView? = null
    private var localModeBtn: Button? = null
    private var remoteModeBtn: Button? = null
    private var onSavedCallback: ((RouterStore.Router) -> Unit)? = null

    /**
     * 显示添加或编辑对话框
     * @param router  为 null 表示添加；非 null 表示编辑
     * @param onSaved 保存成功后的回调
     */
    fun showEditDialog(
        activity: Activity,
        router: RouterStore.Router?,
        cancelable: Boolean,
        onSaved: (RouterStore.Router) -> Unit
    ) {
        editingId = router?.id ?: UUID.randomUUID().toString()
        editingIconColor = router?.iconColor ?: RouterStore.iconColors[0]
        editingCustomIconPath = router?.customIconPath
        editingRemoteUrl = router?.remoteUrl ?: ""
        editingAccessMode = router?.accessMode ?: RouterStore.ACCESS_LOCAL
        onSavedCallback = onSaved

        val nameInput = EditText(activity).apply {
            hint = "名称（如：家里、公司、机房A）"
            setText(router?.name ?: "")
            setSelection(text.length)
            val pad = dp(activity, 12)
            setPadding(pad, pad, pad, pad)
        }
        val urlInput = EditText(activity).apply {
            hint = "本地地址（局域网，如：http://192.168.1.1）"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            setText(router?.url ?: "")
            setSelection(text.length)
            val pad = dp(activity, 12)
            setPadding(pad, pad, pad, pad)
        }
        val remoteUrlInput = EditText(activity).apply {
            hint = "远程地址（Cloudflare等，如：https://router.example.com）"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            setText(editingRemoteUrl)
            setSelection(text.length)
            val pad = dp(activity, 12)
            setPadding(pad, pad, pad, pad)
        }

        // 访问模式切换
        val modeLabel = TextView(activity).apply {
            text = "默认访问方式"
            textSize = 13f
            setTextColor(0xFF666666.toInt())
            setPadding(dp(activity, 4), dp(activity, 8), 0, dp(activity, 4))
        }
        localModeBtn = Button(activity).apply {
            text = "本地访问"
            setOnClickListener {
                editingAccessMode = RouterStore.ACCESS_LOCAL
                updateModeButtons(activity)
            }
        }
        remoteModeBtn = Button(activity).apply {
            text = "远程访问"
            setOnClickListener {
                editingAccessMode = RouterStore.ACCESS_REMOTE
                updateModeButtons(activity)
            }
        }
        val modeBtnRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(localModeBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(activity, 8)
            })
            addView(remoteModeBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        updateModeButtons(activity)
        val usernameInput = EditText(activity).apply {
            hint = "登录用户名（可选，保存后自动填充）"
            setText(router?.username ?: "")
            setSelection(text.length)
            val pad = dp(activity, 12)
            setPadding(pad, pad, pad, pad)
        }
        val passwordInput = EditText(activity).apply {
            hint = "登录密码（可选，保存后自动填充）"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(router?.password ?: "")
            setSelection(text.length)
            val pad = dp(activity, 12)
            setPadding(pad, pad, pad, pad)
        }

        val iconPreview = ImageView(activity).apply {
            val size = dp(activity, 56)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginEnd = dp(activity, 16)
            }
            val previewRouter = RouterStore.Router(
                "preview", "", "", editingIconColor, editingCustomIconPath
            )
            setImageDrawable(RouterStore.getIconDrawable(activity, previewRouter))
            setOnClickListener { showIconPickerDialog(activity) }
        }
        editIconPreview = iconPreview

        val inputLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
            addView(nameInput)
            addView(urlInput)
            addView(remoteUrlInput)
            addView(modeLabel)
            addView(modeBtnRow)
            addView(usernameInput)
            addView(passwordInput)
        }

        val topLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(activity, 20), dp(activity, 16), dp(activity, 20), dp(activity, 8))
            addView(iconPreview)
            addView(inputLayout)
        }

        val hintText = TextView(activity).apply {
            text = "点击左侧图标可更换图标（颜色或相册图片）"
            textSize = 12f
            setTextColor(0xFF888888.toInt())
            setPadding(dp(activity, 20), 0, dp(activity, 20), dp(activity, 12))
        }

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(topLayout)
            addView(hintText)
        }

        AlertDialog.Builder(activity)
            .setTitle(if (router == null) "添加路由器" else "编辑路由器")
            .setView(content)
            .setCancelable(cancelable)
            .setPositiveButton("保存") { _, _ ->
                val name = nameInput.text.toString().trim()
                var url = urlInput.text.toString().trim()

                if (name.isEmpty() || url.isEmpty()) {
                    Toast.makeText(activity, "名称和地址都不能为空", Toast.LENGTH_SHORT).show()
                    if (!cancelable) {
                        showEditDialog(activity, null, false, onSaved)
                    }
                    editIconPreview = null
                    onSavedCallback = null
                    return@setPositiveButton
                }
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "http://$url"
                }

                val finalRouter = RouterStore.Router(
                    id = editingId,
                    name = name,
                    url = url,
                    iconColor = editingIconColor,
                    customIconPath = editingCustomIconPath,
                    username = usernameInput.text.toString().trim(),
                    password = passwordInput.text.toString().trim(),
                    showIp = router?.showIp ?: true,
                    remoteUrl = remoteUrlInput.text.toString().trim(),
                    accessMode = editingAccessMode
                )
                onSavedCallback?.invoke(finalRouter)
                editIconPreview = null
                onSavedCallback = null
            }
            .setNegativeButton("取消") { _, _ ->
                editIconPreview = null
                onSavedCallback = null
            }
            .show()
    }

    /**
     * 宿主 Activity 在 onActivityResult 中调用此方法
     * @return true 表示已处理
     */
    fun handleActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ): Boolean {
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                handleImageSelected(activity, uri)
                return true
            }
        }
        return false
    }

    // ─── 内部：图标选择对话框 ────────────────────────────

    private fun showIconPickerDialog(activity: Activity) {
        val colorGrid = GridLayout(activity).apply {
            columnCount = 4
            setPadding(dp(activity, 20), dp(activity, 16), dp(activity, 20), dp(activity, 8))
        }

        for (color in RouterStore.iconColors) {
            val isSelected = (color == editingIconColor && editingCustomIconPath == null)
            val colorView = View(activity).apply {
                val size = dp(activity, 44)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(dp(activity, 6), dp(activity, 6), dp(activity, 6), dp(activity, 6))
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    if (isSelected) setStroke(dp(activity, 3), 0xFF333333.toInt())
                }
                setOnClickListener {
                    editingIconColor = color
                    editingCustomIconPath = null
                    updateEditIconPreview(activity)
                    for (i in 0 until colorGrid.childCount) {
                        (colorGrid.getChildAt(i).background as? GradientDrawable)?.setStroke(0, 0)
                    }
                    (it.background as? GradientDrawable)?.setStroke(
                        dp(activity, 3), 0xFF333333.toInt()
                    )
                }
            }
            colorGrid.addView(colorView)
        }

        val pickFromAlbumBtn = Button(activity).apply {
            text = "从相册选择图片"
            setOnClickListener {
                val intent = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
                activity.startActivityForResult(intent, REQUEST_PICK_IMAGE)
            }
        }

        val resetBtn = Button(activity).apply {
            text = "恢复默认图标"
            setOnClickListener {
                editingCustomIconPath = null
                editingIconColor = RouterStore.iconColors[0]
                updateEditIconPreview(activity)
                for (i in 0 until colorGrid.childCount) {
                    (colorGrid.getChildAt(i).background as? GradientDrawable)?.setStroke(0, 0)
                }
                (colorGrid.getChildAt(0).background as? GradientDrawable)?.setStroke(
                    dp(activity, 3), 0xFF333333.toInt()
                )
            }
        }

        val btnLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(activity, 20), 0, dp(activity, 20), dp(activity, 16))
            addView(pickFromAlbumBtn)
            addView(resetBtn)
        }

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(colorGrid)
            addView(btnLayout)
        }

        AlertDialog.Builder(activity)
            .setTitle("选择图标")
            .setView(content)
            .setPositiveButton("完成", null)
            .show()
    }

    private fun handleImageSelected(activity: Activity, uri: Uri) {
        try {
            val inputStream = activity.contentResolver.openInputStream(uri) ?: return
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap != null) {
                val path = RouterStore.saveCustomIcon(activity, bitmap, editingId)
                editingCustomIconPath = path
                updateEditIconPreview(activity)
                Toast.makeText(activity, "图标已更换", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(activity, "图片加载失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateEditIconPreview(activity: Activity) {
        editIconPreview?.let {
            val preview = RouterStore.Router(
                "preview", "", "", editingIconColor, editingCustomIconPath
            )
            it.setImageDrawable(RouterStore.getIconDrawable(activity, preview))
        }
    }

    private fun updateModeButtons(activity: Activity) {
        val selected = AppSettings.lighten(AppSettings.getThemeColor(activity), 0.85f)
        localModeBtn?.setBackgroundColor(
            if (editingAccessMode == RouterStore.ACCESS_LOCAL) selected else 0xFFEEEEEE.toInt()
        )
        remoteModeBtn?.setBackgroundColor(
            if (editingAccessMode == RouterStore.ACCESS_REMOTE) selected else 0xFFEEEEEE.toInt()
        )
    }

    private fun dp(activity: Activity, value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }
}
