package com.router.manager

import android.content.Context
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

/**
 * 首页：路由器列表（大卡片 + 大图标风格）
 */
class RouterManagerView(context: Context) : FrameLayout(context) {

    var onRouterClick: ((RouterStore.Router) -> Unit)? = null
    var onRouterEdit: ((RouterStore.Router) -> Unit)? = null
    var onRouterDelete: ((RouterStore.Router) -> Unit)? = null

    private val cardsContainer: LinearLayout
    private var routers: MutableList<RouterStore.Router> = mutableListOf()

    init {
        val scrollView = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            setPadding(0, dp(8), 0, dp(16))
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }

        // 大标题
        val title = TextView(context).apply {
            text = "路由器管理"
            textSize = 32f
            setTextColor(Color.parseColor("#1A1A1A"))
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(12), 0, dp(20))
        }
        content.addView(title)

        cardsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        content.addView(cardsContainer)

        scrollView.addView(content)
        addView(scrollView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun refresh() {
        routers = RouterStore.loadRouters(context)
        cardsContainer.removeAllViews()
        for (router in routers) {
            cardsContainer.addView(createRouterCard(router))
        }
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
            setTextColor(Color.parseColor("#1565C0"))
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
