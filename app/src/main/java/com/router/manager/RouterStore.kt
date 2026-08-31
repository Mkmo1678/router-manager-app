package com.router.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

/**
 * 路由器数据持久化与图标工具（全局共享）
 */
object RouterStore {

    const val PREFS_NAME = "router_manager_prefs"
    const val KEY_ROUTERS = "routers_json"
    const val KEY_CURRENT_ID = "current_router_id"

    val iconColors = intArrayOf(
        0xFF1565C0.toInt(), // 蓝
        0xFF2E7D32.toInt(), // 绿
        0xFFE65100.toInt(), // 橙
        0xFFC62828.toInt(), // 红
        0xFF6A1B9A.toInt(), // 紫
        0xFF00838F.toInt(), // 青
        0xFFAD1457.toInt(), // 粉
        0xFF455A64.toInt(), // 灰蓝
    )

    data class Router(
        val id: String,
        val name: String,
        val url: String,
        val iconColor: Int,
        val customIconPath: String?
    )

    // ─── 持久化 ──────────────────────────────────────────

    fun loadRouters(context: Context): MutableList<Router> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val list = mutableListOf<Router>()
        val json = prefs.getString(KEY_ROUTERS, null) ?: return list
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val customPath = obj.optString("customIconPath", "")
                list.add(
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
        return list
    }

    fun saveRouters(context: Context, routers: List<Router>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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

    fun getCurrentRouterId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CURRENT_ID, null)
    }

    fun setCurrentRouterId(context: Context, id: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (id == null) {
            prefs.edit().remove(KEY_CURRENT_ID).apply()
        } else {
            prefs.edit().putString(KEY_CURRENT_ID, id).apply()
        }
    }

    // ─── 图标工具 ────────────────────────────────────────

    fun getIconDrawable(context: Context, router: Router): Drawable? {
        router.customIconPath?.let { path ->
            getCustomIconDrawable(context, path)?.let { return it }
        }
        return getBuiltinIconDrawable(context, router.iconColor)
    }

    fun getBuiltinIconDrawable(context: Context, color: Int): Drawable {
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
        val symbol = context.getDrawable(R.drawable.ic_launcher_foreground)?.mutate()?.apply {
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
        }
        val layers = if (symbol != null) arrayOf(bg, symbol) else arrayOf(bg)
        return LayerDrawable(layers).apply {
            if (symbol != null) {
                val density = context.resources.displayMetrics.density
                val inset = (10 * density).toInt()
                setLayerInset(1, inset, inset, inset, inset)
            }
        }
    }

    fun getCustomIconDrawable(context: Context, path: String): Drawable? {
        val file = File(path)
        if (!file.exists()) return null
        val bitmap = BitmapFactory.decodeFile(path) ?: return null
        return BitmapDrawable(context.resources, getCircularBitmap(bitmap))
    }

    fun getCircularBitmap(src: Bitmap): Bitmap {
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

    fun scaleToSquare(src: Bitmap, size: Int): Bitmap {
        val minDim = min(src.width, src.height)
        val x = (src.width - minDim) / 2
        val y = (src.height - minDim) / 2
        val cropped = Bitmap.createBitmap(src, x, y, minDim, minDim)
        return Bitmap.createScaledBitmap(cropped, size, size, true)
    }

    fun saveCustomIcon(context: Context, bitmap: Bitmap, routerId: String): String {
        val dir = File(context.filesDir, "router_icons")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "$routerId.png")
        val scaled = scaleToSquare(bitmap, 128)
        FileOutputStream(file).use { out ->
            getCircularBitmap(scaled).compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.absolutePath
    }
}
