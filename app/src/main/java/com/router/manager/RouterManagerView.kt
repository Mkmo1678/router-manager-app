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
 * 首页：路由器列表（大卡片 + 大图标风格）
 * 支持自定义标题、主题色、背景图
 */
class RouterManagerView(context: Context, private val statusBarHeight: Int) : FrameLayout(context) {

    var onRouterClick: ((RouterStore.Router) -> Unit)? = null
    var onRouterEdit: ((RouterStore.Router) -> Unit)? = null
    var onRouterDelete: ((RouterStore.Router) -> Unit)? = null
    var onSettingsClick: (() -> Unit)? = null

    private val cardsContainer: LinearLayout
    private val titleText: TextView
    private val settingsBtn: ImageView
    private val contentLayout: LinearLayout
    private val bgImageView: ImageView
    private var routers: MutableList<RouterStore.Router> = mutableListOf()
    private var themeColor: Int = AppSettings.defaultTheme

    init {
        // 背景图层（最底层，CENTER_CROP 裁剪填满）
        bgImageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            visibility = View.GONE
        }
        addView(bgImageView)

        val scrollView = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            setPadding(0, 0, 0, 0)
        }

        contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            // 顶部留出状态栏高度，底部留出导航栏+底部导航高度
            setPadding(dp(20), statusBarHeight + dp(8), dp(20), dp(140))
        }

        // 标题栏：标题 + 设置按钮
        val titleBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(20))
        }

        titleText = TextView(context).apply {
            text = AppSettings.getTitle(context)
            textSize = 32f
            setTextColor(Color.parseColor("#1A1A1A"))
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
        }

        settingsBtn = ImageView(context).apply {
            val size = dp(36)
            layoutParams = LinearLayout.LayoutParams(size, size)
            setImageResource(R.drawable.ic_settings)
            setColorFilter(Color.parseColor("#999999"), PorterDuff.Mode.SRC_ATOP)
            setPadding(dp(6), dp(6), dp(6), dp(6))
            setOnClickListener { onSettingsClick?.invoke() }
        }

        titleBar.addView(titleText)
        titleBar.addView(settingsBtn)
        contentLayout.addView(titleBar)

        cardsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        contentLayout.addView(cardsContainer)

        scrollView.addView(contentLayout)
        addView(scrollView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        applyTheme()
        applyBackground()
    }

    fun refresh() {
        routers = RouterStore.loadRouters(context)
        titleText.text = AppSettings.getTitle(context)
        cardsContainer.removeAllViews()
        for (router in routers) {
            cardsContainer.addView(createRouterCard(router))
        }
    }

    fun applyTheme() {
        themeColor = AppSettings.getThemeColor(context)
        applyTextColor()
        // 刷新所有卡片的"进入管理"颜色
        for (i in 0 until cardsContainer.childCount) {
            val card = cardsContainer.getChildAt(i) as? LinearLayout
            card?.let {
                // 第三个子View是textLayout，里面第三个是enterText
                val textLayout = it.getChildAt(1) as? LinearLayout
                val enterText = textLayout?.getChildAt(2) as? TextView
                enterText?.setTextColor(themeColor)
            }
        }
    }

    /** 根据背景明暗自动切换标题和设置按钮颜色 */
    private fun applyTextColor() {
        val dark = AppSettings.isDarkBackground(context)
        if (dark) {
            titleText.setTextColor(Color.WHITE)
            settingsBtn.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
        } else {
            titleText.setTextColor(Color.parseColor("#1A1A1A"))
            settingsBtn.setColorFilter(themeColor, PorterDuff.Mode.SRC_ATOP)
        }
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
        setBackgroundColor(Color.parseColor("#F0F2F5"))
        applyTextColor()
    }

    private fun createRouterCard(router: RouterStore.Router): View {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(18), dp(12), dp(18))
            background = createCardBackground()
            elevation = dp(2).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        }

        // 左侧大图标
        val icon = ImageView(context).apply {
            val size = dp(72)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(16)
            }
            setImageDrawable(RouterStore.getIconDrawable(context, router))
            elevation = dp(3).toFloat()
        }

        // 右侧文字区
        val textLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
        }

        val nameText = TextView(context).apply {
            text = router.name
            textSize = 19f
            setTextColor(Color.parseColor("#1A1A1A"))
            setTypeface(null, Typeface.BOLD)
        }

        val urlText = TextView(context).apply {
            text = router.url
            textSize = 13f
            setTextColor(Color.parseColor("#888888"))
            setPadding(0, dp(4), 0, dp(10))
        }

        val enterText = TextView(context).apply {
            text = "进入管理 →"
            textSize = 14f
            setTextColor(themeColor)
            setTypeface(null, Typeface.BOLD)
        }

        textLayout.addView(nameText)
        textLayout.addView(urlText)
        textLayout.addView(enterText)

        // 编辑按钮
        val editBtn = ImageView(context).apply {
            val size = dp(36)
            layoutParams = LinearLayout.LayoutParams(size, size)
            setImageResource(R.drawable.ic_edit)
            setColorFilter(Color.parseColor("#BBBBBB"), PorterDuff.Mode.SRC_ATOP)
            setPadding(dp(6), dp(6), dp(6), dp(6))
            setOnClickListener { onRouterEdit?.invoke(router) }
        }

        card.addView(icon)
        card.addView(textLayout)
        card.addView(editBtn)

        card.setOnClickListener { onRouterClick?.invoke(router) }
        card.setOnLongClickListener {
            onRouterDelete?.invoke(router)
            true
        }

        return card
    }

    private fun createCardBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(18).toFloat()
            setColor(Color.WHITE)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
