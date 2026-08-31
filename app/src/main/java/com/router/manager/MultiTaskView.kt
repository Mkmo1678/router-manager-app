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
 * 多任务页面：已打开的管理界面列表（卡片风格）
 */
class MultiTaskView(context: Context) : FrameLayout(context) {

    var onRouterClick: ((RouterStore.Router) -> Unit)? = null
    var onRouterClose: ((RouterStore.Router) -> Unit)? = null

    private val cardsContainer: LinearLayout
    private val emptyText: TextView
    private var openedRouters: List<RouterStore.Router> = emptyList()
    private var currentRouterId: String? = null

    init {
        val scrollView = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            setPadding(0, dp(8), 0, dp(16))
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }

        val title = TextView(context).apply {
            text = "多任务"
            textSize = 32f
            setTextColor(Color.parseColor("#1A1A1A"))
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(12), 0, dp(8))
        }
        content.addView(title)

        val subtitle = TextView(context).apply {
            text = "已打开的管理界面，点击切换，✕ 关闭"
            textSize = 13f
            setTextColor(Color.parseColor("#999999"))
            setPadding(0, 0, 0, dp(16))
        }
        content.addView(subtitle)

        cardsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        content.addView(cardsContainer)

        emptyText = TextView(context).apply {
            text = "暂无已打开的管理界面\n去首页打开一个路由器吧"
            textSize = 15f
            setTextColor(Color.parseColor("#BBBBBB"))
            gravity = Gravity.CENTER
            setPadding(0, dp(60), 0, 0)
            visibility = View.GONE
        }
        content.addView(emptyText)

        scrollView.addView(content)
        addView(scrollView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun update(opened: List<RouterStore.Router>, currentId: String?) {
        openedRouters = opened
        currentRouterId = currentId
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

    private fun createTaskCard(router: RouterStore.Router): View {
        val isCurrent = router.id == currentRouterId

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(12), dp(16))
            background = createCardBackground(isCurrent)
            elevation = dp(2).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        }

        val icon = ImageView(context).apply {
            val size = dp(56)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(14)
            }
            setImageDrawable(RouterStore.getIconDrawable(context, router))
        }

        val textLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
        }

        val nameText = TextView(context).apply {
            text = if (isCurrent) "✓ ${router.name}" else router.name
            textSize = 17f
            setTextColor(
                if (isCurrent) Color.parseColor("#1565C0")
                else Color.parseColor("#1A1A1A")
            )
            setTypeface(null, Typeface.BOLD)
        }

        val urlText = TextView(context).apply {
            text = router.url
            textSize = 12f
            setTextColor(Color.parseColor("#999999"))
            setPadding(0, dp(4), 0, 0)
        }

        val statusText = TextView(context).apply {
            text = if (isCurrent) "当前使用中" else "后台运行中"
            textSize = 12f
            setTextColor(
                if (isCurrent) Color.parseColor("#1565C0")
                else Color.parseColor("#4CAF50")
            )
            setPadding(0, dp(6), 0, 0)
        }

        textLayout.addView(nameText)
        textLayout.addView(urlText)
        textLayout.addView(statusText)

        val closeBtn = TextView(context).apply {
            text = "✕"
            textSize = 20f
            setTextColor(Color.parseColor("#CCCCCC"))
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener { onRouterClose?.invoke(router) }
        }

        card.addView(icon)
        card.addView(textLayout)
        card.addView(closeBtn)

        card.setOnClickListener { onRouterClick?.invoke(router) }

        return card
    }

    private fun createCardBackground(isCurrent: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(16).toFloat()
            setColor(
                if (isCurrent) Color.parseColor("#E3F2FD")
                else Color.WHITE
            )
            if (isCurrent) {
                setStroke(dp(2), Color.parseColor("#1565C0"))
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
