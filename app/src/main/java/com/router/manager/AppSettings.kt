package com.router.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * App 全局设置：自定义标题、主题色、背景图
 */
object AppSettings {

    private const val PREFS_NAME = "app_settings"
    private const val KEY_TITLE = "custom_title"
    private const val KEY_THEME_COLOR = "theme_color"
    private const val KEY_BACKGROUND_PATH = "background_path"
    private const val KEY_UA_MODE = "ua_mode"
    private const val KEY_SHOW_IP = "show_ip"

    const val UA_MODE_MOBILE = 0
    const val UA_MODE_DESKTOP = 1

    /** 电脑端 Chrome User-Agent */
    const val DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    val defaultTheme = 0xFF1565C0.toInt() // 蓝

    /** 可选主题色 */
    val themeColors = intArrayOf(
        0xFF1565C0.toInt(), // 蓝
        0xFF2E7D32.toInt(), // 绿
        0xFFE65100.toInt(), // 橙
        0xFF6A1B9A.toInt(), // 紫
        0xFFC62828.toInt(), // 红
        0xFF00838F.toInt(), // 青
        0xFF455A64.toInt(), // 灰蓝
    )

    fun getTitle(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TITLE, "路由器管理") ?: "路由器管理"
    }

    fun setTitle(context: Context, title: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TITLE, title).apply()
    }

    fun getThemeColor(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_THEME_COLOR, defaultTheme)
    }

    fun setThemeColor(context: Context, color: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_THEME_COLOR, color).apply()
    }

    fun getBackgroundPath(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val path = prefs.getString(KEY_BACKGROUND_PATH, null) ?: return null
        return if (File(path).exists()) path else null
    }

    fun setBackgroundPath(context: Context, path: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_BACKGROUND_PATH, path).apply()
    }

    fun getUaMode(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_UA_MODE, UA_MODE_DESKTOP) // 默认电脑模式
    }

    fun setUaMode(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_UA_MODE, mode).apply()
    }

    /** 根据当前模式返回 User-Agent，mobileDefault 为 WebView 默认手机 UA */
    fun getUserAgent(context: Context, mobileDefault: String): String {
        return if (getUaMode(context) == UA_MODE_DESKTOP) DESKTOP_UA else mobileDefault
    }

    fun getShowIp(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SHOW_IP, true) // 默认显示
    }

    fun setShowIp(context: Context, show: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_IP, show).apply()
    }

    /** 颜色加深（用于状态栏） */
    fun darken(color: Int, factor: Float = 0.7f): Int {
        val r = ((color shr 16) and 0xFF) * factor
        val g = ((color shr 8) and 0xFF) * factor
        val b = (color and 0xFF) * factor
        return (0xFF shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
    }

    /** 颜色变浅（用于选中背景） */
    fun lighten(color: Int, factor: Float = 0.85f): Int {
        val r = 255 - (255 - ((color shr 16) and 0xFF)) * factor
        val g = 255 - (255 - ((color shr 8) and 0xFF)) * factor
        val b = 255 - (255 - (color and 0xFF)) * factor
        return (0xFF shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
    }

    /**
     * 检测背景图是否偏暗，用于自动切换文字颜色
     * @return true=暗色背景（文字应用白色），false=亮色背景（文字应用黑色）
     */
    fun isDarkBackground(context: Context): Boolean {
        val path = getBackgroundPath(context) ?: return false
        val bitmap = BitmapFactory.decodeFile(path) ?: return false
        return try {
            // 缩放到 8x8 取平均亮度，比 1x1 更稳定
            val scaled = Bitmap.createScaledBitmap(bitmap, 8, 8, true)
            var totalLuminance = 0.0
            var count = 0
            for (x in 0 until scaled.width) {
                for (y in 0 until scaled.height) {
                    val pixel = scaled.getPixel(x, y)
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    // 人眼感知亮度公式
                    totalLuminance += 0.299 * r + 0.587 * g + 0.114 * b
                    count++
                }
            }
            scaled.recycle()
            val avg = if (count > 0) totalLuminance / count else 255.0
            avg < 140.0 // 阈值140，偏保守地认为中等亮度以下就用白字
        } finally {
            bitmap.recycle()
        }
    }
}
