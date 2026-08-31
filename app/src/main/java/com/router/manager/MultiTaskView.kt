package com.router.manager

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File

/**
 * 多任务页面：已打开的管理界面列表（玻璃拟态卡片风格）
 */
class MultiTaskView(context: Context, private val statusBarHeight: Int) : FrameLayout(context) {

    var onRouterClick: ((RouterStore.Router) -> Unit)? = null
    var onRouterClose: ((RouterStore.Router) -> Unit)? = null

    private val cardsContainer: LinearLayout
    private val emptyText: TextView
    private val titleText: TextView
    private val subtitleText: TextView
    private val contentLayout: LinearLayout
    private val bgImageView: ImageView
    private var openedRouters: List<RouterStore.Router> = emptyList()
    private var currentRouterId: String? = null
    private var themeColor: Int = AppSettings.defaultTheme

    init {
        bgImageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            visibility = View.GONE
        }
        addView(bgImageView)

        val scrollView = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
        }

        contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), statusBarHeight + dp(12), dp(16), dp(140))
        }

        titleText = TextView(context).apply {
            text = "多任务"
            textSize = 30f
            setTextColor(Color.parseColor("#1A1A1A"))
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(6), dp(8), 0, dp(6))
        }
        contentLayout.addView(titleText)

        subtitleText = TextView(context).apply {
            text = "已打开的管理界面，点击切换，✕ 关闭"
            textSize = 13f
            setTextColor(Color.parseColor("#999999"))
            setPadding(dp(6), 0, 0, dp(20))
        }
        contentLayout.addView(subtitleText)

        cardsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        contentLayout.addView(cardsContainer)

        emptyText = TextView(context).apply {
            text = "暂无已打开的管理界面\n去首页打开一个路由器吧"
            textSize = 15f
            setTextColor(Color.parseColor("#BBBBBB"))
            gravity = Gravity.CENTER
            setPadding(0, dp(60), 0, 0)
            visibility = View.GONE
        }
        contentLayout.addView(emptyText)

        scrollView.addView(contentLayout)
        addView(scrollView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        themeColor = AppSettings.getThemeColor(context)
        applyBackground()
    }

    fun update(opened: List<RouterStore.Router>, currentId: String?) {
        openedRouters = opened
        currentRouterId = currentId
        themeColor = AppSettings.getThemeColor(context)
        cardsContainer.removeAllViews()

        if (opened.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            cardsContainer.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            cardsContainer.visibility = View.VISIBLE
            for (router in opened) {
                cardsContainer.addView(createTaskCard(router))
            }
        }
    }

    fun applyTheme() {
        themeColor = AppSettings.getThemeColor(context)
        update(openedRouters, currentRouterId)
    }

    fun applyBackground() {
        val path = AppSettings.getBackgroundPath(context)
        if (path != null && File(path).exists()) {
            val bitmap = BitmapFactory.decodeFile(path)
            if (bitmap != null) {
                bgImageView.setImageBitmap(bitmap)
                bgImageView.visibility = View.VISIBLE
                setBackgroundColor(Color.TRANSPARENT)
                contentLayout.setBackgroundColor(Color.TRANSPARENT)
                applyTextColor()
                return
            }
        }
        bgImageView.setImageDrawable(null)
        bgImageView.visibility = View.GONE
        setBackgroundColor(Color.parseColor("#E8EAED"))
        applyTextColor()
    }

    private fun applyTextColor() {
        val dark = AppSettings.isDarkBackground(context)
        if (dark) {
            titleText.setTextColor(Color.WHITE)
            subtitleText.setTextColor(Color.parseColor("#CCCCCC"))
            emptyText.setTextColor(Color.parseColor("#AAAAAA"))
        } else {
            titleText.setTextColor(Color.parseColor("#1A1A1A"))
            subtitleText.setTextColor(Color.parseColor("#999999"))
            emptyText.setTextColor(Color.parseColor("#BBBBBB"))
        }
    }

    private fun createTaskCard(router: RouterStore.Router): View {
        val isCurrent = router.id == currentRouterId
        val show = router.showIp

        // 外层卡片（玻璃拟态，宽度稍窄）
        val card = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dp(8)
                marginEnd = dp(8)
                bottomMargin = dp(16)
            }
            elevation = if (isCurrent) dp(10).toFloat() else dp(5).toFloat()
        }

        // 玻璃背景层（高不透明度确保文字清晰）
        val glassBg = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(22).toFloat()
                if (isCurrent) {
                    setColor(Color.parseColor("#E0FFFFFF"))
                    setStroke(dp(2), themeColor)
                } else {
                    setColor(Color.parseColor("#D8FFFFFF")) // 85% 不透明白
                    setStroke(dp(1), Color.parseColor("#80FFFFFF"))
                }
            }
        }

        // 内容区
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(10), dp(16))
        }

        // 圆形图标
        val iconContainer = FrameLayout(context).apply {
            val size = dp(48)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(12)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(router.iconColor)
            }
        }

        val icon = ImageView(context).apply {
            val size = dp(24)
            layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
            setImageResource(R.drawable.ic_launcher_foreground)
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
        }
        if (router.customIconPath != null && File(router.customIconPath).exists()) {
            icon.setImageBitmap(BitmapFactory.decodeFile(router.customIconPath))
            icon.colorFilter = null
        }
        iconContainer.addView(icon)

        // 文字区
        val textLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
        }

        val nameText = TextView(context).apply {
            text = if (isCurrent) "✓ ${router.name}" else router.name
            textSize = 16f
            setTextColor(
                if (isCurrent) themeColor else Color.parseColor("#1A1A1A")
            )
            setTypeface(null, Typeface.BOLD)
        }

        val urlText = TextView(context).apply {
            text = if (show) router.url else "地址：••••••••••••"
            textSize = 12f
            setTextColor(Color.parseColor("#666666"))
            setPadding(0, dp(3), 0, 0)
        }

        val statusText = TextView(context).apply {
            text = if (isCurrent) "当前使用中" else "后台运行中"
            textSize = 12f
            setTextColor(
                if (isCurrent) themeColor else Color.parseColor("#4CAF50")
            )
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(5), 0, 0)
        }

        textLayout.addView(nameText)
        textLayout.addView(urlText)
        textLayout.addView(statusText)

        // 关闭按钮
        val closeBtn = TextView(context).apply {
            text = "✕"
            textSize = 16f
            setTextColor(Color.parseColor("#BBBBBB"))
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener { onRouterClose?.invoke(router) }
        }

        content.addView(iconContainer)
        content.addView(textLayout)
        content.addView(closeBtn)

        card.addView(glassBg)
        card.addView(content)

        card.setOnClickListener { onRouterClick?.invoke(router) }

        return card
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
