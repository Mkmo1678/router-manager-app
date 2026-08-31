package com.router.manager

import android.content.Context
import java.io.File

/**
 * App 全局设置：自定义标题、主题色、背景图
 */
object AppSettings {

    private const val PREFS_NAME = "app_settings"
    private const val KEY_TITLE = "custom_title"
    private const val KEY_THEME_COLOR = "theme_color"
    private const val KEY_BACKGROUND_PATH = "background_path"

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
}
