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
    private var editIconPreview: ImageView? = null
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
        onSavedCallback = onSaved

        val nameInput = EditText(activity).apply {
            hint = "名称（如：家里、公司、机房A）"
            setText(router?.name ?: "")
            setSelection(text.length)
            val pad = dp(activity, 12)
            setPadding(pad, pad, pad, pad)
        }
        val urlInput = EditText(activity).apply {
            hint = "地址（如：http://192.168.1.1:3000）"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            setText(router?.url ?: "")
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
                    customIconPath = editingCustomIconPath
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

    private fun dp(activity: Activity, value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }
}
